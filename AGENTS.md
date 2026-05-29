# AGENTS.md

Guidance for coding agents working in `ExyliaCommons`.

## 1) Repository Snapshot

- Build system: Gradle (`build.gradle`, `settings.gradle`)
- Language level: Java 21
- Main source root: `src/main/java`
- Test source root: `src/test/java` (currently no tests detected)
- Artifact style: shaded jar via `shadowJar` (wired into `build`)
- Publishing: `publishToMavenLocal` via `maven-publish`
- Key package families: `net.exylia.commons` and `net.exylia.commons.v2`

## 2) Rule Sources Checked

- `.cursor/rules/`: not found
- `.cursorrules`: not found
- `.github/copilot-instructions.md`: not found
- Existing `AGENTS.md`: not found (this file is newly created)

If Cursor/Copilot rules are added later, treat them as high-priority repository policy.

## 3) Build, Check, Test Commands

Run from repository root: `C:\Seafile\JavaProjects\Exylia\ExyliaCommons`.

### Core commands

- Full build (includes `shadowJar`):
  - `./gradlew clean build` (Windows: `gradlew.bat clean build`)
- Build without clean:
  - `./gradlew build`
- Create shaded jar directly:
  - `./gradlew shadowJar`
- Publish to local Maven:
  - `./gradlew publishToMavenLocal`

### Verification commands

- Standard verification pipeline:
  - `./gradlew check`
- Run all tests:
  - `./gradlew test`

### Run a single test (important)

- Single test class:
  - `./gradlew test --tests "com.example.MyTest"`
- Single test method:
  - `./gradlew test --tests "com.example.MyTest.myMethod"`
- Pattern match:
  - `./gradlew test --tests "*MyTest"`

Note: this repository currently appears to have no test classes under `src/test/java`, so `test` may be a no-op until tests are added.

### Lint/format status

- No dedicated lint plugin found (no Checkstyle/Spotless/PMD config in repo root).
- Use `./gradlew check` as the default quality gate.

## 4) Coding Style and Conventions

### General

- Keep code in English.
- Keep code clean, efficient, and readable.
- Avoid adding comments unless logic is non-obvious.

### Formatting

- 4-space indentation.
- K&R brace style (`if (...) {`).
- Keep methods focused and cohesive.
- Preserve existing wrapping/alignment style in touched files.

### Imports

- Both wildcard and explicit imports exist in this codebase.
- Follow local file style when editing existing files.
- For new files, prefer explicit imports unless wildcard materially improves clarity.
- Static wildcard imports are used in some legacy areas; avoid introducing new ones unless consistent with nearby code.

### Types and API shape

- Target Java 21 APIs where useful.
- Use Lombok where it reduces boilerplate and improves clarity (`@Getter`, etc.).
- Prefer interfaces/public service APIs for extension points.
- Maintain stable public entry points; avoid unnecessary breaking API changes.

### Naming

- Packages: lowercase (`net.exylia.commons...`).
- Classes/interfaces: `PascalCase`.
- Methods/fields/variables: `camelCase`.
- Constants: `UPPER_SNAKE_CASE`.
- Suffix patterns commonly used: `Manager`, `Registry`, `Provider`, `Adapter`, `Config`, `Exception`.

### Nullability and validation

- Use defensive null checks at boundaries (`Objects.requireNonNull` where appropriate).
- Use `@NotNull`/`@Nullable` annotations where API contracts benefit.
- Fail fast with clear exception messages for invalid state/input.

### Error handling

- Prefer domain-specific exceptions in domain modules (e.g., database exceptions).
- Preserve original causes when wrapping exceptions.
- Do not silently swallow errors.
- Restore interrupt status when catching `InterruptedException` (`Thread.currentThread().interrupt()`).

### Logging

- Prefer existing logging utilities (`DebugAPI`, `DebugUtils`) already used by module.
- Log concise, actionable messages with context (operation + component).

## 5) Concurrency and Threading Rules

These are critical for Bukkit/Paper compatibility.

- Use async only for IO/DB/network/disk/heavy compute.
- Keep Bukkit API access on main/server thread unless explicitly thread-safe.
- Prefer central executors/task abstractions over creating ad-hoc pools.
- Shut down executors during plugin disable lifecycle.
- Prefer v2 task system for new work:
  - `net.exylia.commons.v2.tasks.api.TaskAPI`
  - `net.exylia.commons.v2.tasks.api.Tasks`
- Legacy `net.exylia.commons.async.AsyncExecutor` is deprecated; avoid for new code.

## 6) Data, Cache, and Command Preferences

- Prefer `net.exylia.commons.v2` modules first; check legacy root package only if needed.
- For cache in plugin-facing usage, prefer Caffeine.
- For player-bound caches, load on join and clear on quit; avoid TTL-based invalidation for session data.
- For command systems in consuming plugins, prefer Lamp (see example template dependencies).

## 7) Dependency and Build Hygiene

- Most server/plugin dependencies are `compileOnly`; do not convert to `implementation` without reason.
- Do not commit secrets/tokens (review `gradle.properties` and env usage patterns carefully).
- Keep relocation/shadow behavior intact unless a change explicitly requires it.

## 8) Practical Workflow for Agents

- Read nearby code before editing; mirror local patterns.
- If adding new functionality, place it in a scalable package structure.
- Prefer small, reviewable patches.
- If tests are added, run at least targeted tests for touched areas.
- At minimum for verification, run `./gradlew check` (or explain why not run).

## 9) Current Gaps You Should Assume

- No repository-level Cursor/Copilot instruction files currently present.
- No dedicated lint/format tooling configuration currently present.
- No test suite currently present in `src/test/java`.

When these are introduced, update this file promptly.
