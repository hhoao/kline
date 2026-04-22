# kline-core GitHub Actions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add GitHub Actions CI and manual release workflows for `kline-common` and `kline-core`.

**Architecture:** Use one workflow for validation on pushes and pull requests, and a separate workflow for manual Maven Central release with secrets-backed credentials and GPG import.

**Tech Stack:** GitHub Actions, Maven, Java 21, Sonatype Central Portal, GPG

---

### Task 1: Add CI workflow

**Files:**
- Create: `.github/workflows/ci.yml`

- [ ] Add `push` and `pull_request` triggers.
- [ ] Set up Java 21 with Maven cache.
- [ ] Run `mvn -pl kline-common,kline-core -am -DskipTests verify` from `kline-server`.

### Task 2: Add manual release workflow

**Files:**
- Create: `.github/workflows/release.yml`

- [ ] Add `workflow_dispatch` trigger.
- [ ] Create `settings.xml` from GitHub Secrets with server id `central`.
- [ ] Import a GPG private key from GitHub Secrets.
- [ ] Run `mvn -pl kline-common,kline-core -am -Prelease-to-central deploy` from `kline-server`.

### Task 3: Add operator documentation

**Files:**
- Create: `.github/workflows/README.md`

- [ ] Document required GitHub Secrets names.
- [ ] Document what `ci.yml` and `release.yml` each do.
