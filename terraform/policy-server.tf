locals {
  config_data = {
    serverConfig = {
      port      = 80
      basePath  = "/api/policies"
      trustedCA = "/opt/certs/localhost.CA.crt"
    }

    policyConfig = {
      trustedServer = "https://localhost.dev"
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

resource "aws_ecs_cluster" "policy-server" {
  name = "policy-server-cluster"
}

resource "aws_ecs_task_definition" "policy-server" {
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

resource "aws_ecs_service" "policy-server" {
  name            = "grading-policy-server"
  cluster         = aws_ecs_cluster.policy-server.id
  task_definition = aws_ecs_task_definition.policy-server.arn
  launch_type     = "FARGATE"
  desired_count   = 1

  network_configuration {
    subnets = [
      for subnet in aws_subnet.private :
      subnet.id

    ]
    security_groups  = [aws_security_group.policy-server-sg.id]
    assign_public_ip = false
  }
}
