#!/usr/bin/env sh
# Gera um backup do PostgreSQL (formato custom do pg_dump) e remove backups antigos.
# Uso na VPS (dentro de backend/):   ./scripts/backup.sh
# Agendamento:  crontab -e  →  0 3 * * * /caminho/do/projeto/backend/scripts/backup.sh >> /var/log/convite-backup.log 2>&1
set -eu

cd "$(dirname "$0")/.."
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.prod.yml}"
DESTINO="${BACKUP_DIR:-./backups}"
RETENCAO_DIAS="${BACKUP_RETENCAO_DIAS:-14}"
NOME="convite-$(date +%Y%m%d-%H%M%S).dump"

mkdir -p "$DESTINO"
docker compose -f "$COMPOSE_FILE" exec -T postgres sh -c \
  'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc -f /tmp/backup.dump'
docker compose -f "$COMPOSE_FILE" cp postgres:/tmp/backup.dump "$DESTINO/$NOME"
docker compose -f "$COMPOSE_FILE" exec -T postgres rm -f /tmp/backup.dump

find "$DESTINO" -name 'convite-*.dump' -type f -mtime +"$RETENCAO_DIAS" -delete
echo "Backup salvo em $DESTINO/$NOME"
