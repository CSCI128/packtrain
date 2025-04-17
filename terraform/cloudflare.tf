resource "cloudflare_dns_record" "packtrain" {
  zone_id = "286b94395485ea60ebc2da1639b50940"
  name    = var.app_domain
  content = aws_lb.packtrain.dns_name
  ttl     = 60
  type    = "CNAME"
}

resource "cloudflare_dns_record" "cert_validation" {
  for_each = {
    for dvo in aws_acm_certificate.cert.domain_validation_options :
    dvo.domain_name => dvo
  }

  zone_id  = "286b94395485ea60ebc2da1639b50940"
  name     = each.value.resource_record_name
  type     = each.value.resource_record_type
  content  = each.value.resource_record_value
  ttl      = 300
  proxied  = false
}
