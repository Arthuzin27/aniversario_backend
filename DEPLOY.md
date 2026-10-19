# Deploy na VPS (KingHost) — Convite

Site: **https://brenonun3s.vps-kinghost.net:3041**

## Visão geral

```
Internet ──HTTPS :3041──▶ convite-nginx (443)  ── estáticos do React
                               │  /api/*, /actuator/health
                               ▼
                         convite-backend (8080) ──▶ convite-postgres (5432)
                         127.0.0.1:1556              127.0.0.1:2334
```

| Container          | Porta no host            | Exposição            |
|--------------------|--------------------------|----------------------|
| `convite-nginx`    | `3041 → 443`             | pública, só HTTPS    |
| `convite-backend`  | `127.0.0.1:1556 → 8080`  | só dentro da VPS     |
| `convite-postgres` | `127.0.0.1:2334 → 5432`  | só dentro da VPS     |

Tudo é exclusivo deste projeto: projeto Compose `convite`, rede `convite-net`, volume `convite_postgres_data`.
O certificado é o Let's Encrypt que já existe na VPS (pasta do estoque360), montado **somente leitura**.

O sistema é formado por **dois repositórios**, que precisam ficar lado a lado:

```
~/convite/backend    ← aniversario_backend (docker-compose.prod.yml, .env, este DEPLOY.md)
~/convite/frontend   ← aniversario_frontend (Dockerfile do Nginx + build do React)
```

Todos os comandos `docker compose` abaixo são executados **dentro de `~/convite/backend`**.

---

## 1. Clonar (primeira vez) ou atualizar os repositórios

```bash
mkdir -p ~/convite && cd ~/convite
git clone https://github.com/Arthuzin27/aniversario_backend.git backend
git clone https://github.com/Arthuzin27/aniversario_frontend.git frontend
```

> Se os repositórios forem privados, o `git clone` pede usuário e um *Personal Access Token* do GitHub
> (no lugar da senha) — ou configure uma chave SSH de deploy e use as URLs `git@github.com:...`.

Atualizar depois (veja também o passo 6):

```bash
cd ~/convite/backend && git pull
cd ~/convite/frontend && git pull
```

## 2. Criar o `.env`

```bash
cd ~/convite/backend
cp .env.example .env
chmod 600 .env
```

Gere os valores aleatórios (copie cada saída para o `.env`):

```bash
# DB_PASSWORD (sem / + = para evitar qualquer problema de escape)
openssl rand -base64 32 | tr -d '/+='
# JWT_SECRET (mínimo 32 caracteres)
openssl rand -base64 48
# ADMIN_PASSWORD (mínimo 10 caracteres)
openssl rand -base64 18
```

Edite com `nano .env` e preencha **todos** os campos. Mínimo obrigatório:

```
DB_NAME=convite
DB_USERNAME=convite
DB_PASSWORD=<gerado>
JWT_SECRET=<gerado>
ADMIN_USERNAME=<seu usuário do painel>
ADMIN_PASSWORD=<gerado ou escolhido, 10+ caracteres>
CORS_ALLOWED_ORIGINS=https://brenonun3s.vps-kinghost.net:3041
```

e os dados da festa (`VITE_FESTA_*`), que aparecem no convite.

> **Guarde a `ADMIN_PASSWORD`** num gerenciador de senhas. O administrador é criado no primeiro start
> (só se ainda não existir). Depois você pode apagar o valor de `ADMIN_PASSWORD` do `.env`.
> Para trocar a senha depois: coloque a nova em `ADMIN_PASSWORD`, `ADMIN_RESET_PASSWORD=true`,
> `sudo docker compose -f docker-compose.prod.yml up -d backend`, e volte para `false`.

## 3. Conferir o certificado

```bash
sudo openssl x509 -enddate -noout -in /home/admin/backend-estoque360/nginx/certbot/conf/live/brenonun3s.vps-kinghost.net/fullchain.pem
```

