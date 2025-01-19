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

    provider_id = client.create_new_provider(
        "default-provider-authorization-explicit-consent",
        "default-invalidation-flow",
        "grading_admin_provider",
        "grading_admin_provider"
    )

    application_id = client.create_new_application(
        provider_id,
        "Grading Admin",
        "grading-admin"
    )
