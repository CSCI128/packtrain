locals {
  config_data = {
    serverConfig = {
      port      = 80
      basePath  = "/"
      trustedCA = null
    }

    policyConfig = {
      trustedServer = "https://${var.app_domain}"
    }

    rabbitMqConfig = {
      username     = "admin"
      password     = "changeme!"
      endpoint     = "localhost.dev"
      exchangeName = "exchange"
      port         = 5672
    }
  }

  config_yaml = yamlencode(local.config_data)
}

resource "aws_ecs_cluster" "policy_server" {
  name = "policy-server-cluster"
}

resource "aws_ecs_task_definition" "policy_server" {
  family                   = "policy-server"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn

  container_definitions = jsonencode([
    {
      name      = "grading-policy-server"
      image     = var.policy_image
      essential = true
      portMappings = [{
        containerPort = 80
        hostPort = 80
        protocol      = "tcp"
      }]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = "/ecs/grading-policy-server"
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
      environment = [
        {
          name  = "CONFIG_YAML"
          value = local.config_yaml
        }
      ],
      command = [
        "sh", "-c",
        "echo \"$CONFIG_YAML\" > /app/config.yaml"
      ]
    },
  ])
}

resource "aws_ecs_service" "policy_server" {
  name            = "grading-policy-server"
  cluster         = aws_ecs_cluster.policy_server.id
  task_definition = aws_ecs_task_definition.policy_server.arn
  launch_type     = "FARGATE"
  desired_count   = 1

  network_configuration {
    subnets = [
      for subnet in aws_subnet.private :
      subnet.id

    ]
    security_groups  = [aws_security_group.policy_server_sg.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.policy_server.arn
    container_name = "grading-policy-server"
    container_port = 80
  }
}

resource "aws_lb" "policy_server_lb" {
  name = "grading-policy-service-lb"
  load_balancer_type = "application"
  internal = true
  security_groups = [aws_security_group.lb_sg.id]
  subnets = [
    for subnet in aws_subnet.private :
    subnet.id

  ]
}

resource "aws_lb_target_group" "policy_server" {
  name = "policy-service-tg"
  port = 80
  protocol = "HTTP"
  vpc_id = aws_vpc.packtrain_vpc.id
  target_type = "ip"

  health_check {
    path = "/-/ready"
    protocol = "HTTP"
    matcher = 200

  }
}

resource "aws_lb_listener" "policy_server_http" {
  load_balancer_arn = aws_lb.policy_server_lb.arn
  port = "80"
  protocol = "HTTP"

  default_action {
    type = "forward"
    target_group_arn = aws_lb_target_group.policy_server.arn
  }
}

