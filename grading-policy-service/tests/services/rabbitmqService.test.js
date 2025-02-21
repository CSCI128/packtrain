"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const node_test_1 = require("node:test");
const node_test_2 = require("node:test");
const rabbitmq_1 = require("@testcontainers/rabbitmq");
const amqplib_1 = __importDefault(require("amqplib"));
const assert = __importStar(require("node:assert"));
const rabbitMqService_1 = require("../../src/services/rabbitMqService");
(0, node_test_1.describe)("RabbitMq Service", () => {
    const username = "admin";
    const password = "password";
    let config = null;
    let rabbitMq = null;
    let connection = null;
    (0, node_test_2.before)(() => __awaiter(void 0, void 0, void 0, function* () {
        rabbitMq = yield new rabbitmq_1.RabbitMQContainer()
            .withEnvironment({
            RABBITMQ_DEFAULT_USER: username,
            RABBITMQ_DEFAULT_PASS: password,
        })
            .start();
        config = {
            username: username,
            password: password,
            endpoint: rabbitMq.getHost(),
            port: rabbitMq.getMappedPort(5672).toString(),
            exchangeName: "default",
        };
        connection = yield amqplib_1.default.connect({
            username: username,
            password: password,
            port: rabbitMq.getMappedPort(5672),
        });
    }));
    (0, node_test_1.it)("should create connection", () => __awaiter(void 0, void 0, void 0, function* () {
        const actualConnection = yield (0, rabbitMqService_1.connect)(config);
        assert.equal(actualConnection == null, false);
    }));
    (0, node_test_2.after)(() => {
        connection === null || connection === void 0 ? void 0 : connection.close();
        rabbitMq === null || rabbitMq === void 0 ? void 0 : rabbitMq.stop();
    });
});
