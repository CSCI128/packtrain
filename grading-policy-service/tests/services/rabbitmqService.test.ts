import {describe, it} from "node:test"
import {before, after} from "node:test"
import {RabbitMQContainer, StartedRabbitMQContainer} from "@testcontainers/rabbitmq";
import ampq from "amqplib";
import * as assert from "node:assert";
import {RabbitMqConfig} from "../../src/config";
import {connect, endConnection, getMigration, ready, startMigration} from "../../src/services/rabbitMqService"
import GradingStartDTO from "../../src/data/GradingStartDTO";
import {randomUUID} from "node:crypto";

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
                RABBITMQ_DEFAULT_PASS: password,
            })
            .start();

        config  = {
            username: username,
            password: password,
            endpoint: rabbitMq.getHost(),
            port: rabbitMq.getMappedPort(5672).toString(),
            exchangeName: "default",
        }
        connection = await ampq.connect({
            username: username,
            password: password,
            port: rabbitMq.getMappedPort(5672),
        });

    });

    it("should create connection", async () => {
        await connect(config!);

        assert.equal(ready(), true);

        await endConnection();
    });

    it("should create channels on migration start", async () =>{
        await connect(config!);

        assert.equal(ready(), true);


        const dto = new GradingStartDTO();
        dto.migrationId = randomUUID();
        dto.scoreCreatedRoutingKey = `${dto.migrationId}-score`;
        dto.rawGradeRoutingKey = `${dto.migrationId}-raw`;


        await startMigration(config!.exchangeName, dto, "console.log(\"default\");");

        const migration = getMigration(dto.migrationId)!;

        assert.equal(migration.consumer.consumerTag == null, false);
        assert.equal(migration.consumer.channel == null, false);
        assert.equal(migration.producer == null, false);

        await endConnection();
    })


    after(() => {
        connection?.close();
        rabbitMq?.stop();
    });
});