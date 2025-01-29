import requests
import time

class Authentik:
    def __init__(self, base, token, recreate=False):
        self.base = base
        self.token = token
        self.recreate = recreate


    def _post(self, endpoint, data, has_body=True):
        res = requests.post(
            url=f"{self.base}/api/v3/{endpoint}",
            json=data,
            headers={
                "Authorization": f"Bearer {self.token}"
            },
        )

        return res.json() if has_body else {}

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

    def _scope_exists(self, scope_name):
        res = self._get("propertymappings/provider/scope/", query={"scope_name": scope_name})

        if len(res["results"]) == 0 or "pk" not in res["results"][0]:
            return False

        return res["results"][0]["pk"]

    def _group_exists(self, group_name):
        res = self._get("core/groups/", query={"name": group_name})

        if len(res["results"]) == 0 or "pk" not in res["results"][0]:
            return False

        return res["results"][0]["pk"]

    def _user_exists(self, user_email):
        res = self._get("core/users/", query={"email": user_email})

        if len(res["results"]) == 0 or "pk" not in res["results"][0]:
            return False

        return res["results"][0]["pk"]

    def _get_scopes(self, requested_scopes):
        res = self._get(f"propertymappings/provider/scope/")

        scopes = []
        for scope in res["results"]:
            if scope["scope_name"] in requested_scopes:
                scopes.append(scope["pk"])

        if len(requested_scopes) != len(scopes):
            return []

        return scopes

    def wait_for_service(self):
        attempt = 0
        while not self._check_health() and attempt <= 15:
            print(f"Waiting for service. Attempt {attempt}/15")
            attempt += 1
            time.sleep(5)

        if not self._check_health():
            print(f"Failed to connect!")
            return False

        print(f"Service is healthy.")

        return True

    def delete_user_group_if_exists(self, name):
        if not (group_id := self._group_exists(name)):
            return

        print(f"Deleting group '{name}' with id '{group_id}'")

        if not self._delete(f"core/groups/{group_id}/"):
            raise Exception(f"Failed to delete group {name} with id {group_id}")

    def create_user_group(self, name, attributes):
        if self.recreate:
            self.delete_user_group_if_exists(name)

        if group_id := self._group_exists(name):
            print(f"Group '{name}' exists with id '{group_id}'! Skipping.")
            return group_id

        body = {
            "name": name,
            "is_superuser": False,
            "attributes": attributes,
        }

        res = self._post("core/groups/", body)

        if "pk" not in res:
            raise Exception(f"Failed to create new group with name: {name}\n{res}")

        print(f"Successfully created group with name '{name}'")

        return res["pk"]

    def delete_user_if_exists(self, email):
        if not (user_id := self._user_exists(email)):
            return

        print(f"Deleting user '{email}' with id '{user_id}'")

        if not self._delete(f"core/users/{user_id}/"):
            raise Exception(f"Failed to delete user {email} with id {user_id}")

    def create_user(self, name, email, groups, cwid):
        if self.recreate:
            self.delete_user_if_exists(email)

        if user_id := self._user_exists(email):
            print(f"User '{email}' exists with id '{user_id}'! Skipping.")
            return user_id

        body = {
            "username": email.split("@")[0],
            "name": name,
            "attributes": {
                "cwid": cwid,
            },
            "path": "users",
            "email": email,
            "groups": groups,
            "type": "internal",
        }

        res = self._post("core/users/", body)

        if "pk" not in res:
            raise Exception(f"Failed to create new user with email: {name}\n{email}")

        print(f"Successfully created user with email '{email}'")

        return res["pk"]


    def set_user_password(self, user_id, password):
        body = {
            "passwords": password
        }

        self._post(f"core/users/{user_id}/set_password", body, has_body=False)


    def delete_scope_mapper_if_exists(self, scope_name):
        if not (scope_id := self._scope_exists(scope_name)):
            return

        print(f"Deleting scope '{scope_name}' with id '{scope_id}'...")

        if not self._delete(f"propertymappings/provider/scope/{scope_id}/"):
            raise Exception(f"Failed to delete scope {scope_name} with id {scope_id}")

    def create_scope_mapper(self, name, scope_name, description, expression):
        if self.recreate:
            self.delete_scope_mapper_if_exists(scope_name)

        if scope_id := self._scope_exists(scope_name):
            print(f"Scope '{scope_name}' exists with id '{scope_id}'! Skipping.")
            return scope_id

        body = {
            "name": name,
            "scope_name": scope_name,
            "description": description,
            "expression": expression,
        }

        res = self._post("propertymappings/provider/scope/", body)

        if "pk" not in res:
            raise Exception(f"Failed to create new scope with name: {scope_name}\n{res}")

        print(f"Successfully created scope with name '{scope_name}'")

        return res["pk"]


    def delete_provider_if_exists(self, provider_name):
        if not (provider_id := self._provider_exists(provider_name)):
            return

        print(f"Deleting provider: {provider_name}")

        if not self._delete(f"providers/oauth2/{provider_id}/"):
            raise Exception(f"Failed to delete provider {provider_name} with id {provider_id}")


    def create_new_provider(self, authentication_flow_name, authorization_flow_name, invalidation_flow_name,  provider_name, client_id, scope_names):
        if self.recreate:
            self.delete_provider_if_exists(provider_name)

        if provider_id := self._provider_exists(provider_name):
            print(f"Provider '{provider_name}' already exists! Skipping!")
            return provider_id

        authentication_flow_id = self._get_flow_id(authentication_flow_name)
        authorization_flow_id = self._get_flow_id(authorization_flow_name)
        invalidation_flow_id = self._get_flow_id(invalidation_flow_name)

        signing_key_id = self._get_private_key_pair()

        scopes = self._get_scopes(scope_names)

        if not scopes:
            raise Exception("Failed to get required scopes!")

        body = {
            "name": provider_name,
            "authentication_flow": authentication_flow_id,
            "authorization_flow": authorization_flow_id,
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
            "property_mappings": scopes,
            "sub_mode": "user_uuid",  # going to use these UUIDs the user id in the users table
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