Deve mostrar uma data `notAfter=` no futuro. Se o arquivo não existir, o `convite-nginx` não sobe.

Confira também que a porta **3041/TCP** está liberada no firewall do painel da KingHost (e no `ufw`, se usado).

## 4. Subir

```bash
cd ~/convite/backend
sudo docker compose -f docker-compose.prod.yml up -d --build
```

A primeira vez demora alguns minutos (download das dependências Maven e npm). Na ordem: o Postgres fica
*healthy*, o backend aplica as migrations do Flyway e fica *healthy*, e só então o Nginx sobe.

## 5. Verificar

```bash
sudo docker compose -f docker-compose.prod.yml ps
sudo docker compose -f docker-compose.prod.yml logs --tail=100 backend
curl -I https://brenonun3s.vps-kinghost.net:3041
curl https://brenonun3s.vps-kinghost.net:3041/api/public/configuracao
```

Esperado:

- `ps`: os três `Up`, com `(healthy)` no postgres e no backend.
- Logs do backend: `The following 1 profile is active: "prod"`, `Successfully applied ... migration`
  (só na primeira vez), `Administrador inicial '...' criado.` (só na primeira vez) e `Started EvelynApplication`.
- `curl -I`: `HTTP/2 200` com `strict-transport-security: max-age=300`.
- `/api/public/configuracao`: `{"maxParticipantes":30}`.
- `curl -I http://brenonun3s.vps-kinghost.net:3041` → `301` para `https://...`.
- `curl -I https://brenonun3s.vps-kinghost.net:3041/actuator/env` → `404` (bloqueado).

Depois abra o site no navegador e entre em `/admin` com o usuário/senha do `.env`.

## 6. Atualizar depois de um `git pull`

```bash
cd ~/convite/backend && git pull
cd ~/convite/frontend && git pull
cd ~/convite/backend
sudo docker compose -f docker-compose.prod.yml up -d --build
sudo docker image prune -f      # remove imagens antigas sem uso (opcional)
```

Os dados do banco ficam no volume `convite_postgres_data` e **não** são apagados por `up --build`, `down`
ou `restart`. Novas migrations do Flyway rodam sozinhas no start do backend.

> Alterou só os dados da festa (`VITE_FESTA_*`) no `.env`? Eles são embutidos no build do frontend:
> `sudo docker compose -f docker-compose.prod.yml up -d --build nginx`.

> **Nunca** rode `down -v` em produção: o `-v` apaga o volume do banco.

## 7. Recarregar o Nginx após renovação do certificado

O certificado é renovado fora deste projeto (pelo certbot do estoque360). Os arquivos novos aparecem
automaticamente dentro do container (a pasta é montada), mas o Nginx só os lê ao recarregar:

```bash
sudo docker exec convite-nginx nginx -t && sudo docker exec convite-nginx nginx -s reload
```

Opcional — recarregar automaticamente toda segunda às 4h (`sudo crontab -e`):

```
0 4 * * 1 docker exec convite-nginx nginx -s reload >/dev/null 2>&1
```

## 8. Acessar o banco pelo túnel SSH (PuTTY + DBeaver/pgAdmin)

O Postgres só escuta em `127.0.0.1:2334` da VPS. Para acessá-lo do seu computador:

1. PuTTY → carregue a sessão da VPS → **Connection › SSH › Tunnels**:
   - *Source port*: `15432` (qualquer porta livre no seu PC)
   - *Destination*: `127.0.0.1:2334`
   - Tipo **Local** → **Add**. Volte em *Session* e **Save** para guardar.
2. Conecte normalmente no PuTTY e deixe a janela aberta.
3. No DBeaver/pgAdmin: host `localhost`, porta `15432`, banco `DB_NAME`, usuário `DB_USERNAME`,
   senha `DB_PASSWORD` (do `.env` da VPS). Sem SSL.

## 9. Backup e restauração

Backup (formato custom do `pg_dump`):

