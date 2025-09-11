import { Text } from "@mantine/core";
import { getApiClient } from "@repo/api/index";
import { store$ } from "@repo/api/store";
import { useMutation } from "@tanstack/react-query";
import { useNavigate, useParams } from "react-router-dom";
import { useState } from "react";
import { PolicyPage } from "./Policy";
import { useGetPolcicy as useGetPolicy } from "../../hooks";
import { Loading } from "@repo/ui/components/Loading";

export function UpdatePolicy() {
  const {policyId} = useParams();
  const navigate = useNavigate();
  const [errors, setErrors] = useState<string[]>([]);

  const {
    data: policy,
    error: policyError,
    isLoading: policyIsLoading,
  } = useGetPolicy(policyId);

  const updatePolicy = useMutation({
    mutationKey: ["updatePolicy"],
    mutationFn: ({ formData }: { formData: FormData }) =>
      getApiClient()
        .then((client) => {
          setErrors([]);
          return client.update_policy(
            {
              course_id: store$.id.get() as string,
              policy_id: policyId,
            },
            {
              name: formData.get("name").toString(),
              file_path: formData.get("file_path").toString(),
              file_data: formData.get("file_data").toString(),
              description: formData.get("description").toString(),
            },
            {
              headers: {
                "Content-Type": "multipart/form-data",
              },
            }
          );
        })
        .then((res) => res.data)
        .catch((err) => {
          setErrors([String(err?.response?.data?.error_message)])
          throw err;
        }),
  });


  const handleUpdatePolicy = (values) => {
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

  return (
    policyIsLoading ?
      <Loading />
      :
      (policyError ? <Text>{policyError.message}</Text> :

        <PolicyPage
          title={`Edit Policy '${policy.name}'`}
          button={"Update"}
          handleOnSubmit={handleUpdatePolicy}
          errors={errors}
          policyName={policy.name}
          fileName={policy.file_path}
          content={policy.file_data}
          description={policy.description}
        />));

}
