import { Box, Button, Container, Divider, Text } from "@mantine/core";
import { getApiClient } from "@repo/api/index";
import { MasterMigration, Migration } from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { formattedDate } from "@repo/ui/DateUtil";
import { Loading } from "@repo/ui/Loading";
import { useMutation } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { useGetMasterMigration, useGetMasterMigrations } from "../../hooks";

export function MigrationsPage() {
  const navigate = useNavigate();
  const { data, error, isLoading } = useGetMasterMigrations();
  const { data: migrationData } = useGetMasterMigration();

  const createMasterMigration = useMutation({
    mutationKey: ["createMasterMigration"],
    mutationFn: () =>
      getApiClient()
        .then((client) =>
          client.create_master_migration({
            course_id: store$.id.get() as string,
          })
        )
        .then((res) => res.data)
        .catch((err) => console.log(err)),
  });

  const deleteMasterMigration = useMutation({
    mutationKey: ["deleteMasterMigration"],
    mutationFn: ({ master_migration_id }: { master_migration_id: string }) =>
      getApiClient()
        .then((client) =>
          client.delete_master_migration({
            course_id: store$.id.get() as string,
            master_migration_id: master_migration_id,
          })
        )
        .then((res) => res.data)
        .catch((err) => console.log(err)),
  });

  if (isLoading || !data) return <Loading />;

  if (error) return `An error occured: ${error}`;

  return (
    <>
      <Container size="md">
        <Text size="xl" fw={700}>
          Migrations
        </Text>

        <Divider my="sm" />

        {data.length > 0 ? (
          data.map((migration: MasterMigration) => (
            <Box pb={20} key={migration.id}>
              <Text>
                <strong>
                  {formattedDate(new Date(migration.date_started as string))}
                </strong>
              </Text>
              <Text>
                <strong>By</strong>: {migration.migrator?.name}
              </Text>
              <Text>
                <strong>Migrated Assignments</strong>:{" "}
                {migration.migrations
                  ?.map((migration: Migration) => migration.assignment.name)
                  .join(", ")}
              </Text>
              <Text>
                <strong>Status:</strong> {migration.status}
              </Text>
            </Box>
          ))
        ) : (
          <Text mb={10}>No migrations were found!</Text>
        )}

        <Divider pt={10} my="sm" />

        {store$.master_migration_id.get() ? (
          <>
            <Button
              mr={10}
              onClick={() => {
                deleteMasterMigration.mutate({
                  master_migration_id:
                    store$.master_migration_id.get() as string,
                });
                store$.master_migration_id.delete();
              }}
              variant="filled"
            >
              Delete active migration
            </Button>
            <Button
              onClick={() => {
                const status = migrationData?.status;

                if (status === "created") {
                  navigate("/instructor/migrate/load");
                } else if (status === "loaded") {
                  navigate("/instructor/migrate/apply");
                } else if (
                  status === "started" ||
                  status === "awaiting_review"
                ) {
                  navigate("/instructor/migrate/review");
                } else if (
                  status === "ready_to_post" ||
                  status === "posting" ||
                  status === "completed"
                ) {
                  navigate("/instructor/migrate/post");
                } else {
                  navigate("/instructor/migrate/load");
                }
              }}
              variant="filled"
            >
              Continue active migration
            </Button>
          </>
        ) : (
          <Button
            onClick={() => {
              createMasterMigration.mutate(undefined, {
                onSuccess: (data) => {
                  store$.master_migration_id.set(data?.id);
                  navigate("/instructor/migrate/load");
                },
              });
            }}
            variant="filled"
          >
            Migrate Grades
          </Button>
        )}
      </Container>
    </>
  );
}
