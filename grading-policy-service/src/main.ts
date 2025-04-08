import express from "express";
import http from "http";
import {config} from "./config/config";
import {setup} from "./api/api";
import {connect} from "./services/rabbitMqService";

const app = setup(config, express());

connect(config.rabbitMqConfig).then(() => {
    console.log("RabbitMQ Connection Established!");
})
.catch(e => {
    console.error("Failed to connect to rabbitMQ", e)
});

http.createServer(app).listen(config.serverConfig.port, () => {
    console.log(`Listening on :${config.serverConfig.port} under path ${config.serverConfig.basePath}`);
});
