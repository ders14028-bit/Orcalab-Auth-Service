#!/bin/bash
# User-data de arranque de las instancias del ASG de OrcaLab.
# Idempotente: puede correr en cada instancia nueva del grupo.
# Sin -x: el trace volcaria passwords al log.
set -euo pipefail
exec > /var/log/orcalab-userdata.log 2>&1

export DEBIAN_FRONTEND=noninteractive

# --- 1. Dependencias ---------------------------------------------------------
apt-get update -y
apt-get install -y docker.io docker-compose-v2 postgresql-client awscli curl
systemctl enable --now docker

mkdir -p /opt/orcalab

# --- 2. room_db en RDS (RDS solo crea auth_db; esto es idempotente) ----------
export PGPASSWORD='${db_password}'
until pg_isready -h ${rds_endpoint} -p 5432 -U ${db_username}; do sleep 5; done
psql -h ${rds_endpoint} -U ${db_username} -d auth_db -tc \
  "SELECT 1 FROM pg_database WHERE datname = 'room_db'" | grep -q 1 || \
  psql -h ${rds_endpoint} -U ${db_username} -d auth_db -c "CREATE DATABASE room_db"
unset PGPASSWORD

# --- 3. Configuracion de Kong, Nginx y compose de produccion -----------------
cat > /opt/orcalab/kong.yml <<'KONG_EOF'
${kong_config}
KONG_EOF

cat > /opt/orcalab/nginx.conf <<'NGINX_EOF'
${nginx_config}
NGINX_EOF

cat > /opt/orcalab/docker-compose.yml <<'COMPOSE_EOF'
${compose_config}
COMPOSE_EOF
chmod 600 /opt/orcalab/docker-compose.yml # contiene credenciales

# --- 4. Build del front (S3 staging privado, via LabInstanceProfile) ---------
mkdir -p /opt/orcalab/front-dist
aws s3 sync "s3://${front_bucket}" /opt/orcalab/front-dist --delete --region ${aws_region}

# --- 5. Login a ECR (credenciales via LabInstanceProfile) y arranque ---------
aws ecr get-login-password --region ${aws_region} | \
  docker login --username AWS --password-stdin ${ecr_registry}

cd /opt/orcalab
docker compose pull
docker compose up -d
