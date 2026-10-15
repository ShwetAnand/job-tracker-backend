# Audit report

**Method:** every source/config file was read. **Nothing could be compiled or run**: the sandbox cannot resolve
`repo.maven.apache.org` (`mvn clean verify` fails at the parent POM download; see "Build result"). Therefore **no item is marked
"Implemented and verified"**. A syntax-only `javac` pass found no syntax errors; type/symbol errors could not be checked.

Status legend: 1 verified, 2 implemented - requires testing, 3 partial, 4 missing, 5 broken/insecure.

| Area | Initial | Final | Notes |
|---|---|---|---|
| Registration | 3 (no email normalisation, min password 6, no ownership on user endpoints) | 2 | BCrypt, normalised unique email, 409 on duplicates |
| Login | 5 (`RuntimeException` -> 500 leaking message, user enumeration by timing) | 2 | 401, same message for both failures, dummy hash compare |
| JWT create/validate | 3 (secret hardcoded, no min length, exceptions escape the filter) | 2 | env secret >= 32 bytes enforced at startup, errors -> unauthenticated -> 401 |
| JWT filter / chain | 5 (unauthenticated -> 403; filter also registered as servlet filter) | 2 | JSON 401/403 handlers, registration disabled |
| Endpoint rules | 5 (`/api/auth/**` fully public) | 2 | only login + register public |
| CORS | 3 (hardcoded origin) | 2 | env-driven exact origins |
| Job ownership | 5 (any user could read/update/delete any job; userId from client) | 2 | `findByIdAndUserId`, owner from principal |
| Resume ownership | 5 (upload trusted `userId`; analyze/match unscoped) | 2 | all lookups scoped; list/get/delete added |
| User accounts | 5 (list all users; get/update/delete any user) | 2 | self only; `/me` endpoints |
| Job CRUD | 2-3 | 2 | DTOs, validation, transactions |
| Search / filter / sort / paging | 3 (unscoped, free `sortBy`, no limits) | 2 | Specifications, whitelisted sort, size 1-100, LIKE escaping, N+1 avoided via entity graph |
| PDF upload & parsing | 3 (extension check only, NUL chars would break Postgres) | 2 | signature/type/size/pages checks, encrypted/corrupt/scanned handling |
| Skill extraction (LLM) | 3 (template injection with `{}` in resumes, unvalidated output, likely `LazyInitializationException` on analyze) | 2 | message objects, bounded parser, targeted update |
| Resume-JD matching | 5 (`String.contains`: "c", "java"/"javascript", "rest") | 2 | whole-token catalog, aliases, disclaimer |
| RAG | 4 (plain LLM call, no retrieval) | 2 | pgvector + chunking + filtered retrieval + grounded prompt |
| Exception handling | 3 | 2 | single error shape, 20+ cases, no internals |
| Tests | 4 (`contextLoads` only) | 2 | 18 test classes written, **not executed** |
| Config / secrets | 5 (DB password, JWT secret, API key committed) | 2 | env vars, `.env.example`, `.gitignore` |
| Build | unknown | 3 | could not run; `mvn clean verify` must be run by you |

## Vulnerabilities fixed
1. Broken object-level authorization on jobs, resumes, analysis, job-match and user accounts (IDOR) - server-side ownership everywhere.
2. Client-supplied `userId` trusted on job creation and resume upload.
3. `GET /api/users` leaked all users' names/emails.
4. Secrets committed in `application.properties` (now environment variables; startup fails without them).
5. Weak/absent JWT key validation (min 32 bytes; explicit UTF-8 and HS256).
6. Malformed/expired JWT caused unhandled exceptions; unauthenticated requests returned 403.
7. Login errors echoed internal messages and allowed user enumeration (message + timing).
8. Over-broad public route `/api/auth/**`.
9. Stack/implementation leakage risk in error bodies (generic messages, AI provider text never returned or logged).
10. Prompt-template injection through `{}` in resumes; prompt-injection hardening via delimiters + escaping.
11. Unbounded sort/paging parameters; LIKE wildcard injection in search.
12. PDF upload accepted anything named `.pdf` (now signature, type, size, page limits).
13. Case-variant duplicate accounts (case-insensitive unique email).

## Files
**Created:** `ai/{LlmClient,SpringAiLlmClient,AiErrorTranslator,PromptText}`, `rag/*` (7 classes), `resume/*` (3), `skills/*` (2),
`repository/spec/JobSpecifications`, `security/{RestAuthenticationEntryPoint,RestAccessDeniedHandler}`,
`exception/{AiProviderException,BadRequestException,InvalidFileException,ResumeParsingException}`,
`dto/{InsightRequestDto,InsightResponseDto,JobPageQuery}`, `.env.example`, `docker-compose.yml`, `README.md`, `docs/*`,
18 test classes + `src/test/resources/application-test.properties`.
**Modified:** `pom.xml` (+pgvector store, +H2 test, +surefire argLine), `application.properties`, `.gitignore`, application class,
security (4), config/CorsConfig, entities (3), repositories (3), services (all), controllers (4), DTOs (4), `ResumeMapper`,
exception handler + `ErrorResponse`. **Removed:** no files; removed endpoints/methods: `GET /api/users`, unscoped repository finders,
`UserService.getAllUsers/getUserByEmail`.

## Design decisions worth knowing
- 404 (not 403) for other users' resources: does not reveal existence.
- Java version left at the POM's 25 (your recent upgrade) although the brief says 21; builds with `-Djava.version=21` too.
- RAG uses `spring-ai-pgvector-store` with a `@Lazy` hand-built `PgVectorStore` instead of the autoconfiguring starter so a missing pgvector
  extension cannot stop the app from booting. Spring AI version is whatever the existing BOM (1.1.x) declares; compatibility with Boot 3.5.7 was
  **not** re-verified here.
- Own character-based chunker instead of `TokenTextSplitter`: deterministic, testable, no tokenizer API surface.
- H2 for integration tests instead of Testcontainers (no Docker requirement; unverified either way).
- `ddl-auto=update` retained (no Flyway) to avoid introducing a new technology.

## Build result
`mvn clean verify -Djava.version=21` (Maven 3.8.7, JDK 21.0.10): **FAILED before compiling** - `Non-resolvable parent POM ...
Unknown host repo.maven.apache.org`. Environmental (no network route to Maven Central), not caused by the project. Tests executed: **0**.

## Remaining / not done
- Run the build and tests; fix whatever the compiler/tests report (most likely places: Spring AI 1.1 API names in `rag/*`
  and `ai/SpringAiLlmClient`, `PgVectorStore.builder` enum names, and test assertions).
- Real PostgreSQL/pgvector and OpenAI behaviour untested (embedding dimension, `CREATE EXTENSION` privileges, retry/timeout property names).
- No rate limiting/cost caps on AI endpoints, no refresh tokens/logout, no OCR, no email verification, no Flyway migrations.
- Original secrets remain in git history: rotate them and consider history rewrite. Tool logs in `.github/modernize/` contain the old DB password.
