import axios from "axios";
import ScoredDTO from "../data/ScoredDTO";
import RawScoreDTO from "../data/RawScoreDTO";

export type ApplyPolicyFunctionSig = (x: RawScoreDTO) => ScoredDTO;

function verifyPolicy(fun: ApplyPolicyFunctionSig): boolean{
    const rawScore = new RawScoreDTO();
    rawScore.cwid= "10000";
    rawScore.extensionDate = new Date();
    rawScore.submissionDate = new Date();
    rawScore.extensionId = "1";
    rawScore.assignmentId = "1"
    rawScore.rawScore = 10;
    rawScore.extensionHours = 0;
    rawScore.extensionType = "Late Pass";

    try{
        const f = fun(rawScore);
        return rawScore.cwid === f.cwid && rawScore.extensionId === f.extensionId && rawScore.assignmentId === f.assignmentId;
    }catch (e){
        return false;
    }
}

export async function downloadAndVerifyPolicy(uuid: string, uri: string): Promise<ApplyPolicyFunctionSig>{
    const res = await axios.get(uri);

    const f = Function(res.data as string) as ApplyPolicyFunctionSig;

    if (!verifyPolicy(f)){
        return Promise.reject("Invalid policy!")
    }

    return f;
}

