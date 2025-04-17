variable "backend_image" {
  description = "Docker image for backend service"
  default     = "ghcr.io/csci128/packtrain/api:main"
  type        = string
}

resource "aws_ecs_cluster" "backend" {
  name = "backend-cluster"
}

resource "aws_ecs_task_definition" "backend" {
  family                   = "backend"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn

  container_definitions = jsonencode([{
    name  = "backend"
    image = var.backend_image
    portMappings = [{
      containerPort = 9000
      hostPort      = 9000
      protocol      = "tcp"
    }] }
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
    security_groups  = [aws_security_group.backend_sg]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.backend_tg.arn
    container_name   = "backend"
    container_port   = 9000
  }
}

resource "aws_lb_target_group" "backend_tg" {
  name        = "backend-tg"
  port        = 9000
  protocol    = "HTTP"
  vpc_id      = aws_vpc.packtrain_vpc.id
  target_type = "ip"

  health_check {
    path     = "/health"
    protocol = "HTTP"
  }
}

resource "aws_lb_listener_rule" "backend_http" {
  listener_arn = aws_lb_listener.http.arn

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
