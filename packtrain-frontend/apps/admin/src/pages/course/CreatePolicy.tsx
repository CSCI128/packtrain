import { getApiClient } from "@repo/api/index";
import { store$ } from "@repo/api/store";
import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { PolicyFormValues, PolicyPage } from "./Policy";

export function CreatePolicy() {
  const navigate = useNavigate();
  const [errors, setErrors] = useState<string[]>([]);

  const newPolicy = useMutation({
    mutationKey: ["newPolicy"],
    mutationFn: async ({ formData }: { formData: FormData }) => {
      const client = await getApiClient();
      setErrors([]);
      const res = await client.new_policy(
        {
          course_id: store$.id.get() as string,
        },
        {
          name: formData.get("name")?.toString() ?? "",
          file_path: formData.get("file_path")?.toString() ?? "",
          file_data: formData.get("file_data")?.toString() ?? "",
          description: formData.get("description")?.toString() ?? "",
        },
        {
          headers: {
            "Content-Type": "multipart/form-data",
          },
        }
      );
      return res.data;
    },
    onError: (error) => {
      setErrors([error.message]);
    },
  });

  const handleNewPolicy = (values: PolicyFormValues) => {
    const formData = new FormData();
    formData.append("name", values.policyName);
    formData.append("file_path", values.fileName);
    formData.append("file_data", values.content);
    formData.append("description", values.description);

    newPolicy.mutate(
      {
        formData,
      },
      {
        onSuccess: () => navigate("/admin/"),
      }
    );
  };

  return (
    <PolicyPage
      title="Create a new Policy"
      handleOnSubmit={handleNewPolicy}
      button={"Create"}
      errors={errors}
      policyName=""
      fileName=""
      content=""
      description=""
    />
  );
}
