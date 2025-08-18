import { getApiClient } from "@repo/api/index";
import { LateRequest, StudentInformation } from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { useQuery } from "@tanstack/react-query";

export function useGetExtensions() {
  return useQuery<LateRequest[] | null>({
    queryKey: ["getExtensions"],
    queryFn: () =>
      getApiClient()
        .then((client) =>
          client.get_all_extensions({
            course_id: store$.id.get() as string,
          })
        )
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return null;
        }),
  });
}

export function useGetCourseStudent() {
  return useQuery<StudentInformation | null>({
    queryKey: ["getCourse"],
    queryFn: () =>
      getApiClient()
        .then((client) =>
          client.get_course_information_student({
            course_id: store$.id.get() as string,
          })
        )
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return null;
        }),
  });
}
