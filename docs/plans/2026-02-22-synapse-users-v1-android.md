# Synapse Users-Only Android App (V1) Implementation Plan

> **For Claude:** Use `${SUPERPOWERS_SKILLS_ROOT}/skills/collaboration/executing-plans/SKILL.md` to implement this plan task-by-task.

**Goal:** Build a production-ready Android app to manage Synapse users only (multi-server setup, auth, user lifecycle, device/session control, audit log) with optional app lock and robust test coverage.

**Architecture:** Use Clean Architecture + MVVM with modular feature separation. All network calls go through Retrofit repositories and use-case boundaries, with runtime capability gating based on Synapse version/endpoint availability. Keep secrets in Keystore-backed encrypted storage and never persist admin passwords.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Retrofit + OkHttp + Kotlinx Serialization, Hilt, Room, EncryptedSharedPreferences/Android Keystore, WorkManager, JUnit5, MockWebServer, Turbine, Compose UI Test.

---

## API Surface for V1
- Discovery: `GET /.well-known/matrix/client`
- Login: `POST /_matrix/client/v3/login`
- Server version: `GET /_synapse/admin/v1/server_version`
- Users list/detail/create-update:
  - `GET /_synapse/admin/v2/users`
  - `GET /_synapse/admin/v2/users/{userId}`
  - `PUT /_synapse/admin/v2/users/{userId}`
- User deactivate: `POST /_synapse/admin/v1/deactivate/{userId}`
- User suspend: `PUT /_synapse/admin/v1/suspend/{userId}`
- User media list/delete:
  - `GET /_synapse/admin/v1/users/{userId}/media`
  - `DELETE /_synapse/admin/v1/media/{serverName}/{mediaId}`
- Device/session controls:
  - `GET /_synapse/admin/v2/users/{userId}/devices`
  - `GET /_synapse/admin/v2/users/{userId}/devices/{deviceId}`
  - `DELETE /_synapse/admin/v2/users/{userId}/devices/{deviceId}`
  - `GET /_synapse/admin/v1/whois/{userId}`

## Project Layout
- `app/`
- `core/network/`
- `core/security/`
- `core/database/`
- `core/model/`
- `core/testing/`
- `feature/servers/`
- `feature/auth/`
- `feature/users/`
- `feature/devices/`
- `feature/settings/`

## Test Strategy
- Unit tests for use-cases, mappers, and security abstractions.
- Integration tests with MockWebServer for API and error handling.
- Compose UI tests for critical flows (server add/login/user actions).
- Contract tests for Synapse response parsing and pagination.

### Task 1: Bootstrap Project and Modules

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `app/build.gradle.kts`
- Create: `core/network/build.gradle.kts`
- Create: `core/security/build.gradle.kts`
- Create: `core/database/build.gradle.kts`
- Create: `feature/servers/build.gradle.kts`
- Create: `feature/auth/build.gradle.kts`
- Create: `feature/users/build.gradle.kts`
- Create: `feature/devices/build.gradle.kts`
- Create: `feature/settings/build.gradle.kts`
- Test: `app/src/test/java/com/matrixmanager/BuildConfigSmokeTest.kt`

**Step 1: Write the failing test**
```kotlin
@Test
fun app_build_config_is_loadable() {
  assertNotNull(BuildConfig.APPLICATION_ID)
}
```

**Step 2: Run test to verify it fails**
Run: `./gradlew :app:testDebugUnitTest --tests BuildConfigSmokeTest -i`
Expected: FAIL with unresolved project/module or BuildConfig references.

**Step 3: Write minimal implementation**
- Add Gradle module declarations and Android/Kotlin plugins.
- Add dependency versions catalog or top-level constants.

**Step 4: Run test to verify it passes**
Run: `./gradlew :app:testDebugUnitTest --tests BuildConfigSmokeTest -i`
Expected: PASS.

**Step 5: Commit**
```bash
git add .
git commit -m "chore: scaffold modular android project"
```

### Task 2: Core Networking and Error Parsing

**Files:**
- Create: `core/network/src/main/java/com/matrixmanager/network/SynapseApi.kt`
- Create: `core/network/src/main/java/com/matrixmanager/network/NetworkModule.kt`
- Create: `core/network/src/main/java/com/matrixmanager/network/MatrixError.kt`
- Create: `core/network/src/main/java/com/matrixmanager/network/MatrixErrorParser.kt`
- Test: `core/network/src/test/java/com/matrixmanager/network/MatrixErrorParserTest.kt`
- Test: `core/network/src/test/java/com/matrixmanager/network/AuthHeaderInterceptorTest.kt`

