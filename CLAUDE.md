# MiraQuest API

API de questões para concursos de TI (app gamificado: XP, vidas, sequência de dias). Cliente: app Flutter em `D:\src\miraquest_app`.

## Stack
Spring Boot 4.0.5, Java 17, Spring Data JPA, Spring Security (resource server, JWT HS256), Lombok, springdoc (Swagger em `/swagger-ui.html`), PostgreSQL (padrão) e H2 em memória (perfil `h2`).

## Arquitetura (`src/main/java/com/miraquest/api`)
- `controller/` — REST sob `/api`; só delega ao service.
- `service/QuestaoService` — toda a regra de negócio (lição, verificação, XP/vidas/streak).
- `repository/` — Spring Data; `findCincoAleatoriasPorMateria` usa SQL nativo (`RANDOM()`).
- `model/` — entidades JPA (`Questao`, `UsuarioProgresso`, enum `TipoQuestao`: CLOZE, CERTO_ERRADO, MULTIPLA_ESCOLHA).
- `dto/` — records enviados/recebidos pela API. Nunca expor entidade direto.
- `config/SecurityConfig` — filtro stateless, JWT (`JwtProperties`: `miraquest.jwt.secret`/`JWT_SECRET`, `JWT_EXPIRACAO_HORAS`), BCrypt.
- `resources/data.sql` — seed idempotente (só insere se as tabelas estiverem vazias; funciona em Postgres e H2). Schema via `ddl-auto=update` (Flyway ainda não adotado).

## Endpoints
- `POST /api/auth/registrar` (201) e `POST /api/auth/login` — `{email, senha(8-72)}` → `{token, usuarioId, email}`. Públicos; os demais exigem `Authorization: Bearer <token>`.
- `GET /api/materias` — `[{nome, totalQuestoes}]`, derivado das questões (sem tabela própria): agrupa grafias sem diferenciar caixa, exibe a mais usada, ordena por nome. Importar um CSV com matéria nova a cria automaticamente.
- `GET /api/questoes/licao?materia=` — 5 questões aleatórias.
- `POST /api/questoes/verificar` — `{usuarioId, questaoId, resposta, materia?}`.
- `GET /api/usuario/{id}` — vidas, xp, streak.

- `POST /api/admin/questoes/importar` — multipart (`arquivo`, opcional `simulacao=true`). Só tokens com escopo ADMIN (e-mails em `ADMIN_EMAILS`); exemplo em `docs/exemplo-questoes.csv`.

## Importação CSV (`QuestaoImportacaoService`)
- Colunas: materia, tipo, enunciado, resposta, distratores (separados por `|`), explicacao, banca, pegadinha. Obrigatórias: materia, tipo, enunciado, resposta. Ordem livre; cabeçalho sem acento/maiúsculas; delimitador `,` `;` ou tab detectado; UTF-8 (BOM ok); máx. 2 MB / 5000 linhas.
- Tipo aceita CLOZE/LACUNA, CERTO_ERRADO, MULTIPLA_ESCOLHA. CLOZE exige `___` no enunciado; CLOZE e MÚLTIPLA exigem >= 1 distrator (dedup, sem repetir a resposta); CERTO_ERRADO aceita CERTO/ERRADO/C/E/V/F.
- Linhas inválidas são reportadas (`linha` 1 = cabeçalho) e as válidas entram; duplicadas (mesma matéria sem diferenciar caixa + mesmo enunciado) são ignoradas, então reenviar o arquivo é seguro.
- `pegadinha` é texto livre (nota sobre a armadilha), `banca` texto curto; ambos opcionais e ainda não expostos ao app.

## Regras importantes
- **Autorização:** o usuário vem do `sub` do JWT, nunca do corpo. `usuarioId` divergente do token → 403 (`/verificar` e `/usuario/{id}`). `/error` precisa ficar liberado, senão erros viram 401.
- Cadastro cria o `UsuarioProgresso` (e-mail em minúsculas, único, `senhaHash` BCrypt). Usuário de seed não tem senha e não consegue logar.
- **A resposta correta NUNCA vai para o cliente.** `QuestaoLicaoDto.opcoes` já vem embaralhada (distratores + correta, deduplicada, case-insensitive); vazia para CERTO_ERRADO. A correção é só em `/verificar`.
- Distratores ficam em `Questao.opcoesDistracao` separados por `|`.
- Acerto: +10 XP e atualiza streak; erro: -1 vida e zera streak.
- `Random` do service é injetável (construtor package-private) para testes determinísticos.

## Convenções
- Tabs para indentação, chaves no estilo do código existente; textos e mensagens de erro em português.
- DTOs como `record`; erros via `ResponseStatusException`.
- Testes em `src/test/java/...` espelhando os pacotes; services testados com Mockito puro (sem Spring).

## Comandos
- Banco: `docker compose up -d` (Postgres 16; env `DB_URL`, `DB_USER`, `DB_PASSWORD`, padrão localhost/miraquest/miraquest).
- Rodar: `./mvnw spring-boot:run` (Postgres) ou `./mvnw spring-boot:run -Dspring-boot.run.profiles=h2` (memória, console em `/h2-console`).
- Testes: `./mvnw test` (`ApplicationTests` usa o perfil `h2`). Após mudar arquivos de resources, use `clean` para não ficar lixo em `target/classes`.

## Backlog
URL da API por configuração, paleta "Pôr do sol" (app).
