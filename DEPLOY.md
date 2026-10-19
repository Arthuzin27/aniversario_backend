# Deploy — Convite (frontend na Vercel + API na VPS KingHost)

- **Site (React):** na **Vercel** (ex.: `https://convite-evelyn.vercel.app`)
- **API:** na VPS, em **https://brenonun3s.vps-kinghost.net:3041**

## Visão geral

```
Navegador ──▶ Vercel (site React)
    │
    └──HTTPS :3041──▶ convite-nginx (443) ── /api/*, /actuator/health
                            │
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
O Nginx fica em [`nginx/`](nginx/) (só proxy da API; qualquer outro caminho responde 404).

Na VPS só é preciso **este repositório (backend)**. Todos os comandos `docker compose` abaixo são executados
**dentro de `~/convite`**.

---

## 1. Clonar (primeira vez) o repositório do backend

```bash
git clone https://github.com/Arthuzin27/aniversario_backend.git ~/convite
cd ~/convite
```

> Troque a URL se usar outro repositório. Se ele for privado, o `git clone` pede usuário e um
> *Personal Access Token* do GitHub (no lugar da senha) — ou use uma chave SSH de deploy (`git@github.com:...`).

## 2. Publicar o frontend na Vercel

1. Na Vercel: **Add New → Project** e importe o repositório do **frontend** (framework **Vite**, build
   `npm run build`, saída `dist` — detectados automaticamente).
2. Em *Settings → Environment Variables* (ambiente **Production**):

   | Variável | Valor |
   |---|---|
   | `VITE_API_URL` | `https://brenonun3s.vps-kinghost.net:3041` (sem barra no final) |
   | `VITE_FESTA_ANIVERSARIANTE`, `VITE_FESTA_DATA`, `VITE_FESTA_HORARIO`, `VITE_FESTA_LOCAL`, `VITE_FESTA_ENDERECO`, `VITE_FESTA_MAPA_URL`, `VITE_FESTA_TRAJE`, `VITE_FESTA_PRAZO_CONFIRMACAO`, `VITE_FESTA_MENSAGEM` | dados da festa (veja `.env.example` do frontend) |

3. Faça o deploy e anote o endereço final do site (ex.: `https://convite-evelyn.vercel.app`, ou o seu domínio
   próprio, se configurar um em *Settings → Domains*). Ele vai no `CORS_ALLOWED_ORIGINS` do passo 3.

> As variáveis `VITE_*` entram no build: depois de alterar alguma na Vercel, faça **Redeploy**.
> O `vercel.json` do frontend já trata as rotas `/admin/...` (F5 funciona).

## 3. Criar o `.env`

```bash
cd ~/convite
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
CORS_ALLOWED_ORIGINS=https://convite-evelyn.vercel.app
```

`CORS_ALLOWED_ORIGINS` deve ser **exatamente** o endereço do site na Vercel (com `https://`, sem barra no final).
Com domínio próprio também, separe por vírgula: `https://convite-evelyn.vercel.app,https://convite.seudominio.com.br`.

> **Guarde a `ADMIN_PASSWORD`** num gerenciador de senhas. O administrador é criado no primeiro start
> (só se ainda não existir). Depois você pode apagar o valor de `ADMIN_PASSWORD` do `.env`.
> Para trocar a senha depois: coloque a nova em `ADMIN_PASSWORD`, `ADMIN_RESET_PASSWORD=true`,
> `sudo docker compose -f docker-compose.prod.yml up -d backend`, e volte para `false`.

## 4. Conferir o certificado e a porta

```bash
sudo openssl x509 -enddate -noout -in /home/admin/backend-estoque360/nginx/certbot/conf/live/brenonun3s.vps-kinghost.net/fullchain.pem
sudo ss -tlnp | grep -E ':(2334|1556|3041)\b'
```

- O certificado deve mostrar uma data `notAfter=` no futuro (se o arquivo não existir, o `convite-nginx` não sobe).
- O `ss` não deve mostrar nada (portas livres).
- A porta **3041/TCP** precisa estar liberada no firewall do painel da KingHost (e no `ufw`, se usado).
  As portas 2334 e 1556 **não** devem ser liberadas.

## 5. Subir

```bash
cd ~/convite
sudo docker compose -f docker-compose.prod.yml up -d --build
```

A primeira vez demora alguns minutos (download das dependências Maven). Na ordem: o Postgres fica *healthy*,
o backend aplica as migrations do Flyway e fica *healthy*, e só então o Nginx sobe.

## 6. Verificar

```bash
sudo docker compose -f docker-compose.prod.yml ps
sudo docker compose -f docker-compose.prod.yml logs --tail=100 backend
curl https://brenonun3s.vps-kinghost.net:3041/api/public/configuracao
curl -I https://brenonun3s.vps-kinghost.net:3041/actuator/env
```

