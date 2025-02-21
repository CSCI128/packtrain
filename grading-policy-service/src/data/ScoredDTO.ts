enum ExtensionStatus{
    IGNORED,
    APPLIED,
    REJECTED,
}

enum SubmissionStatus {
    MISSING,
    EXCUSED,
    LATE,
    EXTENDED,
    ON_TIME,
}

export default class ScoredDTO{
    cwid!: string;
    extensionId!: string;
    assignmentId!: string;
    rawScore!: number;
    finalScore!: number;
    adjustedSubmissionTime!: Date;
    hoursLate!: number;
    submissionStatus!: SubmissionStatus;
    extensionStatus!: ExtensionStatus;
    extensionMessage!: string;
    submissionMessage!: string;
}