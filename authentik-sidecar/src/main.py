import json
import os
import argparse

from authentik import Authentik

ENDPOINT = os.environ['AUTHENTIK_ENDPOINT']
TOKEN = os.environ['AUTHENTIK_BOOTSTRAP_TOKEN']


def parse_args():
    parser = argparse.ArgumentParser()

    parser.add_argument("--recreate", action="store_true")

    return parser.parse_args()


if __name__ == "__main__":
    args = parse_args()

    client = Authentik(ENDPOINT, TOKEN, recreate=args.recreate)

    print(vars(client))

    if not client.wait_for_service():
        raise Exception("Failed to connect to service")

    scopes_to_request = ["offline_access", "openid", "profile", "email",]

    for file in os.listdir(os.path.join("mappers", "grading-admin")):
        with open(os.path.join("mappers", "grading-admin", file)) as r:
            contents = r.read()
        client.create_scope_mapper(f"grading-admin/{file}", file, f"Read your '{file}' property", contents)

        scopes_to_request.append(file)

    available_groups = []

    for file in os.listdir(os.path.join("groups", "grading-admin")):
        with open(os.path.join("groups", "grading-admin", file)) as r:
            contents = json.load(r)

        group_id = client.create_user_group(f"grading-admin-{file}", contents)

        available_groups.append({"id": group_id, "name": f"grading-admin-{file}"})

    for file in os.listdir("users"):
        if "template" in file:
            continue

        with open(os.path.join("users", file)) as r:
            user_info = json.load(r)

        requested_groups = []

        for available_group in available_groups:
            if available_group["name"] in user_info["groups"]:
                requested_groups.append(available_group["id"])
        print(requested_groups)


        user_id = client.create_user(user_info["name"], user_info["email"], requested_groups, user_info["cwid"])
        client.set_user_password(user_id, user_info["password"])

    provider_id = client.create_new_provider(
        "default-authentication-flow",
        "default-provider-authorization-explicit-consent",
        "default-invalidation-flow",
        "grading_admin_provider",
        "grading_admin_provider",
        scopes_to_request
    )

    application_id = client.create_new_application(
        provider_id,
        "Grading Admin",
        "grading-admin"
    )
