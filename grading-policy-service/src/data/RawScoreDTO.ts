import { IncomingExtensionStatus } from "./common";

export default class RawScoreDTO {
    cwid!: string;
    assignmentId!: string;
    rawScore!: number;
    minScore!: number;
    maxScore!: number;
    initialDueDate!: Date;
    submissionDate!: Date;
    extensionId?: string;
    extensionDate?: Date;
    extensionHours?: number;
    extensionType?: string;
    extensionStatus?: IncomingExtensionStatus;
}
