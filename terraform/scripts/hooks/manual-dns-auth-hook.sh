#!/bin/sh
# Auth hook para `certbot certonly --manual --preferred-challenges dns` cuando
# el registrador (ej. Namecheap) no tiene una API tan simple como la de
# DuckDNS (ver duckdns-auth-hook.sh) para publicar el TXT solo.
#
# En vez de automatizar la publicacion del TXT, este hook:
#   1. Escribe el TXT que hay que publicar en /hooks/challenge-status.txt
#      (bind-mount visible desde el host).
#   2. Bloquea hasta que aparezca /hooks/proceed.signal - el host lo crea
#      despues de verificar (con nslookup contra un resolver publico) que el
#      TXT ya esta publicado, tras que el humano lo cargue en el panel DNS.
#   3. Timeout de 20 minutos si nadie confirma.
set -eu

STATUS_FILE="/hooks/challenge-status.txt"
SIGNAL_FILE="/hooks/proceed.signal"
rm -f "$SIGNAL_FILE"

cat > "$STATUS_FILE" <<EOF
DOMINIO: _acme-challenge.${CERTBOT_DOMAIN}
TIPO: TXT
VALOR: ${CERTBOT_VALIDATION}
EOF

echo "=== Agrega este TXT record en el panel DNS ===" >&2
echo "Host:  _acme-challenge" >&2
echo "Type:  TXT Record" >&2
echo "Value: ${CERTBOT_VALIDATION}" >&2
echo "===============================================" >&2
echo "Esperando confirmacion (proceed.signal), timeout 20 min..." >&2

i=0
while [ ! -f "$SIGNAL_FILE" ]; do
  i=$((i + 1))
  if [ "$i" -ge 240 ]; then
    echo "Timeout esperando confirmacion del TXT record." >&2
    exit 1
  fi
  sleep 5
done

echo "Confirmado, continuando con la validacion de Let's Encrypt." >&2