Esperado:

- `ps`: os três `Up`, com `(healthy)` no postgres e no backend.
- Logs do backend: `The following 1 profile is active: "prod"`, `Successfully applied ... migration`
  (só na primeira vez), `Administrador inicial '...' criado.` (só na primeira vez) e `Started EvelynApplication`.
- `/api/public/configuracao`: `{"maxParticipantes":30}`.
- `/actuator/env`: `404` (bloqueado). A raiz `https://...:3041/` também responde `404` — é normal, o site está na Vercel.

Teste o CORS com o endereço da Vercel (troque pelo seu):

```bash
curl -s -o /dev/null -w '%{http_code}\n' -X OPTIONS \
  -H 'Origin: https://convite-evelyn.vercel.app' -H 'Access-Control-Request-Method: POST' \
  https://brenonun3s.vps-kinghost.net:3041/api/public/familias
```

`200` = liberado; `403` = o endereço não confere com `CORS_ALLOWED_ORIGINS` (corrija o `.env` e rode
`sudo docker compose -f docker-compose.prod.yml up -d backend`).

Por fim, abra o site da Vercel, envie uma confirmação de teste e entre em `/admin` com o usuário/senha do `.env`
(depois exclua a família de teste pelo painel).

## 7. Atualizar depois de um `git pull`

```bash
cd ~/convite && git pull
sudo docker compose -f docker-compose.prod.yml up -d --build
sudo docker image prune -f      # remove imagens antigas sem uso (opcional)
```

Os dados do banco ficam no volume `convite_postgres_data` e **não** são apagados por `up --build`, `down`
ou `restart`. Novas migrations do Flyway rodam sozinhas no start do backend.

O frontend é atualizado pela própria Vercel a cada `git push` no repositório do frontend.

> **Nunca** rode `down -v` em produção: o `-v` apaga o volume do banco.

## 8. Recarregar o Nginx após renovação do certificado

O certificado é renovado fora deste projeto (pelo certbot do estoque360). Os arquivos novos aparecem
automaticamente dentro do container (a pasta é montada), mas o Nginx só os lê ao recarregar:

```bash
sudo docker exec convite-nginx nginx -t && sudo docker exec convite-nginx nginx -s reload
```

Opcional — recarregar automaticamente toda segunda às 4h (`sudo crontab -e`):

```
0 4 * * 1 docker exec convite-nginx nginx -s reload >/dev/null 2>&1
```

## 9. Acessar o banco pelo túnel SSH (PuTTY + DBeaver/pgAdmin)

O Postgres só escuta em `127.0.0.1:2334` da VPS. Para acessá-lo do seu computador:

1. PuTTY → carregue a sessão da VPS → **Connection › SSH › Tunnels**:
   - *Source port*: `15432` (qualquer porta livre no seu PC)
   - *Destination*: `127.0.0.1:2334`
   - Tipo **Local** → **Add**. Volte em *Session* e **Save** para guardar.
2. Conecte normalmente no PuTTY e deixe a janela aberta.
3. No DBeaver/pgAdmin: host `localhost`, porta `15432`, banco `DB_NAME`, usuário `DB_USERNAME`,
   senha `DB_PASSWORD` (do `.env` da VPS). Sem SSL.

## 10. Backup e restauração

Backup (formato custom do `pg_dump`):

```bash
mkdir -p ~/convite/backups
sudo docker exec convite-postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' \
  > ~/convite/backups/convite-$(date +%Y%m%d-%H%M%S).dump
ls -lh ~/convite/backups
```

Restauração (**substitui** os dados atuais):

```bash
cd ~/convite
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
0 3 * * * cd /home/<seu-usuario>/convite && sh scripts/backup.sh >> /var/log/convite-backup.log 2>&1
```

**Copie os backups para fora da VPS** (ex.: WinSCP/`pscp` para o seu PC).

## 11. Logs e reinício de cada serviço

```bash
cd ~/convite
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
  navegador chamar a API, ele vai forçar `https://` também em outros sistemas servidos em
  `brenonun3s.vps-kinghost.net` por 5 minutos. Se algum sistema da VPS ainda usa `http://` nesse host, mantenha o
  valor baixo (ou remova o header em `nginx/security-headers.conf`).
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
cp .env.example .local-test/.env   # preencha (CORS_ALLOWED_ORIGINS pode ser um endereço fictício)
docker compose --env-file .local-test/.env -f docker-compose.prod.yml -f docker-compose.local-test.yml up -d --build
curl -k https://localhost:3041/api/public/configuracao
docker compose --env-file .local-test/.env -f docker-compose.prod.yml -f docker-compose.local-test.yml down -v
```

(O `down -v` aqui apaga só o volume de teste `convite_postgres_data`; o banco de desenvolvimento usa
`convite15_pgdata`, de outro projeto Compose.)
