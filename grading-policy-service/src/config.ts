import { readFileSync } from "node:fs";
import { parse } from "yaml";

export interface ServerConfig{
    port: number;
    basePath: string;
}

export interface RabbitMqConfig {
    username: string;
    password: string;
    endpoint: string;
    port: string;
    exchangeName: string;
}

export interface PolicyConfig {
    trustedServer: string;
}

export interface GradingPolicyConfig {
    serverConfig: ServerConfig;
    policyConfig: PolicyConfig;
    rabbitMqConfig: RabbitMqConfig;
}

export const config: GradingPolicyConfig = parse(
    readFileSync(process.argv[2], "utf-8"),
) as GradingPolicyConfig;
