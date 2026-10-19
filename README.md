# Convite de 15 Anos — API (backend)

API REST do sistema de convite e confirmação de presença de uma festa de 15 anos. Recebe as confirmações enviadas
pelas famílias no site público e atende o painel administrativo: indicadores, gerenciamento de famílias e relatório
alfabético de convidados em **PDF** e **CSV**.

> O site (React) fica em um repositório separado — o **frontend**. Esta API precisa estar no ar para ele funcionar.

## Funcionalidades

- Cadastro público de uma família com todos os participantes, em transação única, com validação completa e proteção
  contra envio repetido (`Idempotency-Key`).
- Login administrativo com JWT e senhas em BCrypt; primeiro administrador criado por variáveis de ambiente.
- Listagem paginada de famílias com busca por nome da família ou de participante; consulta, edição e exclusão.
- Estatísticas calculadas no PostgreSQL: total de pessoas, total de famílias e média por família.
- Relatório **LISTA GERAL DE CONVIDADOS** em ordem alfabética (sem diferenciar maiúsculas/minúsculas), numerado, com a
  família de cada convidado e o total — em JSON, PDF e CSV.
- Erros padronizados em português, logs sem dados sensíveis, limite de requisições e documentação Swagger.

## Tecnologias

Java 21, Spring Boot 4.1 (Web MVC, Data JPA/Hibernate 7, Validation, Security + OAuth2 Resource Server/JWT, Actuator),
Flyway, PostgreSQL 17, springdoc-openapi (Swagger), OpenPDF, Maven Wrapper.
Testes: JUnit 5, Mockito, MockMvc e Testcontainers (PostgreSQL real). Infra: Docker, Docker Compose e Nginx (HTTPS).

## Estrutura de pastas

```
.
├── src/main/java/com/convite/evelyn/
│   ├── config/          AppProperties (configurações validadas), OpenAPI
│   ├── controller/      ConviteController (público), FamiliaAdminController, RelatorioController, AuthController
│   ├── dto/             Records de entrada e saída (entidades nunca são expostas)
│   ├── entity/          Familia, Convidado, Administrador
│   ├── exception/       Exceções de negócio e GlobalExceptionHandler
│   ├── repository/      Spring Data JPA
│   ├── security/        SecurityConfig, JWT, rate limit, criação do 1º admin
│   └── service/         Regras de negócio, relatório, exportadores PDF/CSV
├── src/main/resources/db/migration/   Migrations Flyway
├── src/test/java/...    Testes de integração e unitários
├── scripts/             backup.sh e restore.sh do banco
├── compose.yaml         Ambiente local (PostgreSQL + API)
├── docker-compose.prod.yml  Produção na VPS (PostgreSQL + API + Nginx) — veja DEPLOY.md
├── nginx/               Nginx de produção (HTTPS na 3041, proxy da API)
├── Dockerfile
├── .env.example         Modelo das variáveis de produção
├── .env.dev.example     Modelo das variáveis de desenvolvimento
└── mvnw / mvnw.cmd      Maven Wrapper
```

## Pré-requisitos

- **Docker Desktop** em execução (Windows com PowerShell, Linux ou macOS)
- **Java 21** (JDK) — somente para rodar fora do Docker ou executar os testes. O Maven **não** precisa estar
  instalado (usa-se o Maven Wrapper).

## Configuração

Nesta pasta:

```powershell
Copy-Item .env.dev.example .env
```

Edite o `.env` e preencha **pelo menos**:

| Variável | Descrição |
| --- | --- |
| `POSTGRES_PASSWORD` | Senha do banco (longa e aleatória) |
| `JWT_SECRET` | Segredo de assinatura dos tokens do painel, **mínimo 32 caracteres** |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | Primeiro administrador (senha com no mínimo 10 caracteres) |
| `CORS_ALLOWED_ORIGINS` | Endereços do frontend autorizados a chamar a API, separados por vírgula |

Gerar valores aleatórios no PowerShell:

```powershell
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
```

Todas as variáveis estão comentadas no [.env.dev.example](.env.dev.example). A API **não inicia** se `JWT_SECRET` estiver
ausente/curto ou se outra configuração obrigatória for inválida. Se a porta 5432 já estiver em uso na sua máquina,
mude `POSTGRES_PORT` (por exemplo, `5433`).

