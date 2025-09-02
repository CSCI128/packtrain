import { ExtensionStatus, SubmissionStatus } from "./common";

export default class PolicyScoredDTO {
    finalScore!: number;
    adjustedSubmissionDate!: Date;
    adjustedDaysLate!: number;
    submissionStatus!: SubmissionStatus;
    extensionStatus!: ExtensionStatus;
    extensionMessage?: string;
    submissionMessage?: string;
}
