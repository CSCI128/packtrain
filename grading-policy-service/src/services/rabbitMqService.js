"use strict";
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.connect = connect;
exports.ready = ready;
exports.startMigration = startMigration;
const amqplib_1 = require("amqplib");
const policyService_1 = require("./policyService");
const activeMigrations = new Map();
let connection = null;
function connect(rabbitMqConfig) {
    return __awaiter(this, void 0, void 0, function* () {
        if (connection !== null) {
            return Promise.reject("Connection already established");
        }
        const connectionURI = `amqp://${rabbitMqConfig.username}:${rabbitMqConfig.password}@${rabbitMqConfig.endpoint}:${rabbitMqConfig.port}`;
        connection = yield (0, amqplib_1.connect)(connectionURI);
    });
}
function ready() {
    return connection !== null;
}
function startMigration(exchangeName, start) {
    return __awaiter(this, void 0, void 0, function* () {
        if (connection === null) {
            return Promise.reject("Connection has not been established!");
        }
        if (activeMigrations.has(start.migrationId)) {
            return Promise.reject("Migration is already started!");
        }
        const policy = yield (0, policyService_1.downloadAndVerifyPolicy)(start.policyURI);
        const publishChannel = yield connection.createChannel()
            .then((ch) => __awaiter(this, void 0, void 0, function* () {
            const queue = yield ch.assertQueue("");
            yield ch.bindQueue(queue.queue, exchangeName, start.scoreCreatedRoutingKey);
            return publishChannel;
        }));
        const receiverChannel = yield connection.createChannel()
            .then((ch) => __awaiter(this, void 0, void 0, function* () {
            const queue = yield ch.assertQueue("");
            yield ch.bindQueue(queue.queue, exchangeName, start.rawGradeRoutingKey);
            const consumer = yield ch.consume(queue.queue, (msg) => {
                if (!msg) {
                    return;
                }
                if (msg.properties.type == "grade.raw_score" && msg.properties.contentType == "application/json") {
                    const content = JSON.parse(msg.content.toString());
                    onRawScoreReceive(exchangeName, start.scoreCreatedRoutingKey, start.globalMetadata, content, policy, publishChannel);
                }
            });
            return { channel: ch, consumerTag: consumer.consumerTag };
        }));
        activeMigrations.set(start.migrationId, { consumer: receiverChannel, producer: publishChannel });
        return Promise.resolve();
    });
}
function onRawScoreReceive(exchange, routingKey, assignmentMetadata, rawScore, policy, publishChannel) {
    rawScore.assignmentId = assignmentMetadata.assignmentId;
    rawScore.minScore = assignmentMetadata.minScore;
    rawScore.maxScore = assignmentMetadata.maxScore;
    rawScore.initialDueDate = assignmentMetadata.initialDueDate;
    try {
        const score = policy(rawScore);
        publishChannel.publish(exchange, routingKey, Buffer.from(JSON.stringify(score)), { contentType: "application/json", type: "grade.scored" });
    }
    catch (e) {
    }
}
