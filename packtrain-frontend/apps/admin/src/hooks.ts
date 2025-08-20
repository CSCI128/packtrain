import { getApiClient } from "@repo/api/index";
import {
  Course,
  CourseMember,
  Credential,
  Policy,
  User,
} from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { useQuery } from "@tanstack/react-query";

export function useGetCredentials() {
  return useQuery<Credential[]>({
    queryKey: ["getCredentials"],
    queryFn: () =>
      getApiClient()
        .then((client) => client.get_credentials())
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return null;
        }),
  });
}

export function useGetUsers() {
  return useQuery<User[]>({
    queryKey: ["getUsers"],
    queryFn: () =>
      getApiClient()
        .then((client) => client.get_all_users())
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return null;
        }),
  });
}

export function useGetCourse(
  includes: ("members" | "assignments" | "sections")[]
) {
  return useQuery<Course | null>({
    queryKey: ["getCourse"],
    queryFn: () =>
      getApiClient()
        .then((client) =>
          client.get_course({
            course_id: store$.id.get() as string,
            include: includes,
          })
        )
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return null;
        }),
  });
}

export function useGetMembers(
  enrollments: ("tas" | "instructors" | "students")[],
  queryKey: string
) {
  return useQuery<CourseMember[]>({
    queryKey: [queryKey],
    queryFn: () =>
      getApiClient()
        .then((client) =>
          client.get_members({
            course_id: store$.id.get() as string,
            enrollments: enrollments,
          })
        )
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return [];
        }),
  });
}

export function useGetPolicies() {
  return useQuery<Policy[]>({
    queryKey: ["getPolicies"],
    queryFn: () =>
      getApiClient()
        .then((client) =>
          client.owner_get_all_policies({
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
