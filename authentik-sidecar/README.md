# Authentik Sidecar

Basically, this sets authentik up for our usage.
It creates the applications, the users, the providers, the scope mappers, and the groups.

This runs automatically on startup, but if you make any changes, you'll need to run this.

### Building

```bash
docker compose build authenik-sidecar
```

### Running

```bash
docker compose run --rm authenik-sidecar --recreate
```

Adding the `--recreate` flag tells the sidecar to delete anything if it exists and recreate it.
This is necessary every time you make a change to an existing service.


## Adding Users
You may want to add non `akadmin` users.

1. Make a copy of the `users/user.json.template` file
2. Name that copy `<user_name>.json` and put it in the `users` folder.
3. Fill in all the attributes in the file.

Group names are `grading-admin-<group>`.
The available groups can be found under `src/groups/grading-admin`

## Adding Groups
You may want to add more groups.
> In order for them to be useful, you will need a mapper to apply any attributes on the group to the generated JWT.

You can create more groups by creating a file under the `source/groups/grading-admin` folder.

The file should, at a minimum, contain an empty json.
You can add attributes to that json, that be applied to any member of the group.
(see the `admin`) group for an example.