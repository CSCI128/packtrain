import axios from "axios";
import RawScoreDTO from "../data/RawScoreDTO";
import PolicyScoredDTO from "../data/PolicyScoredDTO";
import {AppliedExtensionStatus, SubmissionStatus} from "../data/common";

export type ApplyPolicyFunctionSig = (x: RawScoreDTO) => PolicyScoredDTO;

export interface ValidationResults {
    errors: string[];
    overallStatus: boolean;
}


function validateScoredDTO(scored: PolicyScoredDTO): string[] {
    const errors: string[] = [];

    // using loose equals here bc undefined == null which is helpful

    if (scored == null){
        errors.push("scored object was not set by policy! Ensure that you are returning correctly");
        return errors;
    }

    if (scored.finalScore == null){
        errors.push("finalScore was not set by policy!");
    }
    if (scored.finalScore != null && isNaN(Number(scored.finalScore.toString()))){
        errors.push(`finalScore was not a number! Received: ${scored.finalScore}!`);
    }

    if (scored.adjustedSubmissionDate == null){
        errors.push("adjustedSubmissionDate was not set by policy!");
    }

    if (scored.adjustedSubmissionDate != null && isNaN(new Date(scored.adjustedSubmissionDate.toString()).getDate())){
        errors.push(`adjustedSubmissionDate was not a valid date! Received: ${scored.adjustedSubmissionDate}. Expected: Any valid JS date`);
    }

    if (scored.adjustedDaysLate == null){
        errors.push("adjustedHoursLate was not set by policy!");
    }
    if (scored.adjustedDaysLate != null && isNaN(Number(scored.adjustedDaysLate.toString()))){
        errors.push(`adjustedHoursLate was not a number! Received: ${scored.adjustedDaysLate}!`);
    }

    if (scored.submissionStatus == null){
        errors.push("submissionStatus was not set by policy!");
    }

    if (scored.submissionStatus != null && !Object.values(SubmissionStatus).includes(scored.submissionStatus)){
        errors.push(`Invalid submissionStatus! Received: ${scored.submissionStatus}. Expected one of: ${Object.values(SubmissionStatus)}`);
    }

    if (scored.extensionStatus == null){
        errors.push("extensionStatus was not set by policy!");
    }

    if (scored.extensionStatus != null && !Object.values(AppliedExtensionStatus).includes(scored.extensionStatus)){
        errors.push(`Invalid extensionStatus! Received: ${scored.extensionStatus}. Expected one of: ${Object.values(SubmissionStatus)}`);
    }

    return errors;
}

export function verifyPolicy(fun: ApplyPolicyFunctionSig): ValidationResults{
    const rawScore = new RawScoreDTO();
    rawScore.cwid = "10000";
    rawScore.extensionDate = new Date();
    rawScore.submissionDate = new Date();
    rawScore.extensionId = "1";
    rawScore.assignmentId = "1";
    rawScore.rawScore = 10;
    rawScore.extensionDays = 0;
    rawScore.extensionType = "Late Pass";
    rawScore.submissionStatus = SubmissionStatus.ON_TIME;

    try {
        const scoredDTO = fun(rawScore);

        const errors = validateScoredDTO(scoredDTO);
        return {
            errors: errors,
            overallStatus: errors.length === 0,
        }
    } catch (e) {
        return {
            errors: [String(e)],
            overallStatus: false,
        };
    }
}

function compilePolicy(policyText: string): ApplyPolicyFunctionSig | string{
    try{
        return Function("rawScore", policyText) as ApplyPolicyFunctionSig;
    } catch (e) {
        return String(e)
    }
}

export async function downloadAndVerifyPolicy(
    uri: string,
): Promise<ApplyPolicyFunctionSig> {
    const res = await axios.get(uri);

    const functionOrError = compilePolicy(res.data as string);

    if(typeof functionOrError === "string"){
        return Promise.reject(`Invalid policy: ${functionOrError}`)
    }

    const {errors, overallStatus} = verifyPolicy(functionOrError);

    if (!overallStatus) {
        return Promise.reject(`Invalid policy! ${errors}`);
    }

    return functionOrError;
}
