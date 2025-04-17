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
    type = "redirect"
    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}

resource "aws_acm_certificate" "cert" {
  domain_name       = var.app_domain
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_acm_certificate_validation" "public" {
  certificate_arn         = aws_acm_certificate.cert.arn
  validation_record_fqdns = [
    for dvo in aws_acm_certificate.cert.domain_validation_options :
    dvo.resource_record_name
  ]
}


resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.packtrain.arn
  port              = "443"
  protocol          = "HTTPS"
  certificate_arn   = aws_acm_certificate_validation.public.certificate_arn

  default_action {
    type = "fixed-response"
    fixed_response {
      content_type = "text/plain"
      message_body = "Not Found"
      status_code  = "404"
    }
  }
}


