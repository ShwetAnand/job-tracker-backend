# API reference

Base URL `http://localhost:8080`. JSON unless noted. 🔒 = requires `Authorization: Bearer <token>`.

## Error format (every endpoint)
```json
{ "timestamp": "2026-10-15T10:00:00", "status": 400, "error": "Bad Request",
  "message": "Validation failed", "path": "/api/users", "errors": { "email": "Enter a valid email" } }
```
`errors` appears only for validation failures. Status codes: 400 validation/bad parameter, 401 missing/invalid/expired token or bad
login, 403 forbidden, 404 not found **or not yours**, 405, 409 duplicate/constraint, 413 file too large, 415 unsupported file type,
422 unreadable PDF, 429/502/503/504 AI provider problems, 500 unexpected.

## Auth & users
| Method & path | Auth | Notes |
|---|---|---|
| `POST /api/users` | public | Register. Body `{name, email, password}` (password 8-72 chars). 201 -> `{id,name,email}`. 409 if email exists. Email is lower-cased. |
| `POST /api/auth/login` | public | Body `{email, password}` -> `{token, type:"Bearer", userId, name, email}`. 401 `Invalid email or password`. |
| `GET /api/users/me` 🔒 | | Own profile. |
| `PUT /api/users/me` 🔒 | | Body as register (password required). After an **email change log in again** (token subject is the email). |
| `DELETE /api/users/me` 🔒 | | Deletes account, jobs, resumes, embeddings. |
| `GET/PUT/DELETE /api/users/{id}` 🔒 | | Kept for compatibility; only works for your own id, else 404. |

```bash
curl -X POST localhost:8080/api/users -H 'Content-Type: application/json' \
  -d '{"name":"Jane","email":"jane@example.com","password":"correct-horse-battery"}'
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"jane@example.com","password":"correct-horse-battery"}' | jq -r .token)
```

## Jobs 🔒 (always the caller's own jobs)
Body: `{companyName*, role*, status*, appliedDate* (yyyy-MM-dd), notes (<=1000), jobDescription (<=20000)}`. No `userId` (ignored if sent).
Response: `{id, companyName, role, status, appliedDate, notes, jobDescription, userId, userName}`.

| Method & path | Result |
|---|---|
| `POST /api/jobs` | 201 created |
| `GET /api/jobs` | all own jobs, newest `appliedDate` first |
| `GET /api/jobs/{id}` | 200 / 404 |
| `PUT /api/jobs/{id}` | 200 / 404 |
| `DELETE /api/jobs/{id}` | 200 `"Job Deleted Successfully"` / 404 |
| `GET /api/jobs/search?keyword=` | company/role contains keyword (case-insensitive); blank -> 400 |
| `GET /api/jobs/filter?status=` | status equals (case-insensitive) |
| `GET /api/jobs/page?page=0&size=5&sortBy=companyName&direction=asc&keyword=&status=` | Spring `Page` JSON (`content`, `totalElements`, `totalPages`, `number`, ...). `sortBy` in `id, companyName, role, status, appliedDate`; `size` 1-100; `direction` asc/desc; invalid -> 400 |

```bash
curl -X POST localhost:8080/api/jobs -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"companyName":"Acme","role":"Backend","status":"APPLIED","appliedDate":"2026-10-01","jobDescription":"Java, Spring Boot, Kubernetes"}'
```

## Resumes 🔒
| Method & path | Notes |
|---|---|
| `POST /api/resumes/upload` | `multipart/form-data`, part `file` (PDF, <=5 MB, <=20 pages, must contain selectable text). 201 -> `{id,fileName,rawText,extractedSkills,uploadedAt,indexedAt,userId,userName}`. 413/415/422/400 per rule above. |
| `GET /api/resumes` | own resumes (without `rawText`) |
| `GET /api/resumes/{id}` | with `rawText` |
| `DELETE /api/resumes/{id}` | also deletes its embeddings |
| `POST /api/resumes/{id}/analyze` | LLM skill extraction -> resume with `extractedSkills` (`"Java, Spring Boot"`). AI errors: 429/502/503/504 |
| `POST /api/resumes/{id}/insights` | RAG. Body (optional) `{question?<=500 chars, jobId?, reindex?}` |

Insights response:
```json
{ "resumeId": 3, "jobId": 7, "question": "...", "grounded": true, "summary": "...",
  "strengths": ["..."], "gaps": ["No evidence of Kubernetes in the retrieved passages"], "recommendations": ["..."],
  "sources": [{ "chunkIndex": 2, "snippet": "...", "score": 0.71 }], "disclaimer": "..." }
```
`grounded=false` means nothing relevant was retrieved and no model answer was generated. 503 `Retrieval (vector store) is unavailable`
= RAG disabled or pgvector missing.

## Job match 🔒
`POST /api/job-match/resume/{resumeId}/job/{jobId}` ->
```json
{ "resumeId": 3, "jobId": 7, "companyName": "Acme", "role": "Backend", "matchPercentage": 66.67,
  "matchedSkills": ["java","spring"], "missingSkills": ["kubernetes"], "disclaimer": "Skills listed as missing were not detected ..." }
```
`missingSkills` = required by the job description but **not detected** in the resume. Skill names are lower-case canonical names
(`"Spring Boot"` -> `spring`). Works without AI. 400 if the job has no description; 404 if either id is not yours.

## Breaking changes for the React app
1. Validation errors now use the standard error body (`errors` map) instead of a bare `{field: message}` object.
2. `GET /api/users` (list all users) was removed (it exposed everyone's emails); use `GET /api/users/me`.
3. `userId` is no longer read from job bodies or from the resume upload form; the server uses the token's user (extra fields are ignored).
4. Other users' resources return 404 (previously readable). Unauthenticated calls return 401 (previously 403).
5. Password minimum is now 8 characters (was 6); emails are stored lower-case and matched case-insensitively.
6. `/api/auth/**` is no longer entirely public: only `POST /api/auth/login` is.
7. Job match no longer requires `/analyze` first and its numbers differ because matching is now whole-word (the old `contains` logic over-matched).
8. `JobMatchResponse` gained `disclaimer`; resume responses gained `indexedAt`; the paging endpoint gained optional `keyword`/`status`.
