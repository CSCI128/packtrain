import * as https from "node:https";
import { config } from "./config";
import * as fs from "node:fs";

export const agent =
    config?.serverConfig?.trustedCA != null
        ? new https.Agent({
              ca: fs.readFileSync(config.serverConfig.trustedCA),
          })
        : new https.Agent();
