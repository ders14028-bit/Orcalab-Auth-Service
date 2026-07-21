variable "aws_region" {
  description = "Región AWS (Learner Lab solo permite us-east-1)"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Prefijo para nombrar recursos"
  type        = string
  default     = "orcalab"
}

variable "vpc_cidr" {
  description = "CIDR de la VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  description = "AZs para las subnets (2 requeridas)"
  type        = list(string)
  default     = ["us-east-1a", "us-east-1b"]
}

variable "public_subnet_cidrs" {
  description = "CIDRs de las subnets públicas (ALB + NAT)"
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24"]
}

variable "private_subnet_cidrs" {
  description = "CIDRs de las subnets privadas (EC2 + bases de datos)"
  type        = list(string)
  default     = ["10.0.11.0/24", "10.0.12.0/24"]
}

# ---------------------------------------------------------------------------
# Credenciales de la capa de datos — valores reales en terraform.tfvars
# (gitignored), nunca en el repositorio.
# ---------------------------------------------------------------------------
variable "db_master_username" {
  description = "Usuario master de RDS PostgreSQL"
  type        = string
  default     = "orcalab_admin"
}

variable "db_master_password" {
  description = "Password master de RDS PostgreSQL"
  type        = string
  sensitive   = true
}

variable "mongo_master_username" {
  description = "Usuario root de MongoDB (EC2 dedicada)"
  type        = string
  default     = "orcalab_admin"
}

variable "mongo_master_password" {
  description = "Password root de MongoDB"
  type        = string
  sensitive   = true
}

variable "mongo_instance_type" {
  description = "Tipo de la EC2 dedicada a MongoDB"
  type        = string
  default     = "t3.small"
}

variable "jwt_secret" {
  description = "Secreto JWT compartido por los microservicios (min 256 bits)"
  type        = string
  sensitive   = true
}

variable "admin_seed_password" {
  description = "Password del usuario admin inicial de auth-service"
  type        = string
  sensitive   = true
  default     = "OrcaAdmin123!"
}

# Sin default (a diferencia de admin_seed_password): Grafana solo es alcanzable
# via SSM port-forward, pero sigue siendo un secreto real - debe vivir en
# terraform.tfvars (gitignored), igual que db_master_password/jwt_secret.
variable "grafana_admin_password" {
  description = "Password del usuario admin de Grafana"
  type        = string
  sensitive   = true
}

# ---------------------------------------------------------------------------
# Cómputo
# ---------------------------------------------------------------------------
variable "instance_type" {
  description = "Tipo de instancia del ASG (Kong + 4 Spring Boot ~2.5GB RAM => 4GB min). Subido a t3.large (2026-07-21): t3.medium se saturaba de CPU local (fan-out del broker STOMP) antes de agotar RAM."
  type        = string
  default     = "t3.large"
}

# Migrado a relay Redis Pub/Sub (2026-07-20, ver memoria realtime-redis-relay):
# realtime-service ya no depende de enableSimpleBroker en memoria para difundir
# entre instancias - chat/marcadores/alertas/voz-señalización se propagan via
# Redis a todas las réplicas. Vuelve a los valores de diseño originales: min=2,
# desired=2, max=4 (autoscaling por CPU ya definido en compute.tf).
# GAP CONOCIDO, no resuelto por el relay: VozService y PresenciaService (roster
# de voz y de presencia conectada) siguen en memoria local pura por instancia,
# sin sincronizar entre réplicas - a diferencia de SalaEstadoService, que sí
# reconstruye su estado completo desde el stream Redis. Con 2+ instancias el
# roster de "quién está conectado" o "quién está en la llamada" puede quedar
# incompleto según a qué instancia esté conectado cada usuario.
variable "asg_min_size" {
  type    = number
  default = 2
}

variable "asg_max_size" {
  type    = number
  default = 4
}

variable "asg_desired_capacity" {
  type    = number
  default = 2
}

# ---------------------------------------------------------------------------
# HTTPS del ALB
# ---------------------------------------------------------------------------
# ACM está permitido en el Learner Lab, pero request-certificate (validación DNS)
# y validación por email no son viables: DuckDNS no soporta el CNAME arbitrario
# que ACM exige, y los correos de validación (admin@/webmaster@...) van a
# duckdns.org, no a un buzón que controlemos. CloudFront (que daría HTTPS gratis
# vía *.cloudfront.net sin nada de esto) está bloqueado por IAM en este lab.
# Alternativa que sí funciona: certificado autofirmado importado directo a ACM
# (acm:ImportCertificate no requiere validación de dominio), generado con
# terraform/scripts/generate-alb-cert.sh. Ver limitación documentada en el README.
variable "alb_certificate_arn" {
  description = "ARN del certificado ACM que usa el listener HTTPS del ALB como default (hoy: Let's Encrypt de orcalab.duckdns.org)"
  type        = string
}

# Dominio propio (orcalab.online, comprado 2026-07-18) migrado en paralelo al de
# DuckDNS: certificado adicional via SNI (aws_lb_listener_certificate en
# loadbalancer.tf), sin tocar el default del listener - así ambos dominios
# sirven su certificado correcto al mismo tiempo mientras se confirma que el
# nuevo es estable. Cuando se decida el corte definitivo, este ARN pasa a
# alb_certificate_arn y este recurso se elimina.
variable "orcalab_online_certificate_arn" {
  description = "ARN del certificado ACM (Let's Encrypt) para orcalab.online, agregado via SNI al listener HTTPS"
  type        = string
}

# Origen real del front, para la whitelist de CORS de realtime-service/reporting-service
# (setAllowedOrigins, no wildcard) y de vision-service. CSV: ambos dominios
# habilitados en paralelo mientras se confirma la migración a orcalab.online;
# SecurityConfig.java ya lo splitea por coma (allowedOriginsCsv.split(",")).
variable "frontend_origin" {
  description = "Orígenes permitidos (CSV, scheme+host, sin trailing slash) del frontend en producción, para CORS"
  type        = string
  default     = "https://orcalab.duckdns.org,https://orcalab.online"
}
