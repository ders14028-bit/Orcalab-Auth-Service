#!/bin/sh
# Auth hook para `certbot certonly --manual --preferred-challenges dns`.
# Certbot lo corre dentro del contenedor y expone $CERTBOT_VALIDATION con el
# valor que hay que publicar en el TXT de _acme-challenge.<dominio>.
# Requiere DUCKDNS_DOMAIN y DUCKDNS_TOKEN como variables de entorno (-e en docker run).
set -eu

wget -qO- "https://www.duckdns.org/update?domains=${DUCKDNS_DOMAIN}&token=${DUCKDNS_TOKEN}&txt=${CERTBOT_VALIDATION}&verbose=true"
echo ""
echo "TXT publicado, esperando propagacion..." >&2
sleep 30
