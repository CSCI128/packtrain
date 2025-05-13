import { AppliedExtensionStatus, SubmissionStatus } from "./common";

export default class PolicyScoredDTO {
    finalScore!: number;
    adjustedSubmissionDate!: Date;
    adjustedDaysLate!: number;
    submissionStatus!: SubmissionStatus;
    extensionStatus!: AppliedExtensionStatus;
    extensionMessage?: string;
    submissionMessage?: string;
}
