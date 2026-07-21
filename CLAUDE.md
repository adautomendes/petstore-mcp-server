# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Petstore MCP Server is a Spring Boot application that exposes petstore operations as [Model Context Protocol](https://modelcontextprotocol.io/) tools over SSE, so AI clients can register and query pets. It is a thin MCP-facing layer that sits in front of a separate backend stack ([petstore-ref-project](https://github.com/adautomendes/petstore-ref-project)) consisting of `petstore-auth`, `petstore-core`, `petstore-monitor`, and MongoDB — none of which live in this repo. All Java code lives under `java/`.

## Commands

All Maven commands are run from the `java/` directory.

```bash
# Build
cd java && mvn clean package -DskipTests   # build without tests
cd java && mvn clean install               # full build with tests

# Test
cd java && mvn test                        # run all tests
cd java && mvn test -Dtest=ClassName       # run a single test class

# Run locally (requires petstore-auth and petstore-core reachable)
cd java && mvn spring-boot:run \
  -Dspring-boot.run.arguments="--server.port=8082 --petstore-auth.host=localhost --petstore-core.host=localhost"
```

There are currently no test source files in `java/src`; `mvn test` will be a no-op until tests are added.

### Docker Compose (full stack)

Two compose files exist at the repo root:
- `docker-compose.dev.yaml` — builds the MCP server from source via `docker/Dockerfile`; use this when iterating on Java code.
- `docker-compose.release.yaml` — pulls the pre-built `adautomendes/petstore-mcp-server:latest` image.

Both bring up `petstore-mongo`, `petstore-auth`, `petstore-monitor`, `petstore-core`, and `petstore-mcp-server` on a shared `services-network`, with `petstore-core` gating on health checks for auth/mongo/monitor, and the MCP server gating on `petstore-core`'s health check.

```bash
docker compose -f docker-compose.dev.yaml up -d --build   # rebuild + run everything
docker compose -f docker-compose.dev.yaml logs -f petstore-mcp-server
curl http://localhost:8082/actuator/health
```

### Data population

`populator/populate-petstore.py` logs into `petstore-auth` (default `admin`/`admin`) and creates random pets directly against `petstore-core`'s REST API (not through this MCP server):

```bash
cd populator
pip install requests
python populate-petstore.py --auth-url http://localhost:3001 --core-url http://localhost:3000 --count 10
```

## Architecture

The Java package root is `com.adauto.mediamanager.mcp.server` (note: differs from the Maven groupId `com.adauto.mcp`). Three layers:

- **Adapter** (`adapter/`) — `@Configuration` classes that build `WebClient` beans pointed at downstream services, using `petstore-auth.host`/`.port` and `petstore-core.host`/`.port` properties (backed by `PETSTORE_AUTH_HOST`/`PORT` and `PETSTORE_CORE_HOST`/`PORT` env vars). `PetstoreAuthAdapter` and `PetstoreCoreAdapter` are the only integration points with the outside world.
- **Service** (`service/`) — business logic and the MCP tool surface.
  - `PetstoreCoreService` holds the `@Tool`-annotated methods (`registerPet`, `findAllPet`, `findPetByName`) that Spring AI exposes to MCP clients. Every call fetches a fresh auth token via `PetstoreAuthService.getToken()` and attaches it as a `token` header before calling `petstore-core`.
  - `PetstoreAuthService` performs the login call to `petstore-auth` and returns the bearer token; there is no token caching, so a new login happens per tool invocation.
- **Model/DTO** (`model/dto/`) — Lombok `@Data @Builder` request/response records (`PetDTO`, `LoginRequestDTO`, `LoginResponseDTO`). `PetDTO.id` is mapped from the backend's `_id` field via `@JsonProperty`.

`PetstoreMcpServerApplication` wires the MCP tool callback provider: it registers `PetstoreCoreService` as a tool object via `MethodToolCallbackProvider`, which is how Spring AI discovers the `@Tool` methods at startup. When adding a new tool, add a `@Tool`-annotated method to `PetstoreCoreService` (or a new service registered the same way) — no manual registration step is needed beyond that.

Configuration is in `java/src/main/resources/application.properties`; for local overrides create (gitignored-by-convention, currently absent) `application-local.properties` alongside it.

### Known quirks worth knowing before touching this code

- The `registerPet` tool parameter is named `nome` (Portuguese for "name"), not `name` — this must match what `petstore-core`'s API expects. `findPetByName`'s query parameter is also sent as `nome` even though the tool parameter is `name`.
- Auth credentials (`admin`/`admin`) are hardcoded in `PetstoreAuthService` — there's no per-request identity.
