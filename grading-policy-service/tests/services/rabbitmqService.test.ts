import { describe, it } from "node:test";
import { before, after } from "node:test";
import {
    RabbitMQContainer,
    StartedRabbitMQContainer
} from "@testcontainers/rabbitmq";
import * as ampq from "amqplib";
import * as assert from "node:assert";
import { RabbitMqConfig } from "../../src/config/config";
import {
    connect,
    endConnection,
    getMigration,
    ready,
    startMigration
} from "../../src/services/rabbitMqService";
import GradingStartDTO, { GlobalAssignmentMetadata } from "../../src/data/GradingStartDTO";
import { randomUUID } from "node:crypto";
import ScoredDTO from "../../src/data/ScoredDTO";
import { ConsumeMessage } from "amqplib";
import RawScoreDTO from "../../src/data/RawScoreDTO";

describe("RabbitMq Service", () => {
    const username = "admin";
    const password = "password";
    let config: RabbitMqConfig | null = null;
    let rabbitMq: StartedRabbitMQContainer | null = null;
    let connection: ampq.Connection | null = null;

    before(async () => {
        rabbitMq = await new RabbitMQContainer()
            .withEnvironment({
                RABBITMQ_DEFAULT_USER: username,
                RABBITMQ_DEFAULT_PASS: password
            })
            .start();

        config = {
            username: username,
            password: password,
            endpoint: rabbitMq.getHost(),
            port: rabbitMq.getMappedPort(5672).toString(),
            exchangeName: "default"
        };
        connection = await ampq.connect({
            username: username,
            password: password,
            port: rabbitMq.getMappedPort(5672)
        });
    });

    it("should create connection", async () => {
        await connect(config!);

        assert.equal(ready(), true);

        await endConnection();
    });

    it("should create channels on migration start", async () => {
        await connect(config!);

        assert.equal(ready(), true);

        const dto = new GradingStartDTO();
        dto.migrationId = randomUUID();
        dto.scoreCreatedRoutingKey = `${dto.migrationId}-score`;
        dto.rawGradeRoutingKey = `${dto.migrationId}-raw`;

        await startMigration(
            config!.exchangeName,
            dto,
            "console.log(\"default\");"
        );

        const migration = getMigration(dto.migrationId)!;

        assert.equal(migration.consumer.consumerTag == null, false);
        assert.equal(migration.consumer.channel == null, false);
        assert.equal(migration.producer == null, false);

        await endConnection();
    });

    it("should receive raw scores and send scored data", async () => {
        await connect(config!);
        const policy = `
return {
    finalScore: rawScore.rawScore,
    adjustedSubmissionDate: new Date(),
    adjustedHoursLate: 0,
    submissionStatus: "on_time",
    extensionStatus: "no_extension"
};
`;
        assert.equal(ready(), true);

        const global = new GlobalAssignmentMetadata();
        global.assignmentId = randomUUID();
        global.maxScore = 10;
        global.minScore = 10;
        global.initialDueDate = new Date();

        const dto = new GradingStartDTO();
        dto.migrationId = randomUUID();
        dto.scoreCreatedRoutingKey = `${dto.migrationId}-score`;
        dto.rawGradeRoutingKey = `${dto.migrationId}-raw`;
        dto.globalMetadata = global;

        await startMigration(
            config!.exchangeName,
            dto,
            policy
        );

        let receivedData: ScoredDTO | null = null;

        const scoredReceiver = await connection!.createChannel().then(async (ch) => {
            const queue = await ch.assertQueue("");
            await ch.assertExchange(config!.exchangeName, "direct");
            await ch.bindQueue(
                queue.queue,
                config!.exchangeName,
                dto.scoreCreatedRoutingKey
            );

            await ch.consume(queue.queue, (msg: ConsumeMessage | null) => {
                if (!msg) {
                    return;
                }

                receivedData = JSON.parse(msg.content.toString()) as ScoredDTO;
            });

            return ch;
        });

        const rawScoreProducer = await connection!.createChannel().then(async (ch) => {
            const queue = await ch.assertQueue("");
            await ch.assertExchange(config!.exchangeName, "direct");
            await ch.bindQueue(
                queue.queue,
                config!.exchangeName,
                dto.rawGradeRoutingKey
            );

            return ch;
        });

        const rawScore = new RawScoreDTO();
        rawScore.cwid = "10000";
        rawScore.rawScore = 10;
        rawScore.submissionDate = new Date();

        rawScoreProducer.publish(config!.exchangeName, dto.rawGradeRoutingKey,
            Buffer.from(JSON.stringify(rawScore)),
            {
                contentType: "application/json",
                type: "grade.raw_score"
            }
        );

        await new Promise(resolve => setTimeout(resolve, 500));

        assert.notEqual(receivedData, null);
        assert.equal(receivedData!.cwid, rawScore.cwid);
        assert.equal(receivedData!.assignmentId, global.assignmentId);
        assert.equal(receivedData!.finalScore, rawScore.rawScore);


        await endConnection();
    });

    after(() => {
        connection?.close();
        rabbitMq?.stop();
    });
});
