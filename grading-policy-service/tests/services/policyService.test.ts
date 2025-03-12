import { describe, it, before, after } from "node:test";
import { MinioContainer, StartedMinioContainer } from "@testcontainers/minio";
import * as Minio from "minio";
import { ApplyPolicyFunctionSig, downloadAndVerifyPolicy, verifyPolicy } from "../../src/services/policyService";
import * as assert from "node:assert";

describe("Policy Service", () => {
    const course_id = "000-0000-0000-00";
    const security_policy = JSON.stringify({
        Version: "2012-10-17",
        Statement: [
            {
                Action: [
                    "s3:GetObject"
                ],
                Effect: "Allow",
                Resource: `arn:aws:s3:::${course_id}/*`,
                Principal: "*"
            }
        ]
    });

    const access_key = "admin";
    const secret_key = "password";

    let minio: StartedMinioContainer | null = null;

    let client: Minio.Client | null = null;

    before(async () => {
        minio = await new MinioContainer()
            .withUsername(access_key)
            .withPassword(secret_key)
            .start();

        client = new Minio.Client({
            endPoint: minio?.getHost(),
            port: minio?.getPort(),
            useSSL: false,
            accessKey: access_key,
            secretKey: secret_key,
        });

        await client?.makeBucket(course_id)
        await client?.setBucketPolicy(course_id, security_policy);
    });

    it("should validate a valid policy", () => {
        const policy = `
return {
    finalScore: rawScore.rawScore,
    adjustedSubmissionDate: new Date(),
    adjustedHoursLate: 0,
    submissionStatus: "on_time",
    extensionStatus: "no_extension"
};
`;
        const f = Function("rawScore", policy) as ApplyPolicyFunctionSig;

        const res = verifyPolicy(f);

        assert.strictEqual(res.overallStatus, true);
        assert.strictEqual(res.errors.length, 0);
    });

    it("should reject an invalid policy", () =>{
        const policy = `
return {
    finalScore: "A",
    adjustedSubmissionDate: "invalid date",
    adjustedHoursLate: "zero",
    submissionStatus: "good"
};
`;
        const f = Function("rawScore", policy) as ApplyPolicyFunctionSig;

        const res = verifyPolicy(f);

        assert.strictEqual(res.overallStatus, false);
        assert.strictEqual(res.errors.length, 5);

    });

    it("should crash gracefully", () => {
        const msg = "i hate it here";
        const policy = `
        throw new Error("${msg}");
`;
        const f = Function("rawScore", policy) as ApplyPolicyFunctionSig;

        const res = verifyPolicy(f);

        assert.strictEqual(res.overallStatus, false);
        assert.strictEqual(res.errors.length, 1);
        assert.strictEqual(res.errors[0], `Error: ${msg}`);
    });

    it("should reject if policy doesn't return", () => {
        const policy = "";

        const f = Function("rawScore", policy) as ApplyPolicyFunctionSig;

        const res = verifyPolicy(f);

        assert.strictEqual(res.overallStatus, false);
        assert.strictEqual(res.errors.length, 1);
    });

    it("should download a policy from s3", async () => {
        assert.notEqual(client, null);
        const policy = `
return {
    finalScore: rawScore.rawScore,
    adjustedSubmissionDate: new Date(),
    adjustedHoursLate: 0,
    submissionStatus: "on_time",
    extensionStatus: "no_extension"
};
`;
        const policyName = "policy.js";

        await client?.putObject(course_id, policyName, policy);
        const url = `${minio?.getConnectionUrl()}/${course_id}/${policyName}`

        const res = await downloadAndVerifyPolicy(url);

        assert.equal(typeof res === "function", true);
    });

    after(() => {
        minio?.stop();
    })



});