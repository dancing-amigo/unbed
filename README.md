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
- STEP4-STEP10: settings UI, alarm dispatch, QR scanning, onboarding, and recovery flow
- STEP11-STEP13: test coverage, MVP polish, and future release-condition boundaries

## Current MVP Flow

1. Complete onboarding for notifications, camera, exact alarms, battery settings, and fixed QR placement.
2. Save one local alarm with optional repeat days.
3. When the alarm fires, scan the fixed QR away from bed.
4. Enter snooze for 10 minutes.
5. Tap `Release complete` before the next re-ring, or the alarm loops again.

## Known MVP Constraints

- A single shared fixed QR is used for every user.
- Camera access is mandatory to clear a ringing alarm.
- There is no fallback dismissal path when QR scanning is unavailable.
- Re-rings stop after 10 snooze cycles for a single session.
- `manual_release` is temporary and intended to be replaced by `step_count_release`.
- Android is the only supported runtime today.

## Verification Notes

- Repository verification runs with `ktlintCheck detekt test`.
- Compose UI tests are included under `apps/android/app/src/androidTest`, but no device or emulator was available in this environment to execute them.
- Real-device checks still need to cover alarm timing accuracy, lock-screen takeover, vendor battery policies, and camera behavior.

## Android Device Checklist

- Grant notifications and camera permission on first launch.
- Allow exact alarms on Android 12+.
- Exclude the app from battery optimization on vendors with aggressive background limits.
- Confirm `BOOT_COMPLETED`, time changes, and timezone changes reschedule the next trigger.
- Verify QR mismatch keeps the alarm active and QR success enters snooze.
