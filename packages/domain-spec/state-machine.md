# Alarm State Machine

```text
idle
  -> scheduled
scheduled
  -> ringing
ringing
  -> scanning_qr
scanning_qr
  -> ringing
  -> snoozed_waiting_release
snoozed_waiting_release
  -> cleared
  -> ringing
  -> idle
  -> scheduled
cleared
  -> idle
  -> scheduled
```

## Domain Rules

- QR mismatch or scan cancellation returns to `ringing`.
- QR success does not finish the alarm; it transitions to `snoozed_waiting_release`.
- A snooze expiry increments `snoozeCycleCount` and returns to `ringing`.
- Once re-rings exceed the cap of 10, the session ends as `idle` for a single alarm or `scheduled` for a repeating alarm.
- `manual_release`, `rering_cap_reached`, and `superseded_by_next_schedule` are the supported session end reasons.
- If a regular alarm arrives earlier than a snoozed re-ring, the regular alarm wins and the old session is considered superseded.
