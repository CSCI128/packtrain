import {Box, Button, Container, Menu, Paper, Text, TextInput} from "@mantine/core";
import { useForm } from "@mantine/form";
import {$api, store$} from "../../../api.ts";
import CodeMirror, {EditorState, Extension} from "@uiw/react-codemirror";
import {javascript} from "@codemirror/lang-javascript";

export function CreatePolicy() {
  const createPolicyMutation = $api.useMutation("post", "/instructor/courses/{course_id}/policies");

  const createNewPolicy = (values: typeof form.values) => {
    const file = new File([values.content], values.fileName, {type: "application/javascript"});

    createPolicyMutation.mutate({
      params: {
        path:{
          course_id: store$.id.get() as string,
        }
      },
      body: {
        name: values.policyName,
        file_path: values.fileName,
        // https://github.com/openapi-ts/openapi-typescript/issues/1214
        file_data: file as unknown as string,
      }
    });

  };

  const form  = useForm({
    mode: "uncontrolled",
    initialValues: {
      policyName: "",
      fileName: "",
      content: "",
    },
    validate: {
      policyName: (value) =>
        value.length < 1 ? "Policy name must be set" : null,
      fileName: (value) =>
        value.trim().length < 1 || /^(?!\w+\.js)/.test(value) ? "file name must be a valid js file name" : null,
      content: (value) =>
        value.trim().length < 1 ? "policy must be set!" : null,
    }
  });

  // language=JavaScript
  const fullCode = `function policy(rawScore){\n${form.values.content}\n}`;

  const preventEditingWrapperLines = (): Extension  =>
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
    })
  return(
    <>
      <Container size="md">
        <form onSubmit={form.onSubmit(createNewPolicy)}>
          <TextInput
            pb={8}
            label="Policy Name"
            placeholder="New Policy Name"
            key={form.key("policyName")}
            {...form.getInputProps("policyName")}
          />
          <TextInput
            pb={8}
            label="File name"
            placeholder="policy.js"
            key={form.key("fileName")}
            {...form.getInputProps("fileName")}
          />

          <Text size={"md"}>
            Policy Contents
          </Text>
          <CodeMirror
            value={fullCode}
            extensions={[javascript(), preventEditingWrapperLines()]}
            onChange={c => form.setFieldValue("content", c.split("\n").slice(1, -1).join("\n"))}
          />

          <Button type={"submit"}>
            Create
          </Button>


        </form>


      </Container>

    </>
  );
}