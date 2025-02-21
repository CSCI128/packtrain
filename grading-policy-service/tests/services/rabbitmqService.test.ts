import {describe, it} from "node:test"
import {before, after} from "node:test"
import {RabbitMQContainer, StartedRabbitMQContainer} from "@testcontainers/rabbitmq";
import ampq from "amqplib";
import * as assert from "node:assert";
import {RabbitMqConfig} from "../../src/config";
import {connect} from "../../src/services/rabbitMqService"



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
            gradingMessageRoutingKey: "grading",
        }
        connection = await ampq.connect({
            username: username,
            password: password,
            port: rabbitMq.getMappedPort(5672),
        });
    });

    it("should create connection", async () => {
        const actualConnection = await connect(config!);

        assert.equal(actualConnection == null, false);
    });

    after(() => {
        connection?.close();
        rabbitMq?.stop();
    });
});