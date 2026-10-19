#!/usr/bin/env sh
# Restaura um backup gerado por backup.sh. ATENÇÃO: substitui os dados atuais do banco.
# Uso (dentro de backend/): ./scripts/restore.sh backups/convite-20261216-030000.dump
set -eu

if [ "$#" -ne 1 ] || [ ! -f "$1" ]; then
  echo "Uso: $0 <arquivo.dump>" >&2
  exit 1
fi

cd "$(dirname "$0")/.."
ARQUIVO="$1"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.prod.yml}"

printf 'Isto vai SUBSTITUIR todos os dados atuais pelo backup %s. Digite "restaurar" para continuar: ' "$ARQUIVO"
read -r resposta
[ "$resposta" = "restaurar" ] || { echo "Cancelado."; exit 1; }

docker compose -f "$COMPOSE_FILE" stop backend
docker compose -f "$COMPOSE_FILE" cp "$ARQUIVO" postgres:/tmp/restore.dump
docker compose -f "$COMPOSE_FILE" exec -T postgres sh -c \
  'pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists --no-owner /tmp/restore.dump && rm -f /tmp/restore.dump'
docker compose -f "$COMPOSE_FILE" start backend
echo "Restauração concluída."
