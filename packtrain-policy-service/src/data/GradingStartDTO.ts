export class GlobalAssignmentMetadata {
    assignmentId!: string;
    canvasMaxScore!: number;
    canvasMinScore!: number;
    externalMaxScore!: number;
    initialDueDate!: Date;
}

export default class GradingStartDTO {
    migrationId!: string;
    policyURI!: string;
    scoreCreatedRoutingKey!: string;
    rawGradeRoutingKey!: string;

    globalMetadata!: GlobalAssignmentMetadata;
}
