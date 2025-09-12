import { getApiClient } from "@repo/api/index";
import { Enrollment, LateRequest, StudentInformation } from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { useQuery } from "@tanstack/react-query";

export function useGetEnrollments(queryEnabled: boolean) {
  return useQuery<Enrollment[]>({
    queryKey: ["getEnrollments"],
    queryFn: async () => {
      const client = await getApiClient();
      const res = await client.get_enrollments();
      return res.data;
    },
    enabled: queryEnabled,
  });
}

export function useGetExtensions() {
  return useQuery<LateRequest[]>({
    queryKey: ["getExtensions"],
    queryFn: async () => {
      const client = await getApiClient();
      const res = await client.get_all_extensions({
        course_id: store$.id.get() as string,
      });
      return res.data;
    },
  });
}

export function useGetCourseStudent() {
  return useQuery<StudentInformation>({
    queryKey: ["getCourse"],
    queryFn: async () => {
      const client = await getApiClient();
      const res = await client.get_course_information_student({
        course_id: store$.id.get() as string,
      });
      return res.data;
    },
  });
}
