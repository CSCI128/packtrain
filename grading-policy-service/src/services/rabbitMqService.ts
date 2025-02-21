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

export async function connect(rabbitMqConfig: RabbitMqConfig): Promise<Connection> {
    const connectionURI = `amqp://${rabbitMqConfig.username}:${rabbitMqConfig.password}@${rabbitMqConfig.endpoint}:${rabbitMqConfig.port}`;
    return rabbitMqConnect(connectionURI);
}

async function onGradingStart(connection: Connection, exchangeName: string, start: GradingStartDTO){
    if (activeMigrations.has(start.migrationId)){
        return Promise.reject("Migration is already started!");
    }

    const policy = await downloadAndVerifyPolicy(start.migrationId, start.policyURI);

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

export async function setupGradingMessageChannel(rabbitMqConfig: RabbitMqConfig, connection: Connection): Promise<ConsumerChannel> {
    const channel = await connection.createChannel();

    const queue = await channel.assertExchange(rabbitMqConfig.exchangeName, "direct", {durable: true})
        .then(async (ex) => {
            // let the server give us a randomly named queue
            const queue = await channel.assertQueue("");
            await channel.bindQueue(queue.queue, ex.exchange, rabbitMqConfig.gradingMessageRoutingKey);
            return queue.queue;
        });

    const consumer = await channel.consume(queue, (msg: ConsumeMessage | null) => {
        if (!msg){
            return;
        }

        if (msg.properties.type === "grading.start" && msg.properties.contentType === "application/json"){
            const content  = JSON.parse(msg.content.toString()) as GradingStartDTO;

            onGradingStart(connection, rabbitMqConfig.exchangeName, content);
        }
    });

    return {channel: channel, consumerTag: consumer.consumerTag};
}