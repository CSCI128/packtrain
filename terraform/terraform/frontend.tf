
resource "aws_ecs_cluster" "frontend" {
  name = "frontend-cluster"
}

resource "aws_ecs_task_definition" "frontend" {
  family                   = "frontend"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn

  container_definitions = jsonencode([
    {
      name  = "frontend-admin"
      image = var.frontend_admin_image
      portMappings = [{
        containerPort = 5173
        hostPort      = 5173
        protocol      = "tcp"
      }],
      environment = var.frontend_default_environment
    },
    {
      name  = "frontend-instructor"
      image = var.frontend_instructor_image
      portMappings = [{
        containerPort = 5174
        hostPort      = 5174
        protocol      = "tcp"
      }],
      environment = var.frontend_default_environment
    },
    {
      name  = "frontend-student"
      image = var.frontend_student_image
      portMappings = [{
        containerPort = 5175
        hostPort      = 5175
        protocol      = "tcp"
      }],
      environment = var.frontend_default_environment
    },
  ])
}

resource "aws_ecs_service" "frontend" {
  name            = "frontend-service"
  cluster         = aws_ecs_cluster.frontend.id
  task_definition = aws_ecs_task_definition.frontend.arn
  launch_type     = "FARGATE"
  desired_count   = 1

  network_configuration {
    subnets = [
      for subnet in aws_subnet.private :
      subnet.id
    ]
    security_groups  = [aws_security_group.frontend_sg.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.frontend_admin_tg.arn
    container_name   = "frontend-admin"
    container_port   = 5173
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.frontend_instructor_tg.arn
    container_name   = "frontend-instructor"
    container_port   = 5174
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.frontend_student_tg.arn
    container_name   = "frontend-student"
    container_port   = 5175
  }
}

resource "aws_lb_target_group" "frontend_admin_tg" {
  name        = "frontend-admin-tg"
  port        = 5173
  protocol    = "HTTP"
  vpc_id      = aws_vpc.packtrain_vpc.id
  target_type = "ip"

  health_check {
    path     = "/health"
    protocol = "HTTP"
  }
}

resource "aws_lb_target_group" "frontend_instructor_tg" {
  name        = "frontend-instructor-tg"
  port        = 5174
  protocol    = "HTTP"
  vpc_id      = aws_vpc.packtrain_vpc.id
  target_type = "ip"

  health_check {
    path     = "/health"
    protocol = "HTTP"
  }
}

resource "aws_lb_target_group" "frontend_student_tg" {
  name        = "frontend-student-tg"
  port        = 5175
  protocol    = "HTTP"
  vpc_id      = aws_vpc.packtrain_vpc.id
  target_type = "ip"

  health_check {
    path     = "/health"
    protocol = "HTTP"
  }
}

resource "aws_lb_listener_rule" "frontend_admin_http" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 30

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.frontend_admin_tg.arn
  }

  condition {
    path_pattern {
      values = ["/admin/*"]
    }
  }
}

resource "aws_lb_listener_rule" "frontend_instructor_http" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 20

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.frontend_instructor_tg.arn
  }

  condition {
    path_pattern {
      values = ["/instructor/*"]
    }
  }
}

resource "aws_lb_listener_rule" "frontend_student_http" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 100

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.frontend_student_tg.arn
  }

  condition {
    path_pattern {
      values = ["/*"]
    }
  }
}