**Step 1: Write the failing tests**
```kotlin
@Test
fun parses_errcode_and_error_message() { /* ... */ }

@Test
fun strips_authorization_header_from_logs() { /* ... */ }
```

**Step 2: Run test to verify it fails**
Run: `./gradlew :core:network:testDebugUnitTest -i`
Expected: FAIL due to missing parser/interceptor.

**Step 3: Write minimal implementation**
- Create DTOs for Matrix errors.
- Parse `errcode` and `error` safely on invalid JSON.
- Add auth header interceptor and redacting logging interceptor.

**Step 4: Run test to verify it passes**
Run: `./gradlew :core:network:testDebugUnitTest -i`
Expected: PASS.

**Step 5: Commit**
```bash
git add core/network
git commit -m "feat: add network core with matrix error parsing and header redaction"
```

### Task 3: Secure Token Storage (No Password Persistence)

**Files:**
- Create: `core/security/src/main/java/com/matrixmanager/security/SecureTokenStore.kt`
- Create: `core/security/src/main/java/com/matrixmanager/security/TokenStoreImpl.kt`
- Test: `core/security/src/test/java/com/matrixmanager/security/TokenStoreTest.kt`

**Step 1: Write the failing test**
```kotlin
@Test
fun saves_and_reads_token_without_password_field() { /* ... */ }
```

**Step 2: Run test to verify it fails**
Run: `./gradlew :core:security:testDebugUnitTest -i`
Expected: FAIL because store is missing.

**Step 3: Write minimal implementation**
- Implement Keystore-backed encrypted token storage.
- Expose CRUD by `serverId`.
- Explicitly refuse password persistence API.

**Step 4: Run test to verify it passes**
Run: `./gradlew :core:security:testDebugUnitTest -i`
Expected: PASS.

**Step 5: Commit**
```bash
git add core/security
git commit -m "feat: add secure token store with no password persistence"
```

### Task 4: Server Profile + Discovery Feature

**Files:**
- Create: `feature/servers/src/main/java/com/matrixmanager/servers/data/WellKnownApi.kt`
- Create: `feature/servers/src/main/java/com/matrixmanager/servers/data/ServerRepository.kt`
- Create: `feature/servers/src/main/java/com/matrixmanager/servers/domain/DiscoverServerUseCase.kt`
- Create: `feature/servers/src/main/java/com/matrixmanager/servers/ui/ServerFormViewModel.kt`
- Create: `feature/servers/src/main/java/com/matrixmanager/servers/ui/ServerFormScreen.kt`
- Test: `feature/servers/src/test/java/com/matrixmanager/servers/DiscoverServerUseCaseTest.kt`
- Test: `feature/servers/src/androidTest/java/com/matrixmanager/servers/ServerFormScreenTest.kt`

**Step 1: Write the failing tests**
```kotlin
@Test
fun extracts_homeserver_base_url_from_well_known() { /* ... */ }

@Test
fun add_server_shows_error_on_invalid_domain() { /* ... */ }
```

**Step 2: Run test to verify it fails**
Run: `./gradlew :feature:servers:testDebugUnitTest :feature:servers:connectedDebugAndroidTest -i`
Expected: FAIL due to missing form/use-case.

**Step 3: Write minimal implementation**
- Implement `.well-known` call + base URL extraction.
- Build Server add/edit form with validation.

**Step 4: Run test to verify it passes**
Run: same as step 2.
Expected: PASS.

**Step 5: Commit**
```bash
git add feature/servers
git commit -m "feat: add server discovery and profile management"
```

### Task 5: Auth Login + Session Restore

**Files:**
- Create: `feature/auth/src/main/java/com/matrixmanager/auth/data/AuthApi.kt`
- Create: `feature/auth/src/main/java/com/matrixmanager/auth/domain/LoginUseCase.kt`
- Create: `feature/auth/src/main/java/com/matrixmanager/auth/domain/ValidateSessionUseCase.kt`
- Create: `feature/auth/src/main/java/com/matrixmanager/auth/ui/LoginViewModel.kt`
- Create: `feature/auth/src/main/java/com/matrixmanager/auth/ui/LoginScreen.kt`
- Test: `feature/auth/src/test/java/com/matrixmanager/auth/LoginUseCaseTest.kt`
- Test: `feature/auth/src/test/java/com/matrixmanager/auth/ValidateSessionUseCaseTest.kt`

