import { getApiClient } from "@repo/api/index";
import {
  Course,
  CourseMember,
  LateRequest,
  MasterMigration,
  MigrationWithScores,
  Policy,
} from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { useQuery } from "@tanstack/react-query";

export function useGetMasterMigrations() {
  return useQuery<MasterMigration[] | null>({
    queryKey: ["getMigrations"],
    queryFn: () =>
      getApiClient()
        .then((client) =>
          client.get_all_migrations({
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

export function useGetMasterMigration() {
  return useQuery<MasterMigration | null>({
    queryKey: ["getMasterMigrations"],
    queryFn: () =>
      getApiClient()
        .then((client) =>
          client.get_master_migration({
            course_id: store$.id.get() as string,
            master_migration_id: store$.master_migration_id.get() as string,
          })
        )
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return null;
        }),
  });
}

export function useGetCourseInstructor() {
  return useQuery<Course | null>({
    queryKey: ["getCourse"],
    queryFn: () =>
      getApiClient()
        .then((client) =>
          client.get_course_information_instructor({
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

export function useGetMembersInstructor(
  enrollments: ("tas" | "instructors" | "students")[]
) {
  return useQuery<CourseMember[]>({
    queryKey: ["getMembers"],
    queryFn: () =>
      getApiClient()
        .then((client) =>
          client.get_members_instructor({
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
  return useQuery<Policy[] | null>({
    queryKey: ["getAllPolicies"],
    queryFn: () =>
      getApiClient()
        .then((client) =>
          client.get_all_policies({
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

export function useGetExtensions() {
  return useQuery<LateRequest[] | null>({
    queryKey: ["getExtensions"],
    queryFn: () =>
      getApiClient()
        .then((client) =>
          client.get_all_extensions_for_course({
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

export function useGetMigrationWithScores() {
  return useQuery<MigrationWithScores[] | null>({
    queryKey: ["getMigrationsWithScores"],
    queryFn: () =>
      getApiClient()
        .then((client) =>
          client.get_master_migration_to_review({
            course_id: store$.id.get() as string,
            master_migration_id: store$.master_migration_id.get() as string,
          })
        )
        .then((res) => res.data)
        .catch((err) => {
          console.log(err);
          return null;
        }),
  });
}
