import { IncomingExtensionStatus, SubmissionStatus } from "./common";

export default class RawScoreDTO {
    cwid!: string;
    assignmentId!: string;
    rawScore!: number;
    minScore!: number;
    maxScore!: number;
    initialDueDate!: Date;
    submissionDate!: Date;
    submissionStatus!: SubmissionStatus;
    extensionId?: string;
    extensionDate?: Date;
    extensionDays?: number;
    extensionType?: string;
    extensionStatus?: IncomingExtensionStatus;
}
