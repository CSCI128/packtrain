import { Box, Container, Divider, Text } from "@mantine/core";
import { $api, store$ } from "../../api";
import { components } from "../../lib/api/v1";
import { formattedDate } from "../../utils/DateUtil";

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
            <Box pb={10}>
              <Text>
                <strong>
                  {formattedDate(new Date(migration.timestamp as string))}
                </strong>
              </Text>
              {/* TODO send back name with CourseMember */}
              <Text>
                <strong>By</strong>: {migration.migrator?.cwid}
              </Text>
              <Text>
                <strong>Migrated Assignments</strong>:{" "}
                {migration.migration_list.join(", ")}
              </Text>
            </Box>
          ))
        ) : (
          <>
            <Text>No migrations were found!</Text>
          </>
        )}
      </Container>
    </>
  );
}
