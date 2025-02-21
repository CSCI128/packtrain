export default class RawScoreDTO{
    cwid!: string;
    extensionId!: string;
    assignmentId!: string;
    rawScore!: number;
    minScore!: number;
    maxScore!: number;
    initialDueDate!: Date;
    submissionDate!: Date;
    extensionDate!: Date;
    extensionHours!: number;
    extensionType!: string;
}