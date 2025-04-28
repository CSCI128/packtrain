import { Box, Button, Container, Divider, Text } from "@mantine/core";
import { getApiClient } from "@repo/api/index";
import { MasterMigration, Migration } from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { formattedDate } from "@repo/ui/DateUtil";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";

export function MigrationsPage() {
  const { data, error, isLoading } = useQuery<MasterMigration[] | null>({
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

  if (isLoading || !data) return "Loading...";

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
            </Box>
          ))
        ) : (
          <>
            <Text mb={10}>No migrations were found!</Text>
          </>
        )}

        <Button component={Link} to="/instructor/migrate/load" variant="filled">
          Migrate Grades
        </Button>
      </Container>
    </>
  );
}
