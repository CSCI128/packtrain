import { AppliedExtensionStatus, SubmissionStatus } from "./common";

export default class PolicyScoredDTO {
    finalScore!: number;
    adjustedSubmissionDate!: Date;
    adjustedHoursLate!: number;
    submissionStatus!: SubmissionStatus;
    extensionStatus!: AppliedExtensionStatus;
    extensionMessage?: string;
    submissionMessage?: string;
}
