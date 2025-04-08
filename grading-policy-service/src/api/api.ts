import express from "express";
import GradingStartDTO from "../data/GradingStartDTO";
import { GradingPolicyConfig } from "../config";
import { ready, startMigration } from "../services/rabbitMqService";

export function setup(config: GradingPolicyConfig, app: express.Application) {
    app.use(express.json());
    // keep this at base so health check works
    app.get(`/-/ready`, (req, res) => {
        if (!ready()) {
            res.sendStatus(500);
            return;
        }

        res.sendStatus(200);
    });

    app.get(`${config.serverConfig.basePath}/-/ready`, (req, res) => {
        if (!ready()) {
            res.sendStatus(500);
            return;
        }

        res.sendStatus(200);
    });
    app.post(`${config.serverConfig.basePath}/grading/start`, (req, res) => {
        if (!req.body) {
            res.sendStatus(400);
            return;
        }
        const body = req.body as GradingStartDTO;

        startMigration(config.rabbitMqConfig.exchangeName, body)
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