## Executando localmente

**Opção A — banco e API em containers:**

```powershell
docker compose up -d --build
```

API em http://localhost:8080 — Swagger em http://localhost:8080/swagger-ui.html.

**Opção B — API rodando direto pelo Maven (recarrega mais rápido ao desenvolver):**

```powershell
docker compose up -d db
.\mvnw.cmd spring-boot:run
```

A aplicação lê o `.env` desta pasta automaticamente. Na primeira execução o Flyway cria as tabelas e o administrador
inicial é criado. Linux/macOS: `./mvnw spring-boot:run`.

Parar: `docker compose down` (os dados continuam no volume `convite15_pgdata`; `docker compose down -v` apaga tudo).

## Testes

```powershell
.\mvnw.cmd test
```

43 testes. Os de integração sobem um PostgreSQL temporário com Testcontainers (requer o Docker em execução) e não
tocam no banco local. Cobrem: cadastro válido e com vários convidados; rejeição de nome vazio, quantidade inválida
(zero, negativa, decimal, texto), quantidade incompatível com os nomes, nomes vazios e limite; persistência
consistente e normalização de espaços; idempotência; total de pessoas e média; ordenação alfabética; proteção de todos
os endpoints administrativos; busca e paginação; edição e exclusão; exportação PDF/CSV; erros padronizados (400, 401,
404); limite de requisições.

## Swagger e endpoints

Swagger: http://localhost:8080/swagger-ui.html (JSON em `/v3/api-docs`). Para rotas administrativas, faça
`POST /api/auth/login`, copie o `token` e use o botão **Authorize**. Desative em produção com `SWAGGER_ENABLED=false`.

| Método | Rota | Acesso | Descrição |
| --- | --- | --- | --- |
| `POST` | `/api/public/familias` | público | Confirma presença (aceita `Idempotency-Key`) — 201 criado / 200 repetição |
| `GET` | `/api/public/configuracao` | público | Limite de participantes por família |
| `POST` | `/api/auth/login` | público | Retorna token Bearer |
| `GET` | `/api/auth/me` | admin | Usuário do token |
| `GET` | `/api/admin/familias?busca=&pagina=&tamanho=` | admin | Lista paginada com integrantes |
| `GET` | `/api/admin/familias/{id}` | admin | Consulta uma família |
| `PUT` | `/api/admin/familias/{id}` | admin | Atualiza nome e sincroniza participantes |
| `DELETE` | `/api/admin/familias/{id}` | admin | Exclui família e convidados |
| `GET` | `/api/admin/estatisticas` | admin | Totais e média |
| `GET` | `/api/admin/relatorios/convidados` | admin | Relatório em JSON |
| `GET` | `/api/admin/relatorios/convidados/pdf` | admin | Relatório em PDF |
| `GET` | `/api/admin/relatorios/convidados/csv` | admin | Relatório em CSV (Excel) |
| `GET` | `/actuator/health` | público | Healthcheck |

Erros seguem sempre o formato:

```json
{ "timestamp": "...", "status": 400, "erro": "Bad Request", "mensagem": "Dados inválidos. Verifique os campos informados.",
  "caminho": "/api/public/familias", "campos": [{ "campo": "nome", "mensagem": "Informe o nome da família." }] }
```

## Primeiro administrador

Não há senhas fixas no código. Defina no `.env`:

```
ADMIN_USERNAME=admin
ADMIN_PASSWORD=uma-senha-forte-com-10+-caracteres
```

Na inicialização, se esse usuário ainda não existir, ele é criado com a senha em hash BCrypt (a senha nunca vai para
os logs). Se já existir, nada muda.

**Trocar a senha:** altere `ADMIN_PASSWORD`, defina `ADMIN_RESET_PASSWORD=true`, reinicie a API e depois volte para
`false`. Depois de criado o administrador, você pode remover `ADMIN_PASSWORD` do `.env`.

## Relatório

`GET /api/admin/relatorios/convidados` (JSON, usado na pré-visualização do painel), `/pdf` (A4, pronto para impressão)
e `/csv` (UTF-8 com BOM e separador `;`, abre corretamente no Excel em português). A ordenação usa as regras do
português (`Collator` pt-BR), ignora maiúsculas/minúsculas e é feita no backend, então os três formatos têm exatamente
o mesmo conteúdo e refletem sempre os dados atuais.

