# Glossary

- `regular alarm`: The next time derived from `AlarmConfig`.
- `session`: One lifecycle of a scheduled or fired alarm.
- `re-ring`: Returning from snooze back to `ringing`.
- `manual release`: MVP-only action that completes a snoozed alarm cycle.
- `release handler`: Executable policy that decides how a snoozed session can be completed.
- `step count release`: Planned future release condition driven by motion or sensor data.
- `superseded`: An in-progress session ends because a newer regular alarm takes priority.
