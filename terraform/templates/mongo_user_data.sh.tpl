#!/bin/bash
# Arranque de la instancia dedicada a MongoDB.
# Sin -x: el trace volcaria el password al log.
set -euo pipefail
exec > /var/log/orcalab-mongo-userdata.log 2>&1

export DEBIAN_FRONTEND=noninteractive
apt-get update -y
apt-get install -y docker.io
systemctl enable --now docker

# Mongo 7 (misma version que en desarrollo local) con autenticacion obligatoria.
# realtime_db y reporting_db se crean automaticamente al primer write.
docker run -d \
  --name mongo \
  --restart unless-stopped \
  -p 27017:27017 \
  -v /opt/orcalab/mongo-data:/data/db \
  -e MONGO_INITDB_ROOT_USERNAME='${mongo_username}' \
  -e MONGO_INITDB_ROOT_PASSWORD='${mongo_password}' \
  mongo:7
