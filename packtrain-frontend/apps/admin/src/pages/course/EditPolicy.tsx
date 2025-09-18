import { Text } from "@mantine/core";
import { getApiClient } from "@repo/api/index";
import { store$ } from "@repo/api/store";
import { Loading } from "@repo/ui/components/Loading";
import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useGetPolicy } from "../../hooks";
import { PolicyFormValues, PolicyPage } from "./Policy";

export function UpdatePolicy() {
  const navigate = useNavigate();
  const { policyId } = useParams();
  const [errors, setErrors] = useState<string[]>([]);
  const {
    data: policy,
    error: policyError,
    isLoading: policyIsLoading,
  } = useGetPolicy(policyId as string);

  const updatePolicy = useMutation({
    mutationKey: ["updatePolicy"],
    mutationFn: async ({ formData }: { formData: FormData }) => {
      const client = await getApiClient();
      const name = formData.get("name");
      const filePath = formData.get("file_path");
      const fileData = formData.get("file_data");
      const description = formData.get("description");
      setErrors([]);

      if (name && filePath && fileData && description) {
        const res = await client.update_policy(
          {
            course_id: store$.id.get() as string,
            policy_id: policyId as string,
          },
          {
            name: name.toString(),
            file_path: filePath.toString(),
            file_data: fileData.toString(),
            description: description.toString(),
          },
          {
            headers: {
              "Content-Type": "multipart/form-data",
            },
          }
        );
        return res.data;
      }
    },
    onError: (error) => {
      setErrors([error.message]);
    },
  });

  const handleUpdatePolicy = (values: PolicyFormValues) => {
    const formData = new FormData();
    formData.append("name", values.policyName);
    formData.append("file_path", values.fileName);
    formData.append("file_data", values.content);
    formData.append("description", values.description);

    updatePolicy.mutate(
      {
        formData,
      },
      {
        onSuccess: () => navigate("/admin/"),
      }
    );
  };

  return !policy || policyIsLoading ? (
    <Loading />
  ) : policyError ? (
    <Text>{policyError.message}</Text>
  ) : (
    <PolicyPage
      title={`Edit Policy '${policy.name}'`}
      button={"Update"}
      handleOnSubmit={handleUpdatePolicy}
      errors={errors}
      policyName={policy.name}
      fileName={policy.file_path}
      content={policy.file_data}
      description={policy.description}
    />
  );
}
