# Unbed

Unbed is an Android-first alarm product that forces a physical wake-up flow:

1. Ring at the configured time.
2. Require QR scanning away from the bed.
3. Enter a snooze-and-release loop until the user explicitly completes release.

## Repository Layout

- `apps/android`: Android app and platform implementation.
- `apps/ios`: Placeholder for future iOS work.
- `packages/domain-spec`: Platform-agnostic domain documentation.
- `packages/contracts`: Future backend/shared contracts.
- `backend`: Placeholder for future services.
- `scripts`: Common repository commands.
- `.agent`: Planning and execution records.

## Prerequisites

- JDK 17
- Android SDK with API 34
- Gradle wrapper bootstrap files in `gradle/wrapper`

## Common Commands

```bash
./scripts/format.sh
./scripts/lint.sh
./scripts/test.sh
```

## Current Scope

- STEP1: Monorepo and Android project foundation
- STEP2: Alarm domain model and state machine
- STEP3: Local persistence model and next-trigger calculation
