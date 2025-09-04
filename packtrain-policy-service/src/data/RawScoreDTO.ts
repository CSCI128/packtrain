import { ExtensionStatus, SubmissionStatus } from "./common";

export default class RawScoreDTO {
    cwid!: string;
    assignmentId!: string;
    rawScore!: number;
    canvasMinScore!: number;
    canvasMaxScore!: number;
    externalMaxScore!: number;
    initialDueDate!: string;
    submissionDate!: string;
    submissionStatus!: SubmissionStatus;
    extensionId?: string;
    extensionDate?: string;
    extensionDays?: number;
    extensionType?: string;
    extensionStatus!: ExtensionStatus;
}
