# Publishing Matrix Synapse Manager to Google Play

This guide explains **step by step** how to publish the app on Google Play so users can find and install it from the Play Store.

---

## What you will need

- A **Google account**
- A **one-time payment** (about $25 USD) to register as a developer
- About **30–60 minutes** to complete the first setup (store text, images, forms)
- For **new developer accounts** (created after November 2023): **12 or more testers** who install the app from a “closed test” for **14 days** before you can publish to everyone (Google’s rule)

---

## Words used in this guide

- **Play Console** — The website where you manage your app (create it, add descriptions, upload the app file, set countries, etc.). Link: [play.google.com/console](https://play.google.com/console).
- **AAB (Android App Bundle)** — The file format Google Play requires for uploads. It’s not an APK; it’s a “bundle” that Google turns into APKs for different devices. You build it with `./gradlew :app:bundleRelease`.
- **Keystore** — A secure file (and passwords) that prove future updates are from you. If you lose it, you cannot update the same app on Play anymore. **Back it up safely.**
- **Upload key** — The key you use to sign the AAB before uploading. Google may then re-sign with their own key for users; you only need to keep and use your upload key.
- **Store listing** — The name, description, icon, and screenshots that users see on the app’s Play Store page.
- **Track** — Where you upload a release: **Internal** (only a few testers), **Closed** (list of testers you choose), or **Production** (everyone in the countries you select).

---

## Step 1: Create a developer account (one-time)

1. Open **[Play Console](https://play.google.com/console)** in your browser and sign in with your Google account.
2. Read and accept the **Developer Distribution Agreement**.
3. Pay the **one-time registration fee** (about $25 USD, depending on your country). This is required; there is no way to publish without it.
4. Complete any **account details** Google asks for (developer name, email, sometimes identity verification).
5. **If your account was created after mid-November 2023:** Google will ask you to run a **closed test** with **at least 12 testers** for **14 days in a row** before you can publish to Production. Plan for that: you will upload your app to “Closed testing” first, add 12+ testers, wait 14 days, then you’ll be allowed to publish to “Production.”

---

## Step 2: Create the app in Play Console

1. In Play Console, on the main page, click **“Create app”**.
2. You’ll see a form. Fill it in like this:
   - **App name:** `Matrix Synapse Manager`
   - **Default language:** Choose the main language for your store listing (e.g. English).
   - **App or game:** Select **App**.
   - **Free or paid:** Select **Free**.
3. Check any boxes Google shows (e.g. that you comply with export rules, that your app doesn’t contain misleading content). Then click **“Create app.”**

After this, you’ll see your app’s dashboard. The left sidebar has sections like “Release,” “Grow,” “Monetize,” “App content,” etc. We’ll use those in the next steps.

---

## Step 3: Sign your app so Google can accept it

Google only accepts apps that are **signed** with a key. You create one key (keystore) and use it every time you upload a new version.

### Step 3.1: Create your upload keystore (one-time, on your computer)

Run this in a terminal, in any folder (for example your home folder or the project root). You will be asked for a password and your name; **remember the password** — you’ll need it for every release.

```bash
keytool -genkey -v -keystore upload.keystore -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

- **What this does:** Creates a file called `upload.keystore` containing a key named `upload`. This is your **upload key**.
- **Important:** Copy `upload.keystore` to a safe place (e.g. backup drive or password manager). If you lose this file and its password, you cannot publish updates to the same app on Play anymore.

You can move `upload.keystore` into your project root folder (next to `build.gradle.kts`) so the next step is easy.

### Step 3.2: Tell the project how to use the keystore

1. In your **project root** (same folder as `build.gradle.kts` and `settings.gradle.kts`), copy the example file:

   ```bash
   cp keystore.properties.example keystore.properties
   ```

2. Open **`keystore.properties`** in a text editor. It will look like this:

   ```properties
   storeFile=upload.keystore
   storePassword=your_store_password
   keyAlias=upload
   keyPassword=your_key_password
   ```

3. Replace the placeholders with your real values:
   - **storeFile** — Either `upload.keystore` if the file is in the project root, or the path to it (e.g. `../upload.keystore` if it’s one folder up).
   - **storePassword** — The password you chose when creating the keystore (for the whole file).
   - **keyAlias** — Must be `upload` (same as in the `keytool` command).
   - **keyPassword** — The password for the key (often the same as storePassword).

4. Save the file. **Do not commit `keystore.properties` to Git** — it’s in `.gitignore` because it contains secrets.

5. Build the signed release bundle:

   ```bash
   ./gradlew :app:bundleRelease
   ```

   When it finishes, the file you need for Google Play is here:

   **`app/build/outputs/bundle/release/app-release.aab`**

   That’s the file you will upload in Play Console.

### Step 3.3: First upload and “Play App Signing”

The **first time** you upload an AAB for this app, Play Console will show a screen about **Play App Signing**.

- **What it means:** Google can manage a separate “app signing key” for users. You keep using your **upload key** to sign the AAB; Google may re-sign with their key for distribution. This is the recommended setup.
- **What to do:** Choose the option that lets **Google manage the app signing key** (usually “Continue” or “Use Google’s key”). You will **not** need to send your upload key to Google in most cases — uploading an AAB already signed with your upload key is enough. Follow the on-screen steps until the upload finishes.

From then on, always sign your AAB with the **same** upload keystore and alias so Play can recognize your updates.

---

## Step 4: Fill in the store listing (what users see on the Play Store)

In Play Console, open the left sidebar → **Grow** → **Store presence** → **Main store listing**. (If you add more languages later, you’ll have one “store listing” per language.)

Fill in each field clearly:

| Field | What to enter |
|-------|----------------|
| **App name** | `Matrix Synapse Manager` (max 30 characters). |
| **Short description** | One line, max 80 characters. Example: “Admin panel for Matrix Synapse servers. Manage users, rooms, media and more.” |
| **Full description** | Longer text, max 4000 characters. You can copy the “Features” and “Architecture” sections from the project [README](../README.md), or write a simpler explanation of what the app does and for whom. |
| **App icon** | One image, **512×512 pixels**, PNG, no transparency. This is the icon shown on the store. You can resize your app icon from `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png` to 512×512, or export a 512×512 version from your design tool. |
| **Feature graphic** | One image, **1024×500 pixels**, PNG or JPEG. This is the banner at the top of your app’s store page. Required. You can use a simple graphic with the app name and a short tagline. |
| **Screenshots** | At least **2 screenshots** of the app on a phone (e.g. login screen, server list, user list). More is better. You can add tablet screenshots later; they’re optional. |

Save the page when you’re done.

---

## Step 5: Privacy policy and Data safety

Google requires a **privacy policy** for apps that handle user data. Your app stores access tokens and server URLs on the device, so you must provide a URL to a page that explains that.

- **Privacy policy:** The policy is maintained in the repo as **[docs/privacy.md](privacy.md)**. It describes what data the app stores (access tokens in Android Keystore, server URLs, optional hashed PIN, local audit log), that passwords are not stored, and that data stays on the device and is only sent to the Synapse server the user configures.
- **URL for Play Console:** Google needs a **public URL**. You can use either:
  - **GitHub:** If the repo is on GitHub, use the URL to the rendered file, e.g. `https://github.com/sureserverman/matrix-synapse-manager-android/blob/main/docs/privacy.md` (GitHub renders markdown). Replace `sureserverman/matrix-synapse-manager-android` with your actual repo if different.
  - **GitHub Pages:** If you use GitHub Pages, you can serve the same content at a shorter URL (e.g. `https://yoursite.github.io/matrix-synapse-manager-android/privacy`).
- In Play Console: go to **App content** (left sidebar) and find the **Privacy policy** field. Paste the **URL** you chose above.

**Data safety** is a form in the same “App content” area where you declare what data you collect and share:

- For this app you can typically say: **No** data shared with third parties; **No** data collected for ads or analytics.
- Data stored on device (tokens, server URLs) for **app functionality** — you can select “Credentials” or “App info” and say it’s stored on device and not shared.

Answer all required questions and save.

---

## Step 6: Content rating

Google requires a **content rating** (e.g. Everyone, Teen) so users and parents can see what to expect.

1. In the left sidebar, open **App content** and find **Content rating**.
2. Click to start the **questionnaire**. Answer honestly:
   - For an admin/technical app: usually **no** violence, gambling, user-generated content, etc.
   - You’ll get a rating at the end (e.g. “Everyone”).
3. Submit the questionnaire. Once it’s done, the “Content rating” item in App content will show as complete.

---

## Step 7: Other “App content” items

In **App content**, Google may show more items. Fill them as needed:

- **Ads:** If your app has **no ads**, select “No, my app does not contain ads.”
- **Target audience:** Choose the age group (e.g. 18+ or “All ages” for an admin tool).
- **News app, COVID-19, etc.:** Answer “No” or “Not applicable” unless they clearly apply.

---

## Step 8: Choose a release track and upload your AAB

You can upload your first build to **Internal testing**, then **Closed testing**, then **Production**. For a **new personal developer account**, you may have to use **Closed testing** with **12+ testers for 14 days** before Production is unlocked.

- **Internal testing** — Only people you add (e.g. by email). Good to check that the AAB installs and runs. Optional.
- **Closed testing** — You add a list of testers (email or group). For new accounts, Google may require **at least 12 testers** for **14 consecutive days** before you can go to Production.
- **Production** — The app goes live for everyone in the countries you select.

**To upload:**

1. In the left sidebar, open **Release** → **Testing** → **Internal testing** (or **Closed testing** or **Production**).
2. Click **“Create new release.”**
3. Upload the file **`app/build/outputs/bundle/release/app-release.aab`** (drag and drop or “Upload”).
4. Add **Release notes** (e.g. “Initial release” or “1.0.0 – first version”).
5. Click **Save**, then **Review release**, then **Start rollout to …** (or “Save” and use **Managed publishing** if you want to control the exact time it goes live).

If you’re using Closed testing first: add at least 12 testers (email list or Google Group), share the testing link with them, and wait 14 days before requesting Production access if Google asks for it.

---

## Step 9: Checklist before you publish

Use this list to avoid missing something:

- [ ] Developer account created and fee paid.
- [ ] App created in Play Console (Step 2).
- [ ] Upload keystore created and **backed up**; `keystore.properties` filled in; `./gradlew :app:bundleRelease` produces `app-release.aab`.
- [ ] First AAB uploaded and Play App Signing set up (Step 3.3).
- [ ] Store listing complete: name, short and full description, 512×512 icon, 1024×500 feature graphic, at least 2 screenshots (Step 4).
- [ ] Privacy policy URL set; Data safety and Content rating completed (Steps 5 and 6).
- [ ] Release created on the chosen track (Internal / Closed / Production), AAB attached, release notes added, rollout started (Step 8).
- [ ] If your account is new: closed test with 12+ testers for 14 days done before going to Production (if required by Google).

---

## Step 10: How to publish updates later

When you fix bugs or add features:

1. **Bump the version** in `app/build.gradle.kts`: increase `versionCode` (e.g. from 1 to 2) and set `versionName` (e.g. `"1.0.1"`).
2. Build a new AAB: run `./gradlew :app:bundleRelease` again.
3. In Play Console, open **Release** → the track you want (e.g. **Production**) → **Create new release** → upload the new **app-release.aab** → add release notes → **Save** → **Review release** → **Start rollout**.

Google will review the update; once approved, users will get it via the Play Store.

---

## Useful links

- [Play Console](https://play.google.com/console)
- [Official help: Publish your app](https://support.google.com/googleplay/android-developer/answer/9859751)
- [Play Console requirements](https://support.google.com/googleplay/android-developer/answer/10788890)
- [Testing requirements for new personal accounts](https://support.google.com/googleplay/android-developer/answer/14151465) (14-day closed test, 12+ testers)
