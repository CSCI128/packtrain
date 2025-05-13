export class GlobalAssignmentMetadata {
    assignmentId!: string;
    maxScore!: number;
    minScore!: number;
    initialDueDate!: Date;
}

export default class GradingStartDTO {
    migrationId!: string;
    policyURI!: string;
    scoreCreatedRoutingKey!: string;
    rawGradeRoutingKey!: string;

    globalMetadata!: GlobalAssignmentMetadata;
}