## Decisões de projeto

### Modelagem
- `familia (id, nome, chave_idempotencia, data_cadastro, data_atualizacao)`,
  `convidado (id, nome_completo, familia_id → familia ON DELETE CASCADE, data_cadastro)` e
  `administrador (id, usuario único, senha_hash, data_cadastro)`.
- A quantidade de pessoas **não é armazenada**: é sempre `count(convidado)` da família.
- Restrições `CHECK` impedem nomes vazios mesmo fora da aplicação; `ddl-auto=validate` garante que entidades e
  migrations estejam alinhadas (o esquema só muda via Flyway).
- Cadastro e edição em transação única; listagem paginada em 2 consultas, sem N+1.

### Duplicidade de cadastros
Sem login para convidados nem lista prévia, **não é possível saber com certeza** se dois envios são da mesma família.
A API:
1. Grava a `Idempotency-Key` enviada pelo formulário com restrição `UNIQUE`: duplo clique, reenvio após queda de
   conexão ou repetição da mesma requisição devolvem o cadastro original (HTTP 200) **sem duplicar**.
2. Marca com `possivelDuplicata: true` famílias de nome idêntico (ignorando maiúsculas), para revisão no painel.
   **Nada é bloqueado** — famílias diferentes podem ter o mesmo sobrenome.

Limitação: quem preencher o formulário de novo, em outro aparelho ou com dados diferentes, gera um novo cadastro.
A correção é feita pelo painel (editar/excluir).

### Segurança
- Públicos: só o formulário, a configuração pública, o login e o healthcheck. Todo o resto exige token.
- JWT (HS256) com validade configurável no cabeçalho `Authorization` — sem cookies, portanto sem CSRF.
- BCrypt (fator 12) e mensagem de login genérica ("Usuário ou senha inválidos").
- CORS restrito a `CORS_ALLOWED_ORIGINS`.
- Limite por IP em memória: `RATE_LIMIT_CADASTROS` envios e `RATE_LIMIT_LOGINS` logins por minuto (adequado para uma
  única instância).
- Erros nunca expõem stack trace; logs não registram senhas, tokens nem nomes de convidados.
- CSV protegido contra injeção de fórmulas no Excel.

## Deploy na VPS

Passo a passo completo em [DEPLOY.md](DEPLOY.md): `docker-compose.prod.yml` com PostgreSQL, API e Nginx
(HTTPS) com a API em https://brenonun3s.vps-kinghost.net:3041 e o site na Vercel, perfil Spring `prod` e variáveis em `.env.example`.

## Backup e recuperação

**Produção (VPS):**

```bash
chmod +x scripts/*.sh
./scripts/backup.sh                                          # gera backups/convite-AAAAMMDD-HHMMSS.dump
./scripts/restore.sh backups/convite-20270101-030000.dump    # pede confirmação e substitui os dados
```

Agende diariamente com `crontab -e`:

```
0 3 * * * /caminho/do/repositorio/scripts/backup.sh >> /var/log/convite-backup.log 2>&1
```

Backups com mais de `BACKUP_RETENCAO_DIAS` (padrão 14) são removidos. **Copie os arquivos para fora da VPS** (outro
servidor, nuvem ou seu computador) e teste a restauração periodicamente.

**Local (PowerShell):**

```powershell
New-Item -ItemType Directory -Force backups | Out-Null
docker compose exec db sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc -f /tmp/backup.dump'
docker compose cp db:/tmp/backup.dump .\backups\convite.dump
```

## Recomendações de segurança

- Use senhas e segredos longos e aleatórios; nunca versione o `.env`.
- Mantenha `SWAGGER_ENABLED=false` em produção e o banco sem porta publicada.
- Restrinja `CORS_ALLOWED_ORIGINS` ao domínio real do frontend.
- Atualize periodicamente as imagens e o sistema da VPS.
- Faça backups diários fora da VPS e teste a restauração.
- Após o evento, exporte o relatório final e remova os dados pessoais que não forem mais necessários.

## Observação

O projeto foi gerado originalmente com Gradle e convertido para Maven com Maven Wrapper, mantendo o pacote
`com.convite.evelyn`.
