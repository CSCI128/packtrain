import { getApiClient } from "@repo/api/index";
import {
  Assignment,
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
  return useQuery<MasterMigration[]>({
    queryKey: ["getMigrations"],
    queryFn: async () => {
      const client = await getApiClient();
      const res = await client.get_all_migrations({
        course_id: store$.id.get() as string,
      });
      return res.data;
    },
  });
}

export function useGetMasterMigration() {
  return useQuery<MasterMigration>({
    queryKey: ["getMasterMigrations"],
    queryFn: async () => {
      const client = await getApiClient();
      const res = await client.get_master_migration({
        course_id: store$.id.get() as string,
        master_migration_id: store$.master_migration_id.get() as string,
      });
      return res.data;
    },
    enabled: store$.master_migration_id.get() !== undefined,
  });
}

export function useGetCourseInstructor() {
  return useQuery<Course>({
    queryKey: ["getCourse"],
    queryFn: async () => {
      const client = await getApiClient();
      const res = await client.get_course_information_instructor({
        course_id: store$.id.get() as string,
      });
      return res.data;
    },
  });
}

export function useGetMigratableAssignmentsInstructor() {
  return useQuery<Assignment[]>({
    queryKey: ["getCourseAssignmentsInstuctor"],
    queryFn: async () => {
      const client = await getApiClient();
      const res = await client.get_course_assignments_instuctor({
        course_id: store$.id.get() as string,
        only_migratable: true,
      });
      return res.data;
    },
  });
}

export function useGetMembersInstructor(
  enrollments: ("tas" | "instructors" | "students")[],
  queryKey: string
) {
  return useQuery<CourseMember[]>({
    queryKey: [queryKey],
    queryFn: async () => {
      const client = await getApiClient();
      const res = await client.get_members_instructor({
        course_id: store$.id.get() as string,
        enrollments: enrollments,
      });
      return res.data;
    },
  });
}

export function useGetPolicies() {
  return useQuery<Policy[]>({
    queryKey: ["getAllPolicies"],
    queryFn: async () => {
      const client = await getApiClient();
      const res = await client.get_all_policies({
        course_id: store$.id.get() as string,
      });
      return res.data;
    },
  });
}

export function useGetExtensions() {
  return useQuery<LateRequest[]>({
    queryKey: ["getExtensions"],
    queryFn: async () => {
      const client = await getApiClient();
      const res = await client.get_all_extensions_for_course({
        course_id: store$.id.get() as string,
      });
      return res.data;
    },
  });
}

export function useGetMigrationWithScores() {
  return useQuery<MigrationWithScores[]>({
    queryKey: ["getMigrationsWithScores"],
    queryFn: async () => {
      const client = await getApiClient();
      const res = await client.get_master_migration_to_review({
        course_id: store$.id.get() as string,
        master_migration_id: store$.master_migration_id.get() as string,
      });
      return res.data;
    },
  });
}
