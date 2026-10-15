# AI-Powered Job Tracker & Resume Intelligence Platform - Backend

Spring Boot 3.5 / Spring Security (JWT) / PostgreSQL / PDFBox / Spring AI (OpenAI) with a pgvector-based RAG pipeline.

> **Verification status:** the code and tests in this repository were written and reviewed statically in an
> environment that could not download Maven dependencies, so **`mvn clean verify` has not been executed yet**.
> Run it first (see below) and fix anything it reports. Details: `docs/AUDIT.md`.

## Requirements
- JDK (the POM targets `java.version=25`; use 21 with `-Djava.version=21` if you only have 21)
- PostgreSQL 14+; for RAG the **pgvector** extension (`docker compose up -d` gives you one)
- Optional: an OpenAI API key (chat + embeddings)

## Setup
1. Create secrets (never commit them):
   ```bash
   cp .env.example .env          # then edit: DB_PASSWORD, JWT_SECRET (>= 32 bytes), OPENAI_API_KEY
   openssl rand -base64 48       # good JWT_SECRET
   ```
2. Database: either `DB_PASSWORD=... docker compose up -d` (PostgreSQL + pgvector on port 1574, db `job_tracker`),
   or use your own PostgreSQL and set `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`.
3. Run:
   ```bash
   set -a; source .env; set +a
   ./mvnw spring-boot:run        # or: mvn spring-boot:run
   ```
   The app refuses to start without `DB_PASSWORD` and a `JWT_SECRET` of at least 32 bytes.
   Swagger UI: `http://localhost:8080/swagger-ui.html` (set `SPRINGDOC_ENABLED=false` in production).

## Schema
Hibernate `ddl-auto=update` (default, `JPA_DDL_AUTO`) creates/updates tables. This change adds `resumes.indexed_at`
and indexes on `jobs.user_id` / `resumes.user_id`; no data migration is needed. Use a migration tool (Flyway/Liquibase)
and `validate` before a real production deployment.
The RAG table `vector_store` is created by Spring AI on first use (`RAG_INITIALIZE_SCHEMA=true`). If your DB user may not run
`CREATE EXTENSION`, create it manually as a superuser and set `RAG_INITIALIZE_SCHEMA=false`
(statements per the Spring AI pgvector docs):
```sql
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE TABLE IF NOT EXISTS vector_store (
  id uuid DEFAULT uuid_generate_v4() PRIMARY KEY, content text, metadata json, embedding vector(1536));
CREATE INDEX IF NOT EXISTS spring_ai_vector_index ON vector_store USING HNSW (embedding vector_cosine_ops);
```

## Tests
```bash
mvn clean verify -Djava.version=21     # drop the flag if you run JDK 25
```
- Unit + MockMvc integration tests use in-memory **H2** (profile `test`), mock the LLM and vector store, and never touch your
  real database or OpenAI. (H2 was chosen over Testcontainers so tests need no Docker; JPA code uses portable Specifications.)
- Optional live check against OpenAI (skipped without a key): `OPENAI_API_KEY=sk-... mvn test -Dtest=LiveAiSmokeTest`
- Not covered by automated tests: real PostgreSQL/pgvector behaviour and real OpenAI embeddings (see "Manual verification").

## Security model (summary)
- BCrypt password hashes; stateless JWT (HS256, expiry, signature check); 401 for missing/invalid/expired token, 403 for forbidden.
- The owner of every job/resume comes from the JWT principal, never from the request. Another user's resource answers **404**
  (indistinguishable from "does not exist"). Users can only access their own account.
- Secrets only via environment variables; CORS restricted to `CORS_ALLOWED_ORIGINS`.
- Uploads: PDF only (extension, content type, `%PDF-` signature), size/page limits, no OCR.
- Error bodies never contain stack traces, SQL or provider error text.

## AI features
**Skill extraction** (`POST /api/resumes/{id}/analyze`): LLM returns a comma list; output is parsed defensively (bounded, deduplicated).

**Job match** (`POST /api/job-match/resume/{r}/job/{j}`): deterministic (no LLM). Skills in the job description are found with a
built-in dictionary using whole-word matching; resume skills = LLM-extracted + dictionary matches in the resume text.
`missingSkills` means *not detected in the resume*, not "candidate lacks it" (the response carries a `disclaimer`).

**RAG insights** (`POST /api/resumes/{id}/insights`), how retrieval works:
1. Ownership of the resume (and optional job) is checked in PostgreSQL.
2. First use (or `"reindex": true`): resume text is split into ~1000-char chunks (150 overlap), embedded
   (OpenAI `text-embedding-3-small`, 1536 dims) and stored in pgvector with `userId`/`resumeId` metadata.
3. The query (question + start of the job description, if any) is embedded and the top 5 chunks above similarity 0.25 are
   retrieved with a filter on **both** `userId` and `resumeId`; results from another user are also dropped in code.
4. If nothing is retrieved, no LLM call is made and the response says so (`grounded=false`).
5. Otherwise the chunks (as delimited, untrusted data) go to the chat model with an instruction to use only that context and
   return JSON, which is validated before being returned together with the source snippets.

Limitations: only resumes are indexed (job descriptions are used as the query, not stored); retrieval is limited to 5 chunks, so
insights reflect those passages only; scanned PDFs are unsupported; quality depends on the model; no per-user rate limiting or cost
caps yet; changing embedding model/dimensions requires re-indexing (`reindex`) and a matching `app.rag.embedding-dimensions`;
the vector store is created lazily, so a missing pgvector extension shows up as a 503 on the RAG endpoint, not at startup.

## Manual verification checklist
1. `mvn clean verify` passes on your machine.
2. Start the app against PostgreSQL; register, log in, create jobs (Swagger UI or `docs/API.md` examples).
3. With pgvector + a real `OPENAI_API_KEY`: upload a PDF, call `/analyze`, then `/insights`; confirm rows appear in `vector_store`
   with your `userId`, and that a second account gets 404 for your resume id.
4. Confirm the React app works with the changes listed under "Breaking changes" in `docs/API.md`.
5. **Rotate** the database password and JWT secret that were in the original `application.properties` (they are in git history).
