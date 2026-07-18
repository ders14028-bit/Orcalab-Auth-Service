#!/bin/sh
# Cleanup hook para `certbot certonly --manual --preferred-challenges dns`.
# Limpia el TXT de _acme-challenge tras la validacion (o tras un intento fallido).
#
# IMPORTANTE: `clear=true` SIN el parametro `txt` limpia el registro A/AAAA
# (ip/ipv6), no el TXT -- le paso justamente al parametro "ip" de la API de
# DuckDNS. Hay que incluir `txt=` (el valor no importa con clear=true) para
# que el clear se aplique al TXT en vez de al A record. (Bug real detectado
# el 2026-07-17: esto borro el registro A de orcalab.duckdns.org en produccion.)
set -eu

wget -qO- "https://www.duckdns.org/update?domains=${DUCKDNS_DOMAIN}&token=${DUCKDNS_TOKEN}&txt=x&clear=true"
echo ""
