#!/bin/sh
# Cleanup hook de manual-dns-auth-hook.sh. No borra el TXT del registrador
# (no hay API): solo limpia el estado local. Borrar el TXT en el panel queda
# a criterio del usuario (no estorba dejarlo).
set -eu
rm -f /hooks/challenge-status.txt /hooks/proceed.signal
echo "Listo. Podes borrar el TXT _acme-challenge de tu panel DNS si queres (opcional)." >&2
