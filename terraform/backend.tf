resource "aws_cloudwatch_log_group" "backend_service" {
  name              = "/ecs/backend-service"
  retention_in_days = 3
}
resource "aws_ecs_cluster" "backend" {
  name = "backend-cluster"
}

resource "aws_ecs_task_definition" "backend" {
  family                   = "backend"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "512"
  memory                   = "1024"
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn

  container_definitions = jsonencode([{
    name  = "backend"
    image = var.backend_image
    portMappings = [{
      containerPort = 8080
      hostPort      = 8080
      protocol      = "tcp"
    }],

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = "/ecs/backend-service"
        awslogs-region        = var.aws_region
        awslogs-stream-prefix = "ecs"
      }
    }
    environment = [
      { name = "DB_HOSTNAME", value = aws_db_instance.packtrain.address },
      { name = "PG_DB", value = var.packtrain_db_name },
      { name = "PG_USER", value = var.packtrain_db_user },
      { name = "PG_PASS", value = aws_secretsmanager_secret_version.s__packtrain_db_password.secret_string },
      { name = "RABBITMQ_USER", value = var.mq_username },
      { name = "RABBITMQ_PASSWORD", value = aws_secretsmanager_secret_version.s__rabbitmq_password.secret_string },
      { name = "RABBITMQ_HOST", value = aws_mq_broker.rabbitmq_broker.instances.0.endpoints.0 },
      { name = "AUTH_ISSUER", value = "https://${var.app_domain}/auth/application/o/grading-admin/" },
      { name = "POLCIY_SERVER_URI", value = aws_lb.policy_server_lb.dns_name }

    ]
    }
  ])
}

resource "aws_ecs_service" "backend" {
  name            = "backend-service"
  cluster         = aws_ecs_cluster.backend.id
  task_definition = aws_ecs_task_definition.backend.arn
  launch_type     = "FARGATE"
  desired_count   = 1

  network_configuration {
    subnets = [
      for subnet in aws_subnet.private :
      subnet.id
    ]
    security_groups  = [aws_security_group.backend_sg.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.backend_tg.arn
    container_name   = "backend"
    container_port   = 8080
  }
}

resource "aws_lb_target_group" "backend_tg" {
  name        = "backend-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = aws_vpc.packtrain_vpc.id
  target_type = "ip"

  health_check {
    path     = "/api/-/health"
    protocol = "HTTP"
  }
}

resource "aws_lb_listener_rule" "backend_http" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 10

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend_tg.arn
  }

  condition {
    path_pattern {
      values = ["/api/*"]
    }
  }
}
