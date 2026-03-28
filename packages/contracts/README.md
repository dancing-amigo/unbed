# Contracts Placeholder

Shared backend and client contracts will live here once the product moves beyond local-only MVP scope.

## Reserved Boundaries

- User-specific QR issuance and rotation
- Alarm or session sync across devices
- Release telemetry and wake-up completion analytics
- Account-linked recovery or support flows

## First Expected Contract Areas

- `user-qr`: payload format, ownership, issuance timestamp, revocation
- `alarm-session`: server-visible session identity, release outcome, and timestamps
- `device-registration`: client capability flags such as camera availability and sensor support
