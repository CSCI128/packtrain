import requests
import time

class Authentik:
    def __init__(self, base, token, recreate=False):
        self.base = base
        self.token = token
        self.recreate = recreate


    def _post(self, endpoint, data):
        res = requests.post(
            url=f"{self.base}/api/v3/{endpoint}",
            json=data,
            headers={
                "Authorization": f"Bearer {self.token}"
            },
        )

        return res.json()

    def _get(self, endpoint, query=None):
        res = requests.get(
            url=f"{self.base}/api/v3/{endpoint}",
            params=query,
            headers={
                "Authorization": f"Bearer {self.token}"
            },
        )

        return res.json()

    def _delete(self, endpoint):
        res = requests.delete(
            url=f"{self.base}/api/v3/{endpoint}",
            headers={
                "Authorization": f"Bearer {self.token}"
            },
        )

        return res.ok

    def _check_health(self):
        res = requests.get(
            url=f"{self.base}/-/health/ready/",
            headers={
                "Authorization": f"Bearer {self.token}"
            },
        )

        return res.ok


    def _get_flow_id(self, flow_name):
        res = self._get(f"flows/instances/{flow_name}/")

        if "pk" not in res:
            raise Exception(f"Failed to get id for flow {flow_name}")

        return res["pk"]

    def _get_private_key_pair(self):
        res = self._get("crypto/certificatekeypairs/")

        if "results" not in res:
            raise Exception(f"Failed to get key pairs!")

        if len(res["results"]) == 0:
            raise Exception("No key pairs found!")

        # for now, return the first one

        return res["results"][0]["pk"]

    def _provider_exists(self, provider_name):
        res = self._get("providers/all/")

        if "results" not in res:
            return Exception("Failed to fetch providers")

        for provider in res["results"]:
            if provider["name"] == provider_name:
                return provider["pk"]
        return False

    def _application_exists(self, application_slug):
        # using the list endpoint seems to cache freshly deleted applications,
        # so basically we are just catching the 404 error
        res = self._get(f"core/applications/{application_slug}/")

        if "pk" not in res:
            return False

        return True

    def wait_for_service(self):
        attempt = 0
        # give authentik 10 secs to get ready
        time.sleep(10)
        while not self._check_health() and attempt <= 15:
            print(f"Waiting for service. Attempt {attempt}/15")
            attempt += 1
            time.sleep(5)

        if not self._check_health():
            print(f"Failed to connect!")
            return False

        print(f"Service is healthy.")

        return True

    def delete_provider_if_exists(self, provider_name):
        if not (provider_id := self._provider_exists(provider_name)):
            return

        print(f"Deleting provider: {provider_name}")

        if not self._delete(f"providers/oauth2/{provider_id}/"):
            raise Exception(f"Failed to delete provider {provider_name} with id {provider_id}")


    def create_new_provider(self, auth_flow_name, invalidation_flow_name,  provider_name, client_id):
        if self.recreate:
            self.delete_provider_if_exists(provider_name)

        if provider_id := self._provider_exists(provider_name):
            print(f"Provider '{provider_name}' already exists! Skipping!")
            return provider_id

        auth_flow_id = self._get_flow_id(auth_flow_name)
        invalidation_flow_id = self._get_flow_id(invalidation_flow_name)

        signing_key_id = self._get_private_key_pair()

        body = {
            "name": provider_name,
            "authentication_flow": auth_flow_id,
            "authorization_flow": auth_flow_id,
            "invalidation_flow": invalidation_flow_id,
            "client_type": "public",
            "client_id": client_id,
            "signing_key": signing_key_id,
            "access_code_validity": "days=1",
            "access_token_validity": "days=2",
            "refresh_token_validity": "days=30",
            "include_claims_in_id_token": True,
            "redirect_uris": [
                # im going to leave these hard coded
                {
                    "matching_mode": "regex",
                    "url": "https://localhost.dev/.*"
                },
                {
                    "matching_mode": "strict",
                    "url": "https://oauth.pstmn.io/v1/callback"
                }
            ],
            "sub_mode": "user_email",
            "issuer_mode": "per_provider",
            "jwt_federation_sources": [
            ],
            "jwt_federation_providers": [
            ]
        }

        res = self._post("providers/oauth2/", body)

        if "pk" not in res:
            raise Exception(f"Failed to create new provider with name: {provider_name}\n{res}")

        print(f"Successfully created provider with name {provider_name}")

        return res["pk"]


    def delete_application_if_exists(self, slug):
        if not self._application_exists(slug):
            return

        print(f"Deleting application: {slug}")

        if not self._delete(f"core/applications/{slug}/"):
            raise Exception(f"Failed to delete application {slug}")


    def create_new_application(self, provider_id, application_name, slug):
        if self.recreate:
            self.delete_application_if_exists(slug)

        if application_id := self._application_exists(slug):
            print(f"Application '{slug}' already exists! Skipping!")
            return application_id

        body = {
            "name": application_name,
            "slug": slug,
            "provider": provider_id,
        }

        res = self._post("core/applications/", body)

        if "pk" not in res:
            raise Exception(f"Failed to create new application with name: {application_name}\n{res}")

        print(f"Successfully created application with name {application_name}")
        return res["pk"]

    # todo: seed users / roles / scopes
