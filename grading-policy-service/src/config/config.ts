import { readFileSync } from "node:fs";
import { parse } from "yaml";

export interface ServerConfig {
    port: number;
    basePath: string;
    trustedCA: string | null;
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
    serverConfig: ServerConfig | null;
    policyConfig: PolicyConfig | null;
    rabbitMqConfig: RabbitMqConfig | null;
}

export const config: GradingPolicyConfig = process.argv[2] != null
    ? parse(
        readFileSync(process.argv[2], "utf-8")
    ) as GradingPolicyConfig
    : {} as GradingPolicyConfig;
