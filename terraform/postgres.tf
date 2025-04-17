resource "aws_db_subnet_group" "authentik" {
  name = "authentik_db_subnet_group"
  subnet_ids = [
    for subnet in aws_subnet.private :
    subnet.id

  ]
}

resource "aws_db_instance" "authentik" {
  identifier             = "authentik-db"
  engine                 = "postgres"
  engine_version         = "16"
  instance_class         = "db.t3.micro"
  allocated_storage      = 20
  username               = var.authentik_db_user
  password               = aws_secretsmanager_secret_version.s__authentik_db_password.secret_string
  db_name                = var.authentik_db_name
  vpc_security_group_ids = [aws_security_group.authentik_pg_sg.id]
  db_subnet_group_name   = aws_db_subnet_group.authentik.name
  skip_final_snapshot    = true
  publicly_accessible    = false
}
