import { getApiClient } from "@repo/api/index";
import {
  Course,
  CourseMember,
  Credential,
  Policy,
  PolicyWithCode,
  User,
} from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { useQuery } from "@tanstack/react-query";

export function useGetCredentials() {
  return useQuery<Credential[]>({
    queryKey: ["getCredentials"],
    queryFn: async () => {
      const client = await getApiClient();
      const res = await client.get_credentials();
      return res.data;
    },
  });
}

export function useGetUsers() {
  return useQuery<User[]>({
    queryKey: ["getUsers"],
    queryFn: async () => {
      const client = await getApiClient();
      const res = await client.get_all_users();
      return res.data;
    },
  });
}

export function useGetCourse(
  includes: ("members" | "assignments" | "sections")[]
) {
  return useQuery<Course | null>({
    queryKey: ["getCourse"],
    queryFn: async () => {
      const client = await getApiClient();
      const res = await client.get_course({
        course_id: store$.id.get() as string,
        include: includes,
      });
      return res.data;
    },
  });
}

export function useGetMembers(
  enrollments: ("tas" | "instructors" | "students")[],
  queryKey: string
) {
  return useQuery<CourseMember[]>({
    queryKey: [queryKey],
    queryFn: async () => {
      const client = await getApiClient();
      const res = await client.get_members({
        course_id: store$.id.get() as string,
        enrollments: enrollments,
      });
      return res.data;
    },
  });
}

export function useGetPolicies() {
  return useQuery<Policy[]>({
    queryKey: ["getPolicies"],
    queryFn: async () => {
      const client = await getApiClient();
      const res = await client.owner_get_all_policies({
        course_id: store$.id.get() as string,
      });
      return res.data;
    },
  });
}

export function useGetPolicy(policyId: string) {
  return useQuery<PolicyWithCode>({
    queryKey: ["getPolicy"],
    queryFn: async () => {
      const client = await getApiClient();
      const res = await client.get_policy({
        course_id: store$.id.get() as string,
        policy_id: policyId,
      });
      return res.data;
    },
  });
}
