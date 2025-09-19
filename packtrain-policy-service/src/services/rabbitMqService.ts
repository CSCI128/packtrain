import {
    Channel,
    Connection,
    ConsumeMessage,
    connect as rabbitMqConnect,
} from "amqplib";
import { RabbitMqConfig } from "../config/config";
import GradingStartDTO, {
    GlobalAssignmentMetadata,
} from "../data/GradingStartDTO";
import RawScoreDTO from "../data/RawScoreDTO";
import ScoredDTO from "../data/ScoredDTO";
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
const delay = (ms: number): Promise<void> =>
    new Promise((resolve) => setTimeout(resolve, ms));

const activeMigrations: Map<string, MigrationSet> = new Map();

let connection: Connection | null = null;

export async function connect(rabbitMqConfig: RabbitMqConfig): Promise<void> {
    if (connection !== null) {
        return Promise.reject("Connection already established");
    }

    const connectionURI = `amqp://${rabbitMqConfig.username}:${rabbitMqConfig.password}@${rabbitMqConfig.endpoint}:${rabbitMqConfig.port}`;

    let attempts = 0;
    do {
        try {
            connection = await rabbitMqConnect(connectionURI);
        } catch (e) {
            console.error(
                `Connection attempt failed! Retrying ${attempts + 1}/10...`,
            );
            console.error(e);
        }

        await delay(5000);
    } while (connection === null && ++attempts < 10);

    if (connection === null) {
        return Promise.reject("Failed to connect!");
    }
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

    console.log(start);

    const policy = overridePolicy
        ? (Function("rawScore", overridePolicy) as ApplyPolicyFunctionSig)
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
                        msg.properties.type === "grading.raw_score" &&
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
    rawScore.canvasMinScore = assignmentMetadata.canvasMinScore;
    rawScore.canvasMaxScore = assignmentMetadata.canvasMaxScore;
    rawScore.externalMaxScore = assignmentMetadata.externalMaxScore;
    console.log(assignmentMetadata.initialDueDate);
    rawScore.initialDueDate = assignmentMetadata.initialDueDate.toString();

    try {
        const policyScored = policy(rawScore);
        const scored = new ScoredDTO();
        scored.cwid = rawScore.cwid;
        scored.extensionId = rawScore.extensionId;
        scored.assignmentId = rawScore.assignmentId;
        scored.rawScore = rawScore.rawScore;
        scored.finalScore = policyScored.finalScore;
        scored.adjustedSubmissionTime = policyScored.adjustedSubmissionDate;
        scored.daysLate = policyScored.adjustedDaysLate;
        scored.submissionStatus = policyScored.submissionStatus;
        scored.extensionStatus = policyScored.extensionStatus;
        scored.extensionMessage = policyScored.extensionMessage ?? "";
        scored.submissionMessage = policyScored.submissionMessage ?? "";

        publishChannel.publish(
            exchange,
            routingKey,
            Buffer.from(JSON.stringify(scored)),
            {
                contentType: "application/json",
                type: "grading.scored",
            },
        );
    } catch (e) {
        console.error(e);
    }
}


