import { ExtensionStatus, SubmissionStatus } from "./common";

export default class ScoredDTO {
    cwid!: string;
    extensionId?: string;
    assignmentId!: string;
    rawScore!: number;
    finalScore!: number;
    adjustedSubmissionTime!: Date;
    daysLate!: number;
    submissionStatus!: SubmissionStatus;
    extensionStatus!: ExtensionStatus;
    extensionMessage!: string;
    submissionMessage!: string;
    numberExtensionDaysApplied!: number;
}