```bash
mkdir -p ~/convite/backups
sudo docker exec convite-postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' \
  > ~/convite/backups/convite-$(date +%Y%m%d-%H%M%S).dump
ls -lh ~/convite/backups
```

Restauração (**substitui** os dados atuais):

```bash
cd ~/convite/backend
sudo docker compose -f docker-compose.prod.yml stop backend
sudo docker exec -i convite-postgres sh -c \
  'pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists --no-owner' \
  < ~/convite/backups/convite-AAAAMMDD-HHMMSS.dump
sudo docker compose -f docker-compose.prod.yml start backend
```

Também há scripts prontos (com retenção de 14 dias e confirmação na restauração):
`sudo sh scripts/backup.sh` e `sudo sh scripts/restore.sh backups/<arquivo>.dump`.
Agendamento diário às 3h (`sudo crontab -e`):

```
0 3 * * * cd /home/<seu-usuario>/convite/backend && sh scripts/backup.sh >> /var/log/convite-backup.log 2>&1
```

**Copie os backups para fora da VPS** (ex.: WinSCP/`pscp` para o seu PC).

## 10. Logs e reinício de cada serviço

```bash
cd ~/convite/backend
# logs (Ctrl+C para sair do -f)
sudo docker compose -f docker-compose.prod.yml logs -f --tail=200 backend
sudo docker compose -f docker-compose.prod.yml logs -f --tail=200 nginx
sudo docker compose -f docker-compose.prod.yml logs -f --tail=200 postgres

# reiniciar um serviço
sudo docker compose -f docker-compose.prod.yml restart backend
sudo docker compose -f docker-compose.prod.yml restart nginx
sudo docker compose -f docker-compose.prod.yml restart postgres

# parar / subir tudo (os dados continuam no volume)
sudo docker compose -f docker-compose.prod.yml down
sudo docker compose -f docker-compose.prod.yml up -d

# uso de memória/CPU
sudo docker stats convite-backend convite-nginx convite-postgres
```

Os logs de cada container são rotacionados (3 arquivos de 10 MB) para não encher o disco.

---

## Observações de segurança

- **HSTS** começa com `max-age=300` (5 min). HSTS vale para o **host inteiro**, em todas as portas: depois que o
  navegador visitar o convite, ele vai forçar `https://` também em outros sistemas servidos em
  `brenonun3s.vps-kinghost.net` por 5 minutos. Se algum sistema da VPS ainda usa `http://` nesse host, mantenha o
  valor baixo (ou remova o header em `frontend/nginx/security-headers.conf`).
- Rate limit por IP real do cliente: `RATE_LIMIT_CADASTROS` envios do formulário e `RATE_LIMIT_LOGINS` logins por
  minuto. O Nginx sobrescreve o `X-Forwarded-For` com o IP de quem conectou, então ele não pode ser forjado.
- `/actuator` bloqueado (exceto `/actuator/health`); Swagger desligado (`SWAGGER_ENABLED=false`).

## Teste local do compose de produção (opcional, no seu PC)

Na pasta `backend/` (Git Bash), crie um certificado autoassinado e um `.env` de teste em `.local-test/` (ignorada
pelo git):

```bash
D=.local-test/certbot/conf/live/brenonun3s.vps-kinghost.net && mkdir -p $D
MSYS_NO_PATHCONV=1 openssl req -x509 -nodes -newkey rsa:2048 -days 7 -keyout $D/privkey.pem -out $D/fullchain.pem \
  -subj "/CN=localhost" -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"
cp .env.example .local-test/.env   # preencha, com CORS_ALLOWED_ORIGINS=https://localhost:3041
docker compose --env-file .local-test/.env -f docker-compose.prod.yml -f docker-compose.local-test.yml up -d --build
# ... teste em https://localhost:3041 ...
docker compose --env-file .local-test/.env -f docker-compose.prod.yml -f docker-compose.local-test.yml down -v
```

(O `down -v` aqui apaga só o volume de teste `convite_postgres_data`; o banco de desenvolvimento usa
`convite15_pgdata`, de outro projeto Compose.)
