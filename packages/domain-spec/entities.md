# Entities

## AlarmConfig

- `id`: Stable alarm identifier.
- `time`: Local wake-up time.
- `repeatDays`: Empty for single alarm, weekday set for repeating alarm.
- `enabled`: Whether regular scheduling is active.
- `soundType`: Fixed in MVP.
- `releaseCondition`: persisted selector for `manual_release` now, `step_count_release` later.

## AlarmSession

- `sessionId`: Unique per fired/scheduled session.
- `alarmId`: Parent `AlarmConfig` identifier.
- `scheduledAt`: The regular trigger the session belongs to.
- `state`: Domain state machine status.
- `qrValidatedAt`: Timestamp of the last successful QR validation.
- `snoozeUntil`: Timestamp for the next required re-ring.
- `releasedAt`: Timestamp for release completion.
- `snoozeCycleCount`: Count of completed re-rings.
- `sessionEndedReason`: Why the session stopped.
- release-condition execution is intentionally delegated to a handler boundary so future release logic can change without rewriting session persistence.

## QrConfig

- `fixedValue`: Shared MVP QR payload used for validation.
