import { Text } from "@mantine/core";
import { getApiClient } from "@repo/api/index";
import { store$ } from "@repo/api/store";
import { Loading } from "@repo/ui/components/Loading";
import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useGetPolcicy as useGetPolicy } from "../../hooks";
import { PolicyPage } from "./Policy";

export function UpdatePolicy() {
  const { policyId } = useParams();
  const navigate = useNavigate();
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
      setErrors([]);
      const res = await client.update_policy(
        {
          course_id: store$.id.get() as string,
          policy_id: policyId as string,
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

  const handleUpdatePolicy = (values: any) => {
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
        onSuccess: () => {
          navigate("/admin/");
        },
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
