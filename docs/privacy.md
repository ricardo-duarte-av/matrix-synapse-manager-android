# Privacy Policy — Matrix Synapse Manager

**Last updated:** February 2025

Matrix Synapse Manager is an Android app for administering [Matrix Synapse](https://matrix.org/docs/projects/server/synapse) homeservers. This policy describes what data the app stores and how it is used.

## Data stored on your device

The app stores the following data **only on your device**:

- **Access tokens** — After you log in to a Synapse server, the app stores an access token (and your user ID for that server) so you stay signed in. Tokens are kept in **Android Keystore** via EncryptedSharedPreferences (AES-256-GCM). **Your password is never stored**; only the token returned by the server after a successful login is saved.
- **Server URLs** — The addresses of the Synapse servers you add (e.g. `https://matrix.example.com`) so you can switch between them. These are stored on the device.
- **Optional app PIN** — If you enable “Require app PIN” in Settings, a **hashed** version of your 4-digit PIN (salt + PBKDF2) is stored in EncryptedSharedPreferences. The PIN is never stored in plain text.
- **Audit log** — If the app records an audit log of destructive actions (e.g. user deactivation, room deletion), that log is stored **only locally** in an on-device database and is not sent anywhere.

## Where data is sent

- **Synapse server you configure** — All API requests (user list, room list, admin actions, etc.) go **only** to the Synapse server URL you enter. The app does not send your data to any other servers.
- **No third parties** — We do not send data to analytics services, ad networks, or any other third parties. There are no ads and no tracking.

## Your control

- You can remove a server from the app (and optionally clear its token) at any time.
- You can disable or change the app PIN in Settings.
- Uninstalling the app removes all stored data from the device.

## Matrix and Synapse

This app talks to Matrix Synapse servers using the [Matrix Client-Server API](https://spec.matrix.org/v1.10/client-server-api/) and Synapse Admin API. The privacy and security of data on the Synapse server itself are governed by the server operator and [Matrix.org](https://matrix.org) policies, not by this app.

## Contact

For questions about this privacy policy or the app, open an issue or discussion in the [project repository](https://github.com/sureserverman/matrix-synapse-manager-android).
