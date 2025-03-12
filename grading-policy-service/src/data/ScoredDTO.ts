import { AppliedExtensionStatus, SubmissionStatus } from "./common";

export default class ScoredDTO {
    cwid!: string;
    extensionId?: string;
    assignmentId!: string;
    rawScore!: number;
    finalScore!: number;
    adjustedSubmissionTime!: Date;
    hoursLate!: number;
    submissionStatus!: SubmissionStatus;
    extensionStatus!: AppliedExtensionStatus;
    extensionMessage!: string;
    submissionMessage!: string;
}
