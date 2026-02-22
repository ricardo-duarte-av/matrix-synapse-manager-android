# Matrix Synapse Manager — Android

A production-ready Android application for administering [Synapse](https://matrix.org/docs/projects/server/synapse) homeservers.

Manage users across multiple Synapse servers from a single secure, mobile-first admin panel.

## Features

- **Multi-server management** — add unlimited Synapse servers via `.well-known` discovery or manual URL
- **Secure authentication** — admin login per server; access tokens stored in Android Keystore (EncryptedSharedPreferences); passwords are never persisted
- **User lifecycle** — list, search (paginated), create, edit, lock/unlock, suspend/unsuspend, and deactivate users
- **Media cleanup** — optional erasure of user media before deactivation
- **Device & session control** — view, inspect, and revoke device sessions; whois active-connection view
- **Audit log** — local tamper-evident log of all destructive actions, exportable as JSON with sensitive fields redacted
- **App lock** — optional biometric / device PIN gate on app resume

## Architecture

```
:app
:core:network     — Retrofit + OkHttp + kotlinx-serialization, capability detection
:core:security    — EncryptedSharedPreferences token store (no password persistence)
:core:database    — Room audit log (AuditLogEntity, AuditLogDao, AuditLogger interface)
:core:model       — Domain models (Server, ServerCapabilities)
:core:testing     — Shared test utilities, contract fixtures
:feature:auth     — Login screen, session validation
:feature:servers  — Server discovery (.well-known), profile management
:feature:users    — User list/detail/create/edit, deactivate flow
:feature:devices  — Device list, delete, whois
:feature:settings — Audit log screen, app lock settings, export
```

Clean Architecture + MVVM: ViewModels call use-cases, use-cases call repositories, repositories call per-server Retrofit instances created by `RetrofitFactory`.

## Requirements

| Requirement | Value |
|---|---|
| Minimum Android | API 26 (Android 8.0) |
| Target Android | API 35 (Android 15) |
| Minimum Synapse | See [compatibility matrix](docs/compatibility-matrix.md) |
| Build toolchain | JDK 17, AGP 8.3.2, Kotlin 2.0 |

## Building

```bash
# Clone and sync
git clone https://github.com/your-org/matrix-synapse-manager-android.git
cd matrix-synapse-manager-android

# Set SDK path (or set ANDROID_HOME environment variable)
echo "sdk.dir=$HOME/Android/Sdk" > local.properties

# Build debug APK
./gradlew :app:assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Run all checks (lint + unit tests)
./gradlew lint testDebugUnitTest
```

## Security Model

See [docs/threat-model.md](docs/threat-model.md) for a full description of the security design. Key points:

- Admin passwords are **never** stored; only short-lived access tokens are persisted
- Tokens are held in `EncryptedSharedPreferences` backed by the Android Keystore
- Cleartext HTTP is rejected at the network layer (`usesCleartextTraffic=false`)
- All destructive operations require explicit administrator confirmation
- The audit log redacts `access_token`, `token`, `password`, `refresh_token`, and `secret` values on export

## API Surface (V1)

Full list of Synapse admin API endpoints consumed by this app: [docs/compatibility-matrix.md](docs/compatibility-matrix.md).

## Contributing

1. Create a feature branch
2. Follow the TDD cycle in the plan: write failing test → implement → verify green → commit
3. Ensure `./gradlew lint testDebugUnitTest` passes before opening a PR
4. See the security hard gates in `android-gradle-build` skill — no exceptions
