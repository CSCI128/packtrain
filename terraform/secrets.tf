# generate the secret
resource "random_password" "authentik_db_password" {
  length           = 64
  special          = true
  override_special = "!@#$%&*()-_=+[]{}<>?/"
}

# tell AWS that it is about to exist
resource "aws_secretsmanager_secret" "authentik_db_password" {
  name = "packtrain/authentik-db-password"
}

# push the secret to aws - use this to actually accesss the value 
resource "aws_secretsmanager_secret_version" "s__authentik_db_password" {
  secret_id     = aws_secretsmanager_secret.authentik_db_password.id
  secret_string = random_password.authentik_db_password.result
}

resource "random_password" "authentik_bootstrap_password" {
  length           = 64
  special          = true
  override_special = "!@#$%&*()-_=+[]{}<>?/"
}

resource "aws_secretsmanager_secret" "authentik_bootstrap_password" {
  name = "packtrain/authentik-bootstrap-password"
}

resource "aws_secretsmanager_secret_version" "s__authentik_bootstrap_password" {
  secret_id     = aws_secretsmanager_secret.authentik_bootstrap_password.id
  secret_string = random_password.authentik_bootstrap_password.result
}

resource "random_password" "authentik_bootstrap_token" {
  length           = 64
  special          = true
  override_special = "!@#$%&*()-_=+[]{}<>?/"
}

resource "aws_secretsmanager_secret" "authentik_bootstrap_token" {
  name = "packtrain/authentik-bootstrap-token"
}

resource "aws_secretsmanager_secret_version" "s__authentik_bootstrap_token" {
  secret_id     = aws_secretsmanager_secret.authentik_bootstrap_token.id
  secret_string = random_password.authentik_bootstrap_token.result
}

resource "random_password" "authentik_secret_key" {
  length           = 64
  special          = true
  override_special = "!@#$%&*()-_=+[]{}<>?/"
}

resource "aws_secretsmanager_secret" "authentik_secret_key" {
  name = "packtrain/authentik-secret-key"
}

resource "aws_secretsmanager_secret_version" "s__authentik_secret_key" {
  secret_id     = aws_secretsmanager_secret.authentik_secret_key.id
  secret_string = random_password.authentik_secret_key.result
}
