# kline-core Maven Central Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `kline-parent`, `kline-common`, and `kline-core` publishable to Maven Central via Sonatype Central Portal under `io.github.hhoao`.

**Architecture:** Keep the existing multi-module structure, place release metadata and publishing plugins in the parent POM, and let `kline-common` and `kline-core` inherit the release setup. Add a checked-in credential template so release execution is repeatable.

**Tech Stack:** Maven, Sonatype Central Portal, GPG, flattened consumer POMs

---

### Task 1: Update Maven coordinates and release metadata

**Files:**
- Modify: `kline-server/pom.xml`
- Modify: `kline-server/kline-common/pom.xml`
- Modify: `kline-server/kline-core/pom.xml`

- [ ] Change the root `groupId` from `com.hhoa.kline` to `io.github.hhoao`.
- [ ] Change the shared `revision` from `1.0.0-SNAPSHOT` to `1.0.1`.
- [ ] Add project `name`, `description`, `url`, `licenses`, `developers`, and `scm` metadata in the parent POM.
- [ ] Add module-specific `name` and `description` entries to `kline-common` and `kline-core`.

### Task 2: Add a release-only Central publishing profile

**Files:**
- Modify: `kline-server/pom.xml`

- [ ] Add a `release-to-central` Maven profile.
- [ ] In that profile, attach `sources.jar` via `maven-source-plugin`.
- [ ] In that profile, attach `javadoc.jar` via `maven-javadoc-plugin` with doclint disabled for smoother first release validation.
- [ ] In that profile, sign release artifacts via `maven-gpg-plugin`.
- [ ] In that profile, publish bundles via `org.sonatype.central:central-publishing-maven-plugin`.

### Task 3: Add release operator template files

**Files:**
- Create: `kline-server/settings-central.xml.example`

- [ ] Add a checked-in `settings.xml` template using server id `central`.
- [ ] Document where to put the Portal token username/password.
- [ ] Keep secrets out of git by using placeholder values only.

### Task 4: Verify the release build locally

**Files:**
- Modify if needed: `kline-server/pom.xml`

- [ ] Run `mvn -pl kline-common,kline-core -am -Prelease-to-central -Dgpg.skip=true -DskipTests package`.
- [ ] Confirm the build produces the main jar, sources jar, and javadoc jar for both release modules.
- [ ] Fix any Maven configuration issues that block packaging.

### Task 5: Summarize the release command for actual publish

**Files:**
- No file changes required if the template is sufficient.

- [ ] Confirm the final release command is `mvn -pl kline-common,kline-core -am -Prelease-to-central deploy`.
- [ ] Call out that real publishing still requires a verified namespace and a usable local GPG key.
