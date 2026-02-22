# Threat Model

## Assets

| Asset | Sensitivity | Storage |
|---|---|---|
| Admin access token | Critical | EncryptedSharedPreferences (Android Keystore AES-256-GCM) |
| Admin password | Critical | Never stored — discarded immediately after login |
| Server URLs | Low | In-memory only (V1); Room persistence deferred to V2 |
| Audit log | Medium | Room database (unencrypted — contains action metadata, not tokens) |
| App lock preference | Low | Plain SharedPreferences (non-sensitive boolean) |

## Trust Boundaries

```
[Administrator] ──HTTPS──► [Synapse Server]
      │
  [Android App]
      │
  [Android Keystore] ── AES-256-GCM ──► [EncryptedSharedPreferences: tokens]
      │
  [Room Database] ── plaintext ──► [audit_log table: redacted metadata]
```

## Threat Scenarios and Mitigations

### T1: Token exfiltration via malicious app

**Threat:** A malicious app reads the access token from storage.

**Mitigation:** Tokens are stored in `EncryptedSharedPreferences` backed by the Android Keystore. The encryption key is hardware-backed on devices with a Secure Element. No other app can read the key material.

---

### T2: Password capture

**Threat:** An attacker captures the admin password in transit or from storage.

**Mitigation:** Passwords are passed directly to the Synapse login API and then discarded. The `SecureTokenStore` interface has no `savePassword()` method. The `LoginUseCase` only calls `tokenStore.saveToken()` after receiving the access token.

---

### T3: Man-in-the-middle on the Synapse connection

**Threat:** An attacker intercepts admin API calls over HTTP.

**Mitigation:**
- `usesCleartextTraffic="false"` in `AndroidManifest.xml` — the OS blocks all cleartext HTTP connections.
- The `RetrofitFactory` enforces a trailing slash on HTTPS base URLs.
- Server discovery appends `https://` to bare domain inputs before any network call.

---

### T4: Audit log leaks sensitive data on export

**Threat:** An exported JSON audit log contains access tokens or passwords.

**Mitigation:** `ExportAuditLogUseCase` iterates the `details` map of each entry and replaces values for any key matching `{access_token, token, password, refresh_token, secret}` with `"[REDACTED]"` before serialising to JSON.

---

### T5: Unauthorised physical access to the device

**Threat:** An attacker with physical access to an unlocked device opens the app.

**Mitigation:** The optional app lock feature prompts for biometric or device PIN authentication on every resume when enabled. `AppLockManager.lock()` is called in `MainActivity.onResume()`.

---

### T6: Accidental destructive action

**Threat:** An administrator accidentally deactivates a user or deletes a device.

**Mitigation:** All destructive actions (deactivate, delete device) require explicit confirmation dialogs. Deactivation additionally requires the administrator to type the target user ID verbatim (`DeactivateDialogState.Confirming.isConfirmed`).

---

## Out of Scope (V1)

- End-to-end encryption of the Room audit log database
- Certificate pinning for Synapse connections (operator-configured CAs are trusted)
- Room/moderation/federation endpoints (deferred to V2)
- Offline or background refresh of token validity
