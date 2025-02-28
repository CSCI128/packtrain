import express from "express";
import https from "https";
import { config } from "./config";
import { readFileSync } from "node:fs";
import { setup } from "./api/api";
import { connect } from "./services/rabbitMqService";

function main() {
    const serverOpts: https.ServerOptions = {
        key: readFileSync(config.securityConfig.serverKey),
        cert: readFileSync(config.securityConfig.serverCert),
        requestCert: true,
        rejectUnauthorized: false,
        ca: config.securityConfig.trustedCAs.map((ca) => readFileSync(ca)),
    };

    const app = setup(config, express());

    connect(config.rabbitMqConfig).then(() => {
        console.log("Connection Established!");
    });

    https.createServer(serverOpts, app).listen(config.port, () => {
        console.log(`Listening on :${config.port}`);
    });
}

main();
