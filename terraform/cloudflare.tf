resource "cloudflare_dns_record" "packtrain" {
  zone_id = "286b94395485ea60ebc2da1639b50940"
  name    = "packtrain"
  content = aws_lb.packtrain.dns_name
  ttl     = 60
  type    = "CNAME"
}
