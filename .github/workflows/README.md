# GitHub Actions for kline-core

## Workflows

- `ci.yml`: runs Maven verification for `kline-common` and `kline-core` on `push` and `pull_request`
- `release.yml`: manually publishes `kline-common` and `kline-core` to Maven Central with `workflow_dispatch`

## Required GitHub Secrets

- `CENTRAL_PORTAL_USERNAME`
- `CENTRAL_PORTAL_PASSWORD`
- `GPG_PRIVATE_KEY`
- `GPG_PASSPHRASE`

## Notes

- Both workflows execute from the `kline-server` directory.
- The release workflow uses the Maven `release-to-central` profile already configured in `kline-server/pom.xml`.
- The release workflow requires a `release_version` input.
- The release workflow only proceeds when all of the following are true:
  - `release_version` is not a `-SNAPSHOT`
  - `release_version` matches `<revision>` in `kline-server/pom.xml`
  - a git tag named `v<release_version>` exists
