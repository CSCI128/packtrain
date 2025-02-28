import { RabbitMqConfig } from "../config";
import {
    Connection,
    connect as rabbitMqConnect,
    Channel,
    ConsumeMessage,
} from "amqplib";
import GradingStartDTO, {
    GlobalAssignmentMetadata,
} from "../data/GradingStartDTO";
import RawScoreDTO from "../data/RawScoreDTO";
import {
    ApplyPolicyFunctionSig,
    downloadAndVerifyPolicy,
} from "./policyService";

export interface ConsumerChannel {
    channel: Channel;
    consumerTag: string;
}

export interface MigrationSet {
    consumer: ConsumerChannel;
    producer: Channel;
}

const activeMigrations: Map<string, MigrationSet> = new Map();

let connection: Connection | null = null;

export async function connect(rabbitMqConfig: RabbitMqConfig): Promise<void> {
    if (connection !== null) {
        return Promise.reject("Connection already established");
    }

    const connectionURI = `amqp://${rabbitMqConfig.username}:${rabbitMqConfig.password}@${rabbitMqConfig.endpoint}:${rabbitMqConfig.port}`;

    connection = await rabbitMqConnect(connectionURI);
}

export async function endConnection(): Promise<void> {
    await connection?.close();
    connection = null;
}

export function getMigration(uuid: string): MigrationSet | null {
    if (!activeMigrations.has(uuid)) {
        return null;
    }

    return activeMigrations.get(uuid)!;
}

export function ready() {
    return connection !== null;
}

export async function startMigration(
    exchangeName: string,
    start: GradingStartDTO,
    overridePolicy?: string,
): Promise<void> {
    if (connection === null) {
        return Promise.reject("Connection has not been established!");
    }

    if (activeMigrations.has(start.migrationId)) {
        return Promise.reject("Migration is already started!");
    }

    const policy = overridePolicy
        ? (Function(overridePolicy) as ApplyPolicyFunctionSig)
        : await downloadAndVerifyPolicy(start.policyURI);

    const publishChannel: Channel = await connection
        .createChannel()
        .then(async (ch) => {
            const queue = await ch.assertQueue("");
            await ch.assertExchange(exchangeName, "direct");
            await ch.bindQueue(
                queue.queue,
                exchangeName,
                start.scoreCreatedRoutingKey,
            );

            return ch;
        });

    const receiverChannel = await connection
        .createChannel()
        .then(async (ch): Promise<ConsumerChannel> => {
            const queue = await ch.assertQueue("");
            await ch.assertExchange(exchangeName, "direct");
            await ch.bindQueue(
                queue.queue,
                exchangeName,
                start.rawGradeRoutingKey,
            );
            const consumer = await ch.consume(
                queue.queue,
                (msg: ConsumeMessage | null) => {
                    if (!msg) {
                        return;
                    }
                    if (
                        msg.properties.type === "grade.raw_score" &&
                        msg.properties.contentType === "application/json"
                    ) {
                        const content = JSON.parse(
                            msg.content.toString(),
                        ) as RawScoreDTO;
                        onRawScoreReceive(
                            exchangeName,
                            start.scoreCreatedRoutingKey,
                            start.globalMetadata,
                            content,
                            policy,
                            publishChannel,
                        );
                    }
                },
            );

            return { channel: ch, consumerTag: consumer.consumerTag };
        });

    activeMigrations.set(start.migrationId, {
        consumer: receiverChannel,
        producer: publishChannel,
    });

    return Promise.resolve();
}

function onRawScoreReceive(
    exchange: string,
    routingKey: string,
    assignmentMetadata: GlobalAssignmentMetadata,
    rawScore: RawScoreDTO,
    policy: ApplyPolicyFunctionSig,
    publishChannel: Channel,
) {
    rawScore.assignmentId = assignmentMetadata.assignmentId;
    rawScore.minScore = assignmentMetadata.minScore;
    rawScore.maxScore = assignmentMetadata.maxScore;
    rawScore.initialDueDate = assignmentMetadata.initialDueDate;

    try {
        const score = policy(rawScore);

        publishChannel.publish(
            exchange,
            routingKey,
            Buffer.from(JSON.stringify(score)),
            {
                contentType: "application/json",
                type: "grade.scored",
            },
        );
    } catch (e) {
        console.error(e);
    }
}
