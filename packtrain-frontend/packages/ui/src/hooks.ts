import { getApiClient } from "@repo/api/index";
import { Enrollment, User } from "@repo/api/openapi";
import { useQuery } from "@tanstack/react-query";

export function useGetUser(queryEnabled: boolean) {
  return useQuery<User>({
    queryKey: ["getUser"],
    queryFn: async () => {
      const client = await getApiClient();
      const res = await client.get_user();
      return res.data;
    },
    enabled: queryEnabled,
  });
}

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
