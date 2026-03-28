# Entities

## AlarmConfig

- `id`: Stable alarm identifier.
- `time`: Local wake-up time.
- `repeatDays`: Empty for single alarm, weekday set for repeating alarm.
- `enabled`: Whether regular scheduling is active.
- `soundType`: Fixed in MVP.
- `releaseCondition`: `manual_release` now, `step_count_release` reserved.

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

## QrConfig

- `fixedValue`: Shared MVP QR payload used for validation.

