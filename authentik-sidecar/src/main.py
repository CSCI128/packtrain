import os

from authentik import Authentik

ENDPOINT = os.environ['AUTHENTIK_ENDPOINT']
TOKEN = os.environ['AUTHENTIK_BOOTSTRAP_TOKEN']

if __name__ == "__main__":
    client = Authentik(ENDPOINT, TOKEN)
    if not client.wait_for_service():
        exit(1)

    provider_id = client.create_new_provider(
        "default-provider-authorization-explicit-consent",
        "grading_admin_provider",
        "grading_admin_provider"
    )

    application_id = client.create_new_application(
        provider_id,
        "Grading Admin",
        "grading-admin"
    )


