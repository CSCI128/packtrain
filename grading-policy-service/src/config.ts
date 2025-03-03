import { readFileSync } from "node:fs";
import { parse } from "yaml";

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
    policyConfig: PolicyConfig;
    rabbitMqConfig: RabbitMqConfig;

    port: number;
}

export const config: GradingPolicyConfig = parse(
    readFileSync(process.argv[2], "utf-8"),
) as GradingPolicyConfig;
