resource "aws_mq_broker" "rabbitmq_broker" {
  broker_name                = "grading-rabbitmq"
  engine_type                = "RabbitMQ"
  engine_version             = "3.13"
  auto_minor_version_upgrade = true
  deployment_mode            = "SINGLE_INSTANCE"
  host_instance_type         = "mq.t3.micro"
  publicly_accessible        = false
  subnet_ids = [
    [for subnet in aws_subnet.private :
    subnet.id][0]
  ]
  security_groups = [aws_security_group.mq_sg.id]

  user {
    username = var.mq_username
    password = aws_secretsmanager_secret_version.s__rabbitmq_password.secret_string
  }
}

