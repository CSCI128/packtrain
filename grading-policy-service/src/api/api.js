"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.setup = setup;
const rabbitMqService_1 = require("../services/rabbitMqService");
function setup(config, app) {
    app.get("/-/ready", (req, res) => {
        // unauthenticated endpoint
        if (!(0, rabbitMqService_1.ready)()) {
            res.sendStatus(500);
            return;
        }
        res.sendStatus(200);
    });
    app.post("/grading/start", (req, res) => {
        if (!req.socket.authorized) {
            res.sendStatus(401);
        }
        if (!req.body) {
            res.sendStatus(400);
            return;
        }
        const body = req.body;
        (0, rabbitMqService_1.startMigration)(config.rabbitMqConfig.exchangeName, body)
            .then(() => {
            res.status(201);
            res.send({ status: "created" });
        })
            .catch((e) => {
            res.status(400);
            res.send({ status: "failed", reason: e });
        });
    });
    return app;
}
