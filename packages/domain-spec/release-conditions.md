# Release Conditions

## MVP

- `manual_release` is the only executable release condition today.
- The app reaches `snoozed_waiting_release` after a successful QR scan.
- While in `snoozed_waiting_release`, the release handler decides how the session can terminate.

## Handler Boundary

- `ReleaseConditionType` is the persisted configuration value.
- `ReleaseConditionHandler` is the executable domain boundary.
- `ReleaseConditionHandlerRegistry` resolves the handler for the configured condition.
- `ManualReleaseHandler` is the current implementation.

## Planned `step_count_release`

The future step-based release flow is expected to require:

- a target step count, for example `stepsRequired`
- a sensor availability check
- a foreground collection flow while the session is snoozed
- a timeout or retry policy if sensor data is unavailable

The alarm session state machine does not need a new top-level state for this change. The release condition should operate inside `snoozed_waiting_release`.
