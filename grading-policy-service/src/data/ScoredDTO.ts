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

class ScoredDTO{
    cwid: string;
    extensionId: string;
    rawScore: number;
    finalScore: number;
    adjustedSubmissionTime: Date;
    hoursLate: number;
    submissionStatus: SubmissionStatus;
    extensionStatus: ExtensionStatus;
    extensionMessage: string;
    submissionMessage: string;
}