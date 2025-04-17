resource "aws_elasticache_subnet_group" "authentik" {
  name = "authentik-redis-subnet-group"
  subnet_ids = [
    for subnet in aws_subnet.private :
    subnet.id

  ]
}

resource "aws_elasticache_replication_group" "authentik" {
  replication_group_id       = "authentik-redis"
  description                = "Redis cache for authentik"
  engine                     = "redis"
  engine_version             = "6.x"
  node_type                  = "cache.t3.micro"
  num_cache_clusters         = 1
  automatic_failover_enabled = false

  subnet_group_name  = aws_elasticache_subnet_group.authentik.name
  security_group_ids = [aws_security_group.authentik_redis_sg.id]
}