**Step 1: Write the failing tests**
```kotlin
@Test
fun login_stores_access_token_on_success() { /* ... */ }

@Test
fun expired_token_triggers_reauth_state() { /* ... */ }
```

**Step 2: Run test to verify it fails**
Run: `./gradlew :feature:auth:testDebugUnitTest -i`
Expected: FAIL with missing use-case/viewmodel behavior.

**Step 3: Write minimal implementation**
- Implement login endpoint integration.
- Save token in secure store.
- Add re-auth state mapping for `401/403`.

**Step 4: Run test to verify it passes**
Run: `./gradlew :feature:auth:testDebugUnitTest -i`
Expected: PASS.

**Step 5: Commit**
```bash
git add feature/auth
git commit -m "feat: add auth login and session restore"
```

### Task 6: Capability Detection Service

**Files:**
- Create: `core/network/src/main/java/com/matrixmanager/network/CapabilityService.kt`
- Create: `core/model/src/main/java/com/matrixmanager/model/ServerCapabilities.kt`
- Test: `core/network/src/test/java/com/matrixmanager/network/CapabilityServiceTest.kt`

**Step 1: Write the failing test**
```kotlin
@Test
fun marks_suspend_unsupported_when_endpoint_returns_unrecognized() { /* ... */ }
```

**Step 2: Run test to verify it fails**
Run: `./gradlew :core:network:testDebugUnitTest --tests *CapabilityServiceTest -i`
Expected: FAIL.

**Step 3: Write minimal implementation**
- Probe server version endpoint.
- Probe selected user/device endpoints and cache support flags per server.

**Step 4: Run test to verify it passes**
Run: same as step 2.
Expected: PASS.

**Step 5: Commit**
```bash
git add core/network core/model
git commit -m "feat: add capability detection and caching"
```

### Task 7: Users List + Search + Pagination

**Files:**
- Create: `feature/users/src/main/java/com/matrixmanager/users/data/UserAdminApi.kt`
- Create: `feature/users/src/main/java/com/matrixmanager/users/data/UserRepository.kt`
- Create: `feature/users/src/main/java/com/matrixmanager/users/ui/UserListViewModel.kt`
- Create: `feature/users/src/main/java/com/matrixmanager/users/ui/UserListScreen.kt`
- Test: `feature/users/src/test/java/com/matrixmanager/users/UserRepositoryPaginationTest.kt`
- Test: `feature/users/src/androidTest/java/com/matrixmanager/users/UserListScreenTest.kt`

**Step 1: Write the failing tests**
```kotlin
@Test
fun appends_next_page_when_next_token_present() { /* ... */ }

@Test
fun search_filters_local_loaded_results() { /* ... */ }
```

**Step 2: Run test to verify it fails**
Run: `./gradlew :feature:users:testDebugUnitTest :feature:users:connectedDebugAndroidTest -i`
Expected: FAIL.

**Step 3: Write minimal implementation**
- Implement paginated `GET /users` flow.
- Add debounced search in UI.

**Step 4: Run test to verify it passes**
Run: same as step 2.
Expected: PASS.

**Step 5: Commit**
```bash
git add feature/users
git commit -m "feat: add users list with pagination and search"
```

### Task 8: User Detail + Create/Update

**Files:**
- Create: `feature/users/src/main/java/com/matrixmanager/users/domain/UpsertUserUseCase.kt`
- Create: `feature/users/src/main/java/com/matrixmanager/users/ui/UserEditViewModel.kt`
- Create: `feature/users/src/main/java/com/matrixmanager/users/ui/UserEditScreen.kt`
- Test: `feature/users/src/test/java/com/matrixmanager/users/UpsertUserUseCaseTest.kt`
- Test: `feature/users/src/androidTest/java/com/matrixmanager/users/UserEditScreenTest.kt`

**Step 1: Write the failing tests**
```kotlin
@Test
fun creates_new_user_with_required_fields() { /* ... */ }

@Test
fun updates_existing_user_without_overwriting_unset_fields() { /* ... */ }
```

**Step 2: Run test to verify it fails**
Run: `./gradlew :feature:users:testDebugUnitTest --tests *UpsertUserUseCaseTest -i`
Expected: FAIL.

**Step 3: Write minimal implementation**
- Implement detail fetch + upsert behavior.
- Validate username/password/displayname inputs.

**Step 4: Run test to verify it passes**
Run: same as step 2.
Expected: PASS.

