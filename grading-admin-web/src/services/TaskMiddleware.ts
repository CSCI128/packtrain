import {components} from "../lib/api/v1";
import {$api} from "../api.ts";

export async function waitForTaskCompletion(tasks: components["schemas"]["Task"][], onSuccess: () => void, onError: (name?: string, msg?: string) => void, onProgress: (name?: string) => void){
  let activeTaskIds = tasks.map(a => a.id);

  do{
    for (let i = 0; i < activeTaskIds.length; i++){
      const {
        data: taskData,
      } = $api.useQuery(
        "get",
        `/tasks/{task_id}`,
        {
          params: {
            path: { task_id: activeTaskIds[i]},
          },
        });

      if (taskData?.status === "FAILED"){
        onError(taskData?.name, taskData?.message);
        return;
      }

      if (taskData?.status === "COMPLETED"){
        onProgress(taskData?.name);
        activeTaskIds[i] = -1;
      }
    }

    // clean up solved tasks
    console.log(activeTaskIds);
    activeTaskIds = activeTaskIds.filter(id => id !== -1);
    await new Promise(f => setTimeout(f, 10000));

    if (activeTaskIds.length === 0)
      onSuccess();

  } while (activeTaskIds.length !== 0);

}