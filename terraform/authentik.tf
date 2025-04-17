resource "aws_ecs_cluster" "authentik" {
  name = "authenik-cluster"
}

resource "aws_lb_target_group" "authentik" {
  name        = "authentik-tg"
  port        = 9000
  protocol    = "HTTP"
  vpc_id      = aws_vpc.packtrain_vpc.id
  target_type = "ip"

  health_check {
    path                = "/auth/-/health/live/"
    protocol            = "HTTP"
    matcher             = "200"
    interval            = 120
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }
}

resource "aws_cloudwatch_log_group" "authentik_server" {
  name              = "/ecs/authentik-server"
  retention_in_days = 3
}

resource "aws_lb_listener_rule" "authentik_http" {
  listener_arn = aws_lb_listener.http.arn

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.authentik.arn
  }

  condition {
    path_pattern {
      values = ["/auth/*"]
    }
  }
}

resource "aws_ecs_task_definition" "authentik" {
  family                   = "authentik"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "512"
  memory                   = "1024"
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn

  container_definitions = jsonencode([
    {
      name      = "authentik-server"
      image     = var.authentik_image
      essential = true
      command   = ["server"]
      portMappings = [{
        containerPort = 9000
        protocol      = "tcp"
      }]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = "/ecs/authentik-server"
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
      environment = [
        { name = "AUTHENTIK_POSTGRESQL__HOST", value = aws_db_instance.authentik.address },
        { name = "AUTHENTIK_POSTGRESQL__USER", value = var.authentik_db_user },
        { name = "AUTHENTIK_POSTGRESQL__NAME", value = var.authentik_db_name },
        { name = "AUTHENTIK_POSTGRESQL__PASSWORD", value = aws_secretsmanager_secret_version.s__authentik_db_password.secret_string },
        { name = "AUTHENTIK_SECRET_KEY", value = aws_secretsmanager_secret_version.s__authentik_secret_key.secret_string },
        { name = "AUTHENTIK_BOOTSTRAP_PASSWORD", value = aws_secretsmanager_secret_version.s__authentik_bootstrap_password.secret_string },
        { name = "AUTHENTIK_BOOTSTRAP_TOKEN", value = aws_secretsmanager_secret_version.s__authentik_bootstrap_token.secret_string },
        { name = "AUTHENTIK_REDIS__HOST", value = aws_elasticache_replication_group.authentik.primary_endpoint_address },
        { name = "AUTHENTIK_WEB__PATH", value = "/auth/" }
      ]
    },
    {
      name      = "authentik-worker"
      image     = var.authentik_image
      essential = false
      command   = ["worker"]
      environment = [
        { name = "AUTHENTIK_POSTGRESQL__HOST", value = aws_db_instance.authentik.address },
        { name = "AUTHENTIK_POSTGRESQL__USER", value = var.authentik_db_user },
        { name = "AUTHENTIK_POSTGRESQL__NAME", value = var.authentik_db_name },
        { name = "AUTHENTIK_POSTGRESQL__PASSWORD", value = aws_secretsmanager_secret_version.s__authentik_db_password.secret_string },
        { name = "AUTHENTIK_SECRET_KEY", value = aws_secretsmanager_secret_version.s__authentik_secret_key.secret_string },
        { name = "AUTHENTIK_BOOTSTRAP_PASSWORD", value = aws_secretsmanager_secret_version.s__authentik_bootstrap_password.secret_string },
        { name = "AUTHENTIK_BOOTSTRAP_TOKEN", value = aws_secretsmanager_secret_version.s__authentik_bootstrap_token.secret_string },
        { name = "AUTHENTIK_REDIS__HOST", value = aws_elasticache_replication_group.authentik.primary_endpoint_address },
        { name = "AUTHENTIK_WEB__PATH", value = "/auth/" }
      ]
    }
  ])
}


resource "aws_ecs_service" "authentik" {
  name            = "authentik-service"
  cluster         = aws_ecs_cluster.authentik.id
  task_definition = aws_ecs_task_definition.authentik.arn
  launch_type     = "FARGATE"
  desired_count   = 1

  network_configuration {
    subnets = [
      for subnet in aws_subnet.private :
      subnet.id

    ]
    security_groups  = [aws_security_group.authentik_sg.id]
    assign_public_ip = false
  }
  load_balancer {
    target_group_arn = aws_lb_target_group.authentik.arn
    container_name   = "authentik-server"
    container_port   = 9000
  }
}