**Step 5: Commit**
```bash
git add feature/users
git commit -m "feat: add user detail and create update flow"
```

### Task 9: User Lock/Unlock and Suspend/Unsuspend

**Files:**
- Modify: `feature/users/src/main/java/com/matrixmanager/users/data/UserRepository.kt`
- Modify: `feature/users/src/main/java/com/matrixmanager/users/ui/UserDetailViewModel.kt`
- Test: `feature/users/src/test/java/com/matrixmanager/users/UserStateActionsTest.kt`

**Step 1: Write the failing tests**
```kotlin
@Test
fun toggles_locked_flag_via_user_put() { /* ... */ }

@Test
fun hides_suspend_action_when_capability_missing() { /* ... */ }
```

**Step 2: Run test to verify it fails**
Run: `./gradlew :feature:users:testDebugUnitTest --tests *UserStateActionsTest -i`
Expected: FAIL.

**Step 3: Write minimal implementation**
- Add lock/unlock mutation via `PUT /users/{id}`.
- Add suspend/unsuspend via endpoint when supported.
- Gate UI actions by capability flags.

**Step 4: Run test to verify it passes**
Run: same as step 2.
Expected: PASS.

**Step 5: Commit**
```bash
git add feature/users
git commit -m "feat: add lock unlock and suspend actions with capability gating"
```

### Task 10: Deactivate Flow with Optional Media Cleanup

**Files:**
- Modify: `feature/users/src/main/java/com/matrixmanager/users/data/UserRepository.kt`
- Create: `feature/users/src/main/java/com/matrixmanager/users/domain/DeactivateUserUseCase.kt`
- Create: `feature/users/src/main/java/com/matrixmanager/users/ui/DeactivateDialogState.kt`
- Test: `feature/users/src/test/java/com/matrixmanager/users/DeactivateUserUseCaseTest.kt`
- Test: `feature/users/src/androidTest/java/com/matrixmanager/users/DeactivateUserFlowTest.kt`

**Step 1: Write the failing tests**
```kotlin
@Test
fun deactivation_deletes_media_first_when_option_enabled() { /* ... */ }

@Test
fun typed_confirmation_is_required_for_deactivate() { /* ... */ }
```

**Step 2: Run test to verify it fails**
Run: `./gradlew :feature:users:testDebugUnitTest :feature:users:connectedDebugAndroidTest -i`
Expected: FAIL.

**Step 3: Write minimal implementation**
- Add typed confirmation dialog.
- If option enabled: list user media then delete items, continue on partial failures, then deactivate with `erase=true`.

**Step 4: Run test to verify it passes**
Run: same as step 2.
Expected: PASS.

**Step 5: Commit**
```bash
git add feature/users
git commit -m "feat: add safe deactivate flow with optional media cleanup"
```

### Task 11: Device and Whois Controls

**Files:**
- Create: `feature/devices/src/main/java/com/matrixmanager/devices/data/DeviceAdminApi.kt`
- Create: `feature/devices/src/main/java/com/matrixmanager/devices/data/DeviceRepository.kt`
- Create: `feature/devices/src/main/java/com/matrixmanager/devices/ui/DeviceListViewModel.kt`
- Create: `feature/devices/src/main/java/com/matrixmanager/devices/ui/DeviceListScreen.kt`
- Create: `feature/devices/src/main/java/com/matrixmanager/devices/ui/WhoisScreen.kt`
- Test: `feature/devices/src/test/java/com/matrixmanager/devices/DeviceRepositoryTest.kt`
- Test: `feature/devices/src/androidTest/java/com/matrixmanager/devices/DeviceActionsUiTest.kt`

**Step 1: Write the failing tests**
```kotlin
@Test
fun deletes_selected_device_and_refreshes_list() { /* ... */ }

@Test
fun whois_response_maps_active_sessions() { /* ... */ }
```

**Step 2: Run test to verify it fails**
Run: `./gradlew :feature:devices:testDebugUnitTest :feature:devices:connectedDebugAndroidTest -i`
Expected: FAIL.

**Step 3: Write minimal implementation**
- Implement device list/detail/delete and whois read.
- Add confirmation modal before deleting device.

**Step 4: Run test to verify it passes**
Run: same as step 2.
Expected: PASS.

**Step 5: Commit**
```bash
git add feature/devices
git commit -m "feat: add device management and whois session views"
```

### Task 12: Local Audit Log + Export

