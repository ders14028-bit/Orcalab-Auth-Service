output "vpc_id" {
  description = "ID de la VPC de OrcaLab"
  value       = aws_vpc.main.id
}

output "public_subnet_ids" {
  description = "Subnets públicas (ALB)"
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "Subnets privadas (EC2 + datos)"
  value       = aws_subnet.private[*].id
}

output "nat_gateway_ip" {
  description = "IP pública del NAT Gateway"
  value       = aws_eip.nat.public_ip
}

output "front_bucket_name" {
  description = "Nombre del bucket S3 de staging privado del front (para aws s3 sync desde deploy-front.ps1; ya no es publico)"
  value       = aws_s3_bucket.front.id
}

output "asg_name" {
  description = "Nombre del Auto Scaling Group (para SSM RunCommand desde deploy-front.ps1)"
  value       = aws_autoscaling_group.app.name
}

output "alb_dns_name" {
  description = "URL unica y publica de OrcaLab — front + API + WebSocket, todo bajo este mismo DNS (http:// o https://<este-dns> — ver README para el paso manual de aceptar el certificado autofirmado)"
  value       = aws_lb.main.dns_name
}

output "rds_endpoint" {
  description = "Endpoint de RDS PostgreSQL (auth_db + room_db)"
  value       = aws_db_instance.postgres.address
}

output "mongo_endpoint" {
  description = "IP privada de la EC2 MongoDB (realtime_db + reporting_db)"
  value       = aws_instance.mongo.private_ip
}

output "redis_endpoint" {
  description = "Endpoint primario de ElastiCache Redis"
  value       = aws_elasticache_replication_group.redis.primary_endpoint_address
}

output "ecr_repository_urls" {
  description = "URLs de los repositorios ECR para push de imágenes"
  value       = { for k, r in aws_ecr_repository.service : k => r.repository_url }
}
