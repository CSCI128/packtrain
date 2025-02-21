import {config, RabbitMqConfig} from "../config";
import {Connection, connect as rabbitMqConnect, Channel,  ConsumeMessage} from "amqplib"
import GradingStartDTO, {GlobalAssignmentMetadata} from "../data/GradingStartDTO";
import RawScoreDTO from "../data/RawScoreDTO";
import {ApplyPolicyFunctionSig, downloadAndVerifyPolicy} from "./policyService";

interface ConsumerChannel {
    channel: Channel;
    consumerTag: string;
}

interface MigrationSet {
    consumer: ConsumerChannel;
    producer: Channel;
}

const activeMigrations: Map<string, MigrationSet> = new Map();

let connection: Connection | null = null;

export async function connect(rabbitMqConfig: RabbitMqConfig): Promise<void> {
    if (connection !== null){
        return Promise.reject("Connection already established");
    }

    const connectionURI = `amqp://${rabbitMqConfig.username}:${rabbitMqConfig.password}@${rabbitMqConfig.endpoint}:${rabbitMqConfig.port}`;

    connection = await rabbitMqConnect(connectionURI);
}

export function ready(){
    return connection !== null;
}

export async function startMigration(exchangeName: string, start:GradingStartDTO): Promise<void> {
    if (connection === null){
        return Promise.reject("Connection has not been established!");
    }

    if (activeMigrations.has(start.migrationId)){
        return Promise.reject("Migration is already started!");
    }

    const policy = await downloadAndVerifyPolicy(start.policyURI);

    const publishChannel: Channel = await connection.createChannel()
        .then(async ch => {
            const queue = await ch.assertQueue("");
            await ch.bindQueue(queue.queue, exchangeName, start.scoreCreatedRoutingKey);

            return publishChannel;
        })

    const receiverChannel= await connection.createChannel()
        .then(async (ch): Promise<ConsumerChannel> => {
            const queue = await ch.assertQueue("");
            await ch.bindQueue(queue.queue, exchangeName, start.rawGradeRoutingKey);
            const consumer= await ch.consume(queue.queue, (msg: ConsumeMessage | null) => {
                if (!msg){
                    return;
                }
                if (msg.properties.type == "grade.raw_score" && msg.properties.contentType == "application/json"){
                    const content  = JSON.parse(msg.content.toString()) as RawScoreDTO;
                    onRawScoreReceive(exchangeName, start.scoreCreatedRoutingKey, start.globalMetadata, content, policy, publishChannel);
                }
            });

            return {channel: ch, consumerTag: consumer.consumerTag};
        })

    activeMigrations.set(start.migrationId, {consumer: receiverChannel, producer: publishChannel});

    return Promise.resolve();
}

function onRawScoreReceive(exchange: string, routingKey: string, assignmentMetadata: GlobalAssignmentMetadata, rawScore: RawScoreDTO, policy: ApplyPolicyFunctionSig, publishChannel: Channel){
    rawScore.assignmentId = assignmentMetadata.assignmentId;
    rawScore.minScore = assignmentMetadata.minScore;
    rawScore.maxScore = assignmentMetadata.maxScore;
    rawScore.initialDueDate = assignmentMetadata.initialDueDate;


    try {
        const score = policy(rawScore);

        publishChannel.publish(exchange, routingKey, Buffer.from(JSON.stringify(score)), {contentType: "application/json", type: "grade.scored"})
    }
    catch (e){
    }
}