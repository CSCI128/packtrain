import { javascript } from "@codemirror/lang-javascript";
import {
  Accordion,
  Button,
  Center,
  Container,
  Group,
  Input,
  Modal,
  NativeSelect,
  NumberInput,
  Paper,
  Space,
  Stack,
  Tabs,
  Text,
  Textarea,
  TextInput,
} from "@mantine/core";
import { DateTimePicker } from "@mantine/dates";
import { useForm } from "@mantine/form";
import { useDisclosure } from "@mantine/hooks";
import { getApiClient } from "@repo/api/index";
import { PolicyDryRunResults, PolicyRawScore } from "@repo/api/openapi";
import { store$ } from "@repo/api/store";
import { useMutation } from "@tanstack/react-query";
import CodeMirror, { EditorState, Extension } from "@uiw/react-codemirror";
import { useState } from "react";
import { v4 as uuid } from "uuid";

export function PolicyPage({
  title,
  button,
  handleOnSubmit,
  errors,
  policyName,
  fileName,
  description,
  content,
}: {
  title: string;
  button: string;
  handleOnSubmit: (values: any) => void;
  errors: string[];
  policyName: string;
  fileName: string;
  description: string;
  content: string;
}) {
  const [dryRunRes, setDryRunRes] = useState<PolicyDryRunResults | null>(null);
  const dryRun = useMutation({
    mutationKey: ["dryRunPolicy"],
    mutationFn: ({
      file_data,
      raw_score,
    }: {
      file_data: File;
      raw_score: PolicyRawScore;
    }) => {
      getApiClient()
        .then((client) => {
          return client.dry_run_policy(
            {
              course_id: store$.id.get() as string,
            },
            {
              // @ts-expect-error OpenAPI expects a string, should be file
              file_data: file_data,
              // @ts-expect-error OpenAPI expects a json, but react is a hate crime so you have to do this slop
              raw_score: new Blob([JSON.stringify(raw_score)], {
                type: "application/json",
              }),
            },
            {
              headers: {
                "Content-Type": "multipart/form-data",
              },
            }
          );
        })
        .then((res) => {
          setDryRunRes(res.data);
        })
        .catch((err) => {
          setDryRunRes({ errors: [String(err)] });
        });
    },
  });

  const [testPolicyOpened, { open: openTestPolicy, close: closeTestPolicy }] =
    useDisclosure(false);

  const rawScoreForm = useForm({
    mode: "uncontrolled",
    initialValues: {
      raw_score: {
        cwid: "10000000",
        rawScore: 10,
        canvasMaxScore: 10,
        canvasMinScore: 0,
        externalMaxScore: 10,
        submissionDate: new Date(),
        initialDueDate: new Date(),
        submissionStatus: "on_time",
        extensionStatus: "no_extension",
        extensionId: null,
        extensionDate: null,
        extensionDays: null,
        extensionType: null,
      },
      policyContent: "",
    },
  });

  const form = useForm({
    mode: "uncontrolled",
    initialValues: {
      policyName: policyName,
      fileName: fileName,
      description: description,
      content: content,
    },
    validate: {
      policyName: (value) =>
        value.length < 1 ? "Policy name must be set" : null,
      fileName: (value) =>
        value.trim().length < 1 || /^(?!\w+\.js)/.test(value)
          ? "file name must be a valid js file name"
          : null,
      description: (value) =>
        value.trim().length < 1 || value.trim().length > 100
          ? "description must between 1 and 100 characters"
          : null,
      content: (value) =>
        value.trim().length < 1 ? "policy must be set!" : null,
    },
  });

  const handleDryRun = (values: typeof rawScoreForm.values) => {
    setDryRunRes(null);
    const file = new File([values.policyContent], `${uuid()}.js`, {
      type: "application/javascript",
    });

    dryRun.mutate({
      file_data: file,
      raw_score: values.raw_score as unknown as PolicyRawScore,
    });
  };

  const handleDryRunOpen = () => {
    setDryRunRes(null);
    rawScoreForm.setFieldValue("policyContent", form.getValues().content);
    openTestPolicy();
  };

  const dryRunComponents = () => (
    <>
      <Stack>
        <form onSubmit={rawScoreForm.onSubmit(handleDryRun)}>
          <Tabs defaultValue="raw_score">
            <Tabs.List>
              <Tabs.Tab value="policy">Policy</Tabs.Tab>
              <Tabs.Tab value="raw_score">Raw Score</Tabs.Tab>

              <Tabs.Tab value="extension_information">
                Extension Information
              </Tabs.Tab>

              <Tabs.Tab value="results">Results</Tabs.Tab>
            </Tabs.List>

            <Tabs.Panel value="policy">
              <Input.Wrapper label="Policy Contents" required>
                <Paper
                  withBorder
                  radius="md"
                  p={0}
                  style={{
                    borderColor: "#ced4da",
                    overflow: "hidden",
                  }}
                >
                  <CodeMirror
                    value={rawScoreForm.getValues().policyContent}
                    extensions={[javascript()]}
                    readOnly
                  />
                </Paper>
              </Input.Wrapper>
            </Tabs.Panel>
            <Tabs.Panel value="raw_score">
              <Group grow>
                <TextInput
                  pb={8}
                  label="CWID"
                  placeholder="User CWID"
                  key={rawScoreForm.key("raw_score.cwid")}
                  {...rawScoreForm.getInputProps("raw_score.cwid")}
                  required
                  disabled
                />
                <DateTimePicker
                  label="Submission Date"
                  valueFormat="MM/DD/YYYY hh:mm A"
                  placeholder="Submission Date"
                  key={rawScoreForm.key("raw_score.submissionDate")}
                  {...rawScoreForm.getInputProps("raw_score.submissionDate")}
                  required
                  firstDayOfWeek={0}
                />
              </Group>
              <Group grow>
                <NumberInput
                  pb={8}
                  label="Raw Score"
                  placeholder="Score"
                  key={rawScoreForm.key("raw_score.rawScore")}
                  {...rawScoreForm.getInputProps("raw_score.rawScore")}
                  required
                />
                <NativeSelect
                  label="Submission Status"
                  key={rawScoreForm.key("raw_score.submissionStatus")}
                  {...rawScoreForm.getInputProps("raw_score.submissionStatus")}
                  data={["missing", "late", "on_time"]}
                  required
                />
              </Group>
              <Group grow>
                <DateTimePicker
                  label="Initial Due Date"
                  placeholder="Initial Due Date"
                  valueFormat="MM/DD/YYYY hh:mm A"
                  key={rawScoreForm.key("raw_score.initialDueDate")}
                  {...rawScoreForm.getInputProps("raw_score.initialDueDate")}
                  required
                  firstDayOfWeek={0}
                />
                <NumberInput
                  pb={8}
                  label="Canvas Max Score"
                  placeholder="Score"
                  key={rawScoreForm.key("raw_score.canvasMaxScore")}
                  {...rawScoreForm.getInputProps("raw_score.canvasMaxScore")}
                  required
                />
                <NumberInput
                  pb={8}
                  label="Canvas Min Score"
                  placeholder="Score"
                  key={rawScoreForm.key("raw_score.canvasMinScore")}
                  {...rawScoreForm.getInputProps("raw_score.canvasMinScore")}
                  required
                />
                <NumberInput
                  pb={8}
                  label="External Max Score"
                  placeholder="Score"
                  key={rawScoreForm.key("raw_score.externalMaxScore")}
                  {...rawScoreForm.getInputProps("raw_score.externalMaxScore")}
                  required
                />
              </Group>
              <NativeSelect
                label="Extension Status"
                key={rawScoreForm.key("raw_score.extensionStatus")}
                {...rawScoreForm.getInputProps("raw_score.extensionStatus")}
                data={["approved", "rejected", "pending", "no_extension"]}
                required
              />
            </Tabs.Panel>

            <Tabs.Panel value="extension_information">
              {rawScoreForm.getValues().raw_score.extensionStatus ===
              "no_extension" ? (
                <Text>
                  To configure an extension, change to a status other than
                  "no_extension"
                </Text>
              ) : (
                <div>
                  <DateTimePicker
                    label="Extension Date"
                    valueFormat="MM/DD/YYYY hh:mm A"
                    placeholder="Extension Date"
                    key={rawScoreForm.key("raw_score.extensionDate")}
                    {...rawScoreForm.getInputProps("raw_score.extensionDate")}
                    required
                    firstDayOfWeek={0}
                  />
                  <NumberInput
                    pb={8}
                    label="Extension Days"
                    placeholder="Extension Days"
                    key={rawScoreForm.key("raw_score.extensionDays")}
                    {...rawScoreForm.getInputProps("raw_score.extensionDays")}
                    required
                  />
                  <TextInput
                    pb={8}
                    label="Extension Type"
                    placeholder="Extension Type"
                    key={rawScoreForm.key("raw_score.extensionType")}
                    {...rawScoreForm.getInputProps("raw_score.extensionType")}
                    required
                  />
                </div>
              )}
            </Tabs.Panel>

            <Tabs.Panel value="results">
              <Accordion defaultValue="scored">
                <Accordion.Item value="scored">
                  <Accordion.Control>Scored Results</Accordion.Control>
                  <Accordion.Panel>
                    {dryRunRes == null ? (
                      <Text c="indigo">Execute test to view results</Text>
                    ) : !dryRunRes.policyResults ? (
                      <Text c="red">No results were populated</Text>
                    ) : (
                      <div>
                        <Group grow>
                          <NumberInput
                            pb={8}
                            label="Final Score"
                            value={
                              dryRunRes.policyResults.finalScore ?? undefined
                            }
                            disabled
                          />
                          <DateTimePicker
                            label="Adjusted Submission Date"
                            valueFormat="MM/DD/YYYY hh:mm A"
                            value={
                              new Date(
                                dryRunRes.policyResults
                                  .adjustedSubmissionDate as string
                              )
                            }
                            disabled
                          />
                          <NumberInput
                            pb={8}
                            label="Adjusted Day Late"
                            value={dryRunRes.policyResults.adjustedDaysLate}
                            disabled
                          />
                        </Group>
                        <Group grow>
                          <TextInput
                            label="Submission Status"
                            value={
                              dryRunRes.policyResults.submissionStatus ??
                              undefined
                            }
                            disabled
                          />
                          <TextInput
                            label="Extension Status"
                            value={
                              dryRunRes.policyResults.extensionStatus ??
                              undefined
                            }
                            disabled
                          />
                        </Group>
                        <Textarea
                          label="Submission Message"
                          value={
                            dryRunRes.policyResults.submissionMessage ??
                            undefined
                          }
                          disabled
                        />
                        <Textarea
                          label="Extension Message"
                          value={
                            dryRunRes.policyResults.extensionMessage ??
                            undefined
                          }
                          disabled
                        />
                      </div>
                    )}
                  </Accordion.Panel>
                </Accordion.Item>
                <Accordion.Item value="errors">
                  <Accordion.Control>Errors</Accordion.Control>
                  <Accordion.Panel>
                    {dryRunRes == null ? (
                      <Text>No errors!</Text>
                    ) : (
                      <div>
                        {dryRunRes.errors?.map((e) => <Text c="red">{e}</Text>)}
                      </div>
                    )}
                  </Accordion.Panel>
                </Accordion.Item>
              </Accordion>
            </Tabs.Panel>
          </Tabs>

          <Space h="md" />

          <Center>
            <Button type="submit" fullWidth>
              Execute Test
            </Button>
          </Center>
        </form>
      </Stack>
    </>
  );

  // language=JavaScript
  const fullCode = `function policy(rawScore){\n${form.values.content}\n}`;

  const preventEditingWrapperLines = (): Extension =>
    EditorState.changeFilter.of((tr) => {
      const doc = tr.startState.doc;
      const firstLine = doc.line(1);
      const lastLine = doc.line(doc.lines);

      let blocked = false;

      tr.changes.iterChanges((fromA, toA) => {
        const fromLine = doc.lineAt(fromA).number;
        const toLine = doc.lineAt(toA).number;

        if (fromLine === firstLine.number || toLine === lastLine.number) {
          blocked = true;
        }
      });

      // Returning false blocks the entire change
      return !blocked;
    });

  return (
    <>
      <Container size="md">
        <Text size="xl" fw={700}>
          {title}
        </Text>

        <form onSubmit={form.onSubmit(handleOnSubmit)}>
          <TextInput
            pb={8}
            label="Policy Name"
            placeholder="New Policy Name"
            key={form.key("policyName")}
            {...form.getInputProps("policyName")}
            required
          />
          <TextInput
            pb={8}
            label="File name"
            placeholder="policy.js"
            key={form.key("fileName")}
            {...form.getInputProps("fileName")}
            required
          />
          <Textarea
            pb={8}
            label="Policy Description"
            placeholder="A high level overview of what this policy does"
            key={form.key("description")}
            {...form.getInputProps("description")}
            required
          />

          <Input.Wrapper label="Policy Contents" required>
            <Paper
              withBorder
              radius="md"
              p={0}
              style={{
                borderColor: "#ced4da",
                overflow: "hidden",
              }}
            >
              <CodeMirror
                value={fullCode}
                extensions={[javascript(), preventEditingWrapperLines()]}
                onChange={(c) =>
                  form.setFieldValue(
                    "content",
                    c.split("\n").slice(1, -1).join("\n")
                  )
                }
              />
            </Paper>
          </Input.Wrapper>

          <Space h="md" />

          <Group grow flex="flex-end">
            <Button variant="outline" onClick={handleDryRunOpen}>
              Test
            </Button>

            <Button type={"submit"}>{button}</Button>
          </Group>
        </form>

        {!errors ? (
          <div></div>
        ) : (
          <div>
            {errors.map((e) => (
              <Text>{e}</Text>
            ))}
          </div>
        )}
      </Container>

      <Modal
        opened={testPolicyOpened}
        onClose={closeTestPolicy}
        size="lg"
        title="Test Policy"
      >
        {dryRunComponents()}
      </Modal>
    </>
  );
}
