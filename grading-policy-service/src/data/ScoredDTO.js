"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var ExtensionStatus;
(function (ExtensionStatus) {
    ExtensionStatus[ExtensionStatus["IGNORED"] = 0] = "IGNORED";
    ExtensionStatus[ExtensionStatus["APPLIED"] = 1] = "APPLIED";
    ExtensionStatus[ExtensionStatus["REJECTED"] = 2] = "REJECTED";
})(ExtensionStatus || (ExtensionStatus = {}));
var SubmissionStatus;
(function (SubmissionStatus) {
    SubmissionStatus[SubmissionStatus["MISSING"] = 0] = "MISSING";
    SubmissionStatus[SubmissionStatus["EXCUSED"] = 1] = "EXCUSED";
    SubmissionStatus[SubmissionStatus["LATE"] = 2] = "LATE";
    SubmissionStatus[SubmissionStatus["EXTENDED"] = 3] = "EXTENDED";
    SubmissionStatus[SubmissionStatus["ON_TIME"] = 4] = "ON_TIME";
})(SubmissionStatus || (SubmissionStatus = {}));
class ScoredDTO {
}
exports.default = ScoredDTO;
