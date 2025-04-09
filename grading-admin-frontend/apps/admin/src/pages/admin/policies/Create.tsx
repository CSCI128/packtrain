import {Button, Container, Input, Paper, Space, Textarea, TextInput} from "@mantine/core";
import {useForm} from "@mantine/form";
import {store$, userManager} from "../../../api.ts";
import CodeMirror, {EditorState, Extension} from "@uiw/react-codemirror";
import {javascript} from "@codemirror/lang-javascript";
import axios from "axios";
import {useNavigate} from "react-router-dom";

export function CreatePolicy() {
  const navigate = useNavigate();
  let errors = "";

  const createNewPolicy = (values: typeof form.values) => {
    const file = new File([values.content], values.fileName, {type: "application/javascript"});
    userManager.getUser().then(u => {
        if (u == null) {
          throw new Error("Failed to get user");
        }

        const formData = new FormData();

        formData.append("name", values.policyName);
        formData.append("file_path", values.fileName);
        formData.append("file_data", file);
        formData.append("description", values.description);

        axios.post(`/api/instructor/courses/${store$.id.get()}/policies`, formData, {
          headers: {
            "authorization": `Bearer ${u.access_token}`,
            "content-type": "multipart/form-data",
          },
        }).then(_ => navigate("/admin/home"))
          .catch(e => errors = e);
      }
    )


  };

  const form = useForm({
    mode: "uncontrolled",
    initialValues: {
      policyName: "",
      fileName: "",
      description: "",
      content: "",
    },
    validate: {
      policyName: (value) =>
        value.length < 1 ? "Policy name must be set" : null,
      fileName: (value) =>
        value.trim().length < 1 || /^(?!\w+\.js)/.test(value) ? "file name must be a valid js file name" : null,
      description: (value) =>
        value.trim().length < 1 || value.trim().length > 100 ? "description must between 1 and 100 characters": null,
      content: (value) =>
        value.trim().length < 1 ? "policy must be set!" : null,
    }
  });

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
    })
  return (
    <>
      <Container size="md">
        {errors != null &&
            errors
        }

        <form onSubmit={form.onSubmit(createNewPolicy)}>
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
                borderColor: '#ced4da',
                overflow: 'hidden',
              }}
            >
              <CodeMirror
                value={fullCode}
                extensions={[javascript(), preventEditingWrapperLines()]}
                onChange={c => form.setFieldValue("content", c.split("\n").slice(1, -1).join("\n"))}
              />
            </Paper>
          </Input.Wrapper>

          <Space h="md"/>

          <Button type={"submit"}>
            Create
          </Button>

        </form>


      </Container>

    </>
  );
}