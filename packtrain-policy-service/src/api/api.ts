import express from "express";
import multer from "multer";
import morgan from "morgan";
import GradingStartDTO from "../data/GradingStartDTO";
import ValidateDTO from "../data/ValidateDTO";
import { GradingPolicyConfig } from "../config/config";
import { ready, startMigration } from "../services/rabbitMqService";
import { downloadAndVerifyPolicy, policyDryRun } from "../services/policyService";
import RawScoreDTO from "../data/RawScoreDTO";
import fs from "node:fs";

export function setup(config: GradingPolicyConfig, app: express.Application) {
    app.use(morgan('combined'))

    app.use(express.json());
    // keep this at base so health check works
    app.get(`/-/ready`, (_, res) => {
        if (!ready()) {
            res.sendStatus(500);
            return;
        }

        res.sendStatus(200);
    });

    app.get(`${config.serverConfig!.basePath}/-/ready`, (_, res) => {
        if (!ready()) {
            res.sendStatus(500);
            return;
        }

        res.sendStatus(200);
    });
    app.post(`${config.serverConfig!.basePath}/grading/start`, (req, res) => {
        if (!req.body) {
            res.sendStatus(400);
            return;
        }
        const body = req.body as GradingStartDTO;

        startMigration(config.rabbitMqConfig!.exchangeName, body)
            .then(() => {
                res.status(201);
                res.send({ status: "created" });
            })
            .catch((e) => {
                res.status(400);
                res.send({ status: "failed", reason: e });
            });
    });

    app.post(`${config.serverConfig!.basePath}/validate`, (req, res) => {
        if (!req.body) {
            res.sendStatus(400);
            return;
        }

        const body = req.body as ValidateDTO;

        downloadAndVerifyPolicy(body.policyURI)
            // eslint-disable-next-line @typescript-eslint/no-unused-vars
            .then((_) => {
                res.status(200);
                res.send({ status: "valid" });
            })
            .catch((e) => {
                console.log(e);
                res.status(400);
                res.send({ status: "invalid", reason: e });
            });
    });


    const upload = multer({dest: "/dry-run"})

    app.post(`${config.serverConfig!.basePath}/dry-run`, upload.single("file"), (req, res) => {
        if (!req.body || !req.body.raw_score){
            res.sendStatus(400);
            return;
        }

        if (!req.file){
            res.sendStatus(400);
            return;
        }

        const body = JSON.parse(req.body.raw_score) as RawScoreDTO;
        const javascriptFile = req.file;

        const policyContent = fs.readFileSync(javascriptFile.path, 'utf8');

        const dry_run_res = policyDryRun(policyContent, body);

        res.status(200);
        res.send(dry_run_res);
    })

    return app;
}
