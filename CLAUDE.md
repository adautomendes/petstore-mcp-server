# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Petstore MCP Server is a Spring Boot application that exposes petstore operations as [Model Context Protocol](https://modelcontextprotocol.io/) tools over SSE, so AI clients can register and query pets. It is a thin MCP-facing layer that sits in front of a separate backend stack ([petstore-ref-project](https://github.com/adautomendes/petstore-ref-project)) consisting of `petstore-auth`, `petstore-core`, `petstore-monitor`, and MongoDB — none of which live in this repo. All Java code lives under `java/`.

Stack: Java 21, Spring Boot 4.0.0, Spring AI **2.0.0-SNAPSHOT** (resolved from the Spring snapshot / Central Portal snapshot repos declared in `pom.xml`, so a build can break when the upstream snapshot changes).

## Commands

All Maven commands are run from the `java/` directory. A Maven wrapper is checked in, but `java/mvnw` lacks the executable bit in git, so invoke it as `sh mvnw ...` if Maven is not installed.

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

Two compose files exist at the repo root, identical except for the `petstore-mcp-server` service:
- `docker-compose.dev.yaml` — builds the MCP server from source via `docker/Dockerfile` (build context is the repo root; the Dockerfile copies only `java/pom.xml` and `java/src`).
- `docker-compose.release.yaml` — pulls the pre-built `adautomendes/petstore-mcp-server:latest` image.

In both, the backend services are always pulled as pre-built `adautomendes/petstore-*:latest` images. They bring up `petstore-mongo`, `petstore-auth` (3001), `petstore-monitor` (3002), `petstore-core` (3000), and `petstore-mcp-server` (8082) on a shared `services-network`, with `petstore-core` gating on health checks for auth/mongo/monitor, and the MCP server gating on `petstore-core`'s health check. The MCP server itself has no healthcheck.

```bash
docker compose -f docker-compose.dev.yaml up -d --build   # rebuild + run everything
docker compose -f docker-compose.dev.yaml logs -f petstore-mcp-server
```

Spring Boot Actuator is **not** a dependency, so `/actuator/health` (referenced in the README) does not exist.

### CI

`cicd/build-and-push.groovy` is a Jenkins pipeline that clones both this repo and `petstore-ref-project`, builds all Docker images, and pushes them to Docker Hub as `:latest` plus a version tag (for this repo, the version comes from `project.version` in `java/pom.xml`).

### Data population

`populator/populate-petstore.py` logs into `petstore-auth` (default `admin`/`admin`) and creates random pets directly against `petstore-core`'s REST API (not through this MCP server):

```bash
cd populator
pip install requests
python populate-petstore.py --auth-url http://localhost:3001 --core-url http://localhost:3000 --count 10
```

## Architecture

The Java package root is `com.adauto.mediamanager.mcp.server` (note: differs from the Maven groupId `com.adauto.mcp`). Three layers:

- **Adapter** (`adapter/`) — `@Configuration` classes that build `WebClient` beans pointed at downstream services, using `petstore-auth.host`/`.port` and `petstore-core.host`/`.port` properties (backed by `PETSTORE_AUTH_HOST`/`PORT` and `PETSTORE_CORE_HOST`/`PORT` env vars). `PetstoreAuthAdapter` and `PetstoreCoreAdapter` are the only integration points with the outside world. Services inject the adapter class itself and call its `@Bean` methods (e.g. `getPetstoreCoreWebClient()`) directly; the `@Configuration` proxy returns the singleton bean.
- **Service** (`service/`) — business logic and the MCP tool surface.
  - `PetstoreCoreService` holds the `@Tool`-annotated methods (`registerPet`, `findAllPet`, `findPetByName`) that Spring AI exposes to MCP clients. Every call fetches a fresh auth token via `PetstoreAuthService.getToken()` and attaches it as a `token` header before calling `petstore-core`.
  - `PetstoreAuthService` performs the login call (`POST /auth/login`) to `petstore-auth` and returns the token; there is no token caching, so a new login happens per tool invocation.
- **Model/DTO** (`model/dto/`) — Lombok `@Data @Builder` request/response classes (`PetDTO`, `LoginRequestDTO`, `LoginResponseDTO`). `PetDTO.id` is mapped from the backend's `_id` field via `@JsonProperty`.

The MCP server runs on the servlet stack (`spring-ai-starter-mcp-server-webmvc`); WebFlux is on the classpath only for `WebClient`, and every downstream call is synchronous via `.block()`.

`PetstoreMcpServerApplication` wires the MCP tool callback provider: it registers `PetstoreCoreService` as a tool object via `MethodToolCallbackProvider`, which is how Spring AI discovers the `@Tool` methods at startup. A new `@Tool` method on `PetstoreCoreService` is picked up automatically; a tool in a *new* service class must also be added to `toolObjects(...)` in that bean.

Configuration is in `java/src/main/resources/application.properties`. The README suggests an `application-local.properties` alongside it for local overrides (currently absent); it is only loaded with `--spring.profiles.active=local`.

### Known quirks worth knowing before touching this code

- The `registerPet` tool parameter is named `nome` (Portuguese for "name"), so that is the argument name MCP clients see in the tool schema; it is mapped into `PetDTO.name`, so the JSON body sent to `petstore-core` uses `name`. Renaming the Java parameter changes the public tool schema.
- `findPetByName` takes a `name` tool parameter but sends it to `petstore-core` as the `nome` query parameter, built with `String.format` (not URL-encoded).
- Auth credentials (`admin`/`admin`) are hardcoded in `PetstoreAuthService` — there's no per-request identity.
