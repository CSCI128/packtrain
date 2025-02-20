import {readFileSync} from "node:fs";
import { parse } from "yaml";

interface RabbitMqConfig {
    username: string;
    password: string;
    endpoint: string;
    port: string;
}

interface PolicyConfig{
    trustedServer: string;
}

interface GradingPolicyConfig{
    policyConfig: PolicyConfig;
    rabbitMqConfig: RabbitMqConfig;
}

export const config: GradingPolicyConfig = parse(readFileSync("", "utf-8")) as GradingPolicyConfig;


