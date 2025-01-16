import requests
import time

class Authentik:
    def __init__(self, base, token):
        self.base = base
        self.token = token


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

    def _provider_exists(self, provider_name):
        res = self._get("providers/all/")

        if "results" not in res:
            return Exception("Failed to fetch providers")

        for provider in res["results"]:
            if provider["name"] == provider_name:
                return provider["pk"]
        return False

    def _application_exists(self, application_slug):
        res = self._get("core/applications/")

        if "results" not in res:
            return Exception("Failed to fetch applications")

        for application in res["results"]:
            if application["slug"] == application_slug:
                return application["pk"]
        return False


    def wait_for_service(self):
        attempt = 0
        while not self._check_health() and attempt <= 10:
            attempt += 1
            time.sleep(5)

        if not self._check_health():
            return False

        return True

    def create_new_provider(self, flow_name, provider_name, client_id):
        if provider_id := self._provider_exists(provider_name):
            return provider_id

        flow_id = self._get_flow_id(flow_name)

        body = {
            "name": provider_name,
            "authentication_flow": flow_id,
            "authorization_flow": flow_id,
            "invalidation_flow": flow_id,
            "client_type": "public",
            "client_id": client_id,
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
            "issuer_mode": "global",
            "jwt_federation_sources": [
            ],
            "jwt_federation_providers": [
            ]
        }

        res = self._post("providers/oauth2/", body)

        if "pk" not in res:
            raise Exception(f"Failed to create new provider with name: {provider_name}\n{res}")

        return res["pk"]

    def create_new_application(self, provider_id, application_name, slug):
        if application_id := self._application_exists(slug):
            return application_id

        body = {
            "name": application_name,
            "slug": slug,
            "provider": provider_id,
        }

        res = self._post("core/applications/", body)

        if "pk" not in res:
            raise Exception(f"Failed to create new application with name: {application_name}\n{res}")

        return res["pk"]

    # todo: seed users / roles / scopes
