#!/bin/bash
# Emite un certificado real de Let's Encrypt para el subdominio DuckDNS del ALB
# via reto DNS-01 (no requiere que el dominio ya apunte a nada publico) y lo
# importa a ACM. Reemplaza al certificado autofirmado de generate-alb-cert.sh
# sin necesitar CloudFront/Route53 (bloqueados en este Learner Lab, ver README).
#
# Requiere Docker corriendo (usa certbot/certbot en contenedor, no instala
# nada en el host ni en la instancia EC2 de produccion).
#
# Uso:
#   DUCKDNS_DOMAIN=orcalab DUCKDNS_TOKEN=xxxxxxxx-xxxx-... ./issue-letsencrypt-cert.sh
#
# El ARN resultante se pega en terraform.tfvars como alb_certificate_arn
# (igual que con el autofirmado). Renovar corriendo este mismo script de nuevo
# antes de que venza (Let's Encrypt: ~90 dias) e importando el nuevo cert -
# ACM no permite "renovar in place" un certificado importado, hay que
# reimportar y volver a apuntar el listener al nuevo ARN.
set -euo pipefail

: "${DUCKDNS_DOMAIN:?Falta DUCKDNS_DOMAIN (el subnombre, sin .duckdns.org)}"
: "${DUCKDNS_TOKEN:?Falta DUCKDNS_TOKEN (token de tu cuenta DuckDNS)}"

FQDN="${DUCKDNS_DOMAIN}.duckdns.org"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORKDIR=$(mktemp -d)
trap 'rm -rf "$WORKDIR"' EXIT

echo "Emitiendo certificado Let's Encrypt para ${FQDN} (reto DNS-01 via DuckDNS)..." >&2

# Docker en Windows/Git Bash necesita rutas de host estilo Windows (C:/...) en
# el lado izquierdo de -v; MSYS_NO_PATHCONV evita que ademas reinterprete las
# rutas del CONTENEDOR (/hooks/..., /etc/letsencrypt) como si fueran relativas
# a la raiz de MSYS (lo que rompia --manual-auth-hook antes de este fix).
mkdir -p "${WORKDIR}/letsencrypt"
WORKDIR_WIN=$(cd "${WORKDIR}" && pwd -W)
SCRIPT_DIR_WIN=$(cd "${SCRIPT_DIR}" && pwd -W)

# Los archivos en live/ son symlinks (creados por el proceso Linux del
# contenedor) — sobre un bind-mount de Windows, un binario nativo como aws-cli
# no los resuelve. Por eso el propio contenedor hace `cp -L` (sigue symlinks)
# a export/ antes de salir, dejando ahi contenido real, no symlinks.
MSYS_NO_PATHCONV=1 docker run --rm \
  --entrypoint sh \
  -e DUCKDNS_DOMAIN="${DUCKDNS_DOMAIN}" \
  -e DUCKDNS_TOKEN="${DUCKDNS_TOKEN}" \
  -e FQDN="${FQDN}" \
  -v "${WORKDIR_WIN}/letsencrypt:/etc/letsencrypt" \
  -v "${SCRIPT_DIR_WIN}/hooks:/hooks:ro" \
  certbot/certbot -c '
    set -e
    certbot certonly \
      --manual --preferred-challenges dns \
      --manual-auth-hook /hooks/duckdns-auth-hook.sh \
      --manual-cleanup-hook /hooks/duckdns-cleanup-hook.sh \
      --non-interactive --agree-tos --register-unsafely-without-email \
      -d "$FQDN"
    mkdir -p /etc/letsencrypt/export
    cp -L "/etc/letsencrypt/live/$FQDN/cert.pem" \
          "/etc/letsencrypt/live/$FQDN/privkey.pem" \
          "/etc/letsencrypt/live/$FQDN/chain.pem" \
          /etc/letsencrypt/export/
  '

EXPORT_DIR="${WORKDIR}/letsencrypt/export"
echo "Diagnostico post-docker: contenido de ${EXPORT_DIR}" >&2
ls -la "${EXPORT_DIR}" >&2 2>&1 || echo "(ls fallo, el directorio puede no existir todavia)" >&2

# El bind-mount de Docker Desktop en Windows a veces tarda un instante en
# hacer visibles al host archivos recien creados dentro del contenedor.
# Reintenta leer con bash (no con aws-cli) antes de rendirse.
CERT_DIR="${WORKDIR}"
for f in cert.pem privkey.pem chain.pem; do
  ok=0
  for _ in 1 2 3 4 5; do
    if cat "${EXPORT_DIR}/${f}" > "${WORKDIR}/${f}" 2>/dev/null; then
      ok=1
      break
    fi
    sleep 1
  done
  if [ "$ok" -ne 1 ]; then
    echo "No se pudo leer ${EXPORT_DIR}/${f} desde el host tras varios intentos." >&2
    exit 1
  fi
done

echo "Importando a ACM..." >&2
# Igual que generate-alb-cert.sh: rutas relativas desde dentro de WORKDIR.
# fileb:// con ruta absoluta /tmp/... se rompe porque aws-cli (binario nativo
# de Windows) necesita la conversion automatica de MSYS a ruta C:/..., y esa
# conversion no aplica a rutas relativas de la misma forma (no hace falta).
CERT_ARN=$(cd "${CERT_DIR}" && aws acm import-certificate \
  --region us-east-1 \
  --certificate fileb://cert.pem \
  --private-key fileb://privkey.pem \
  --certificate-chain fileb://chain.pem \
  --query CertificateArn --output text)

echo ""
echo "Certificado importado: $CERT_ARN"
echo "Actualiza terraform.tfvars:"
echo "alb_certificate_arn = \"$CERT_ARN\""
