import { Box, Button, Container, Divider, Text } from "@mantine/core";
import { store$ } from "@repo/api/store";
import { Link } from "react-router-dom";
import { formattedDate } from "../../../packages/ui/src/DateUtil";
import { components } from "../../admin/src/lib/api/v1";
import { $api } from "./api";

export function MigrationsPage() {
  const { data, error, isLoading } = $api.useQuery(
    "get",
    "/instructor/courses/{course_id}/migrations",
    {
      params: {
        path: { course_id: store$.id.get() as string },
      },
    }
  );

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
          data.map((migration: components["schemas"]["MasterMigration"]) => (
            <Box pb={20} key={migration.id}>
              <Text>
                <strong>
                  {formattedDate(new Date(migration.date_started as string))}
                </strong>
              </Text>
              {/* TODO send back name with CourseMember */}
              <Text>
                <strong>By</strong>: {migration.migrator?.cwid}
              </Text>
              <Text>
                <strong>Migrated Assignments</strong>:{" "}
                {migration.migrations
                  ?.map((migration: any) => migration.assignment.name)
                  .join(", ")}
              </Text>
            </Box>
          ))
        ) : (
          <>
            <Text>No migrations were found!</Text>
          </>
        )}

        <Button component={Link} to="/instructor/migrate/load" variant="filled">
          Migrate Grades
        </Button>
      </Container>
    </>
  );
}