**Files:**
- Create: `core/database/src/main/java/com/matrixmanager/database/AuditLogEntity.kt`
- Create: `core/database/src/main/java/com/matrixmanager/database/AuditLogDao.kt`
- Create: `feature/settings/src/main/java/com/matrixmanager/settings/ui/AuditLogScreen.kt`
- Create: `feature/settings/src/main/java/com/matrixmanager/settings/domain/ExportAuditLogUseCase.kt`
- Test: `core/database/src/test/java/com/matrixmanager/database/AuditLogDaoTest.kt`
- Test: `feature/settings/src/test/java/com/matrixmanager/settings/ExportAuditLogUseCaseTest.kt`

**Step 1: Write the failing tests**
```kotlin
@Test
fun writes_audit_record_for_destructive_user_actions() { /* ... */ }

@Test
fun exports_json_with_redacted_sensitive_fields() { /* ... */ }
```

**Step 2: Run test to verify it fails**
Run: `./gradlew :core:database:testDebugUnitTest :feature:settings:testDebugUnitTest -i`
Expected: FAIL.

**Step 3: Write minimal implementation**
- Add Room entity/DAO for audit entries.
- Add log writes for login, create user, lock/suspend, deactivate, device delete.
- Export JSON through share sheet.

**Step 4: Run test to verify it passes**
Run: same as step 2.
Expected: PASS.

**Step 5: Commit**
```bash
git add core/database feature/settings
git commit -m "feat: add local audit log and json export"
```

### Task 13: Optional Biometric/PIN App Lock

**Files:**
- Create: `feature/settings/src/main/java/com/matrixmanager/settings/security/AppLockManager.kt`
- Create: `feature/settings/src/main/java/com/matrixmanager/settings/ui/AppLockSettingsScreen.kt`
- Modify: `app/src/main/java/com/matrixmanager/MainActivity.kt`
- Test: `feature/settings/src/test/java/com/matrixmanager/settings/AppLockManagerTest.kt`
- Test: `app/src/androidTest/java/com/matrixmanager/AppLockFlowTest.kt`

**Step 1: Write the failing tests**
```kotlin
@Test
fun app_lock_disabled_by_default_and_toggleable() { /* ... */ }

@Test
fun locked_app_requires_successful_biometric_or_pin() { /* ... */ }
```

**Step 2: Run test to verify it fails**
Run: `./gradlew :feature:settings:testDebugUnitTest :app:connectedDebugAndroidTest -i`
Expected: FAIL.

**Step 3: Write minimal implementation**
- Add settings toggle for app lock.
- Wire biometric prompt/PIN fallback on resume.

**Step 4: Run test to verify it passes**
Run: same as step 2.
Expected: PASS.

**Step 5: Commit**
```bash
git add app feature/settings
git commit -m "feat: add optional biometric or pin app lock"
```

### Task 14: End-to-End Regression and Documentation

**Files:**
- Create: `README.md`
- Create: `docs/compatibility-matrix.md`
- Create: `docs/threat-model.md`
- Create: `.github/workflows/android-ci.yml`
- Test: `core/testing/src/test/java/com/matrixmanager/testing/SynapseContractFixturesTest.kt`

**Step 1: Write the failing test**
```kotlin
@Test
fun synapse_contract_fixtures_cover_supported_user_endpoints() { /* ... */ }
```

**Step 2: Run test to verify it fails**
Run: `./gradlew testDebugUnitTest -i`
Expected: FAIL until fixtures/docs/CI wiring complete.

**Step 3: Write minimal implementation**
- Add README setup and architecture overview.
- Add compatibility matrix for maintained Synapse versions only.
- Add threat model and CI workflow for lint, unit, and connected tests.

**Step 4: Run test to verify it passes**
Run: `./gradlew lint testDebugUnitTest connectedDebugAndroidTest -i`
Expected: PASS (or clearly documented emulator-only failures in CI notes).

**Step 5: Commit**
```bash
git add .
git commit -m "docs: add compatibility matrix threat model and CI"
```

## Definition of Done
- Users-only admin flows are functional across supported Synapse versions.
- Passwords are never persisted; tokens are encrypted.
- All destructive actions require explicit confirmation.
- Core tests (unit + integration + key UI tests) pass in CI.
- Unsupported endpoints are hidden or disabled via capability detection.
- Audit logging and export work without leaking secrets.

## Deferred to V2
- Room administration
- Moderation reports and event inspection
- Federation diagnostics
- Background jobs and operational dashboards

