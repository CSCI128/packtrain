"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.config = void 0;
const node_fs_1 = require("node:fs");
const yaml_1 = require("yaml");
exports.config = (0, yaml_1.parse)((0, node_fs_1.readFileSync)(process.argv[2], "utf-8"));
