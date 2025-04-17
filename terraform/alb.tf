resource "aws_lb" "packtrain" {
  subnets = [
    for subnet in aws_subnet.public :
    subnet.id

  ]
  security_groups            = [aws_security_group.lb_sg.id]
  load_balancer_type         = "application"
  internal                   = false
  enable_deletion_protection = false
  idle_timeout               = 60

}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.packtrain.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type = "fixed-response"
    fixed_response {
      content_type = "text/plain"
      message_body = "Not Found"
      status_code  = "404"
    }
  }
}

