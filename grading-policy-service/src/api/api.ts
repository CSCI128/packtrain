import express from "express";
import GradingStartDTO from "../data/GradingStartDTO";
import ValidateDTO from "../data/ValidateDTO";
import { GradingPolicyConfig } from "../config/config";
import { ready, startMigration } from "../services/rabbitMqService";
import { downloadAndVerifyPolicy } from "../services/policyService";

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

    app.post(`${config.serverConfig.basePath}/validate`, (req, res) =>{
        if (!req.body) {
            res.sendStatus(400);
            return;
        }

        const body = req.body as ValidateDTO;

        downloadAndVerifyPolicy(body.policyURI)
          .then(_ => {
              res.status(200);
              res.send({status: "valid"});
          })
          .catch(e => {
              console.log(e)
              res.status(400);
              res.send({ status: "invalid", reason: e });
          });
    });

    return app;
}
