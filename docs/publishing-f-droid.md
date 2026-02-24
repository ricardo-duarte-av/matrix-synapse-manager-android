# Publishing Matrix Synapse Manager to F-Droid

This guide explains how to get the app included in the official [F-Droid](https://f-droid.org/) repository so users can install and update it from F-Droid.

## Prerequisites

1. **Public source code**  
   The app must be in a **public** Git repository (e.g. GitHub, GitLab, Codeberg). F-Droid builds from source; they do not accept pre-built APKs for the main repo.

2. **Repository URL**  
   The F-Droid metadata and README in this repo point to  
   `https://github.com/sureserverman/matrix-synapse-manager-android`.  
   If you move the repo, update [docs/f-droid/com.matrix.synapse.manager.yml](f-droid/com.matrix.synapse.manager.yml) and the clone URL in [README.md](../README.md).

3. **Tagged release**  
   F-Droid uses **tags** for version/update checks. Create a tag for the version you want to publish (e.g. `v1.0.0` matching `versionName` / `versionCode` in the metadata):

   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

   For future releases, tag each release (e.g. `v1.1.0`) and push the tag; with `UpdateCheckMode: Tags` and `AutoUpdateMode: Version`, F-Droid can add new builds automatically.

## How to submit

F-Droid app data lives in the [fdroiddata](https://gitlab.com/fdroid/fdroiddata) repository. You can either **request packaging** (simplest for first-time submitters) or **add the metadata yourself** via a merge request.

### Option A: Request for Packaging (recommended for first time)

1. Open the **RFP (Request for Packaging)** issue tracker:  
   <https://gitlab.com/fdroid/rfp/-/issues>
2. Click **New issue** and use the **Request for Packaging** template.
3. Fill in the template:
   - **App name:** Matrix Synapse Manager
   - **Package name:** com.matrix.synapse.manager
   - **Source code URL:** https://github.com/sureserverman/matrix-synapse-manager-android
   - **License:** Apache-2.0
   - **Short description:** e.g. “Admin panel for Matrix Synapse homeservers. Manage users, rooms, media, federation and server health.”
4. Optionally attach or link to the metadata YAML from [docs/f-droid/com.matrix.synapse.manager.yml](f-droid/com.matrix.synapse.manager.yml) so packagers can use it as a starting point.

A volunteer will create the metadata in fdroiddata and get the app building. This can take some time; you can also do Option B if you prefer to contribute the metadata yourself.

### Option B: Add the app via Merge Request

1. **Register** on [GitLab](https://gitlab.com/) and **fork** [fdroiddata](https://gitlab.com/fdroid/fdroiddata).
2. In your fork, create a **new branch** (e.g. `com.matrix.synapse.manager`).
3. In the **metadata** directory, add a file named **com.matrix.synapse.manager.yml**.
4. Copy the content from [docs/f-droid/com.matrix.synapse.manager.yml](f-droid/com.matrix.synapse.manager.yml) and remove the comment lines at the top (the “Copy this file…” and “See docs…” lines).
5. Commit, push, and open a **Merge Request** against `fdroid/fdroiddata`.
6. In the MR, use the project’s MR template. The CI pipeline will run; fix any reported metadata or build issues.
7. After the MR is merged, F-Droid will build the app from source and publish it. See the [F-Droid wiki FAQ](https://gitlab.com/fdroid/wiki/-/wikis/FAQ#how-long-does-it-take-for-my-app-to-show-up-on-website-and-client) for typical delays.

## Build and compliance

- F-Droid **builds the app from source** on their infrastructure and **signs the APK** with their key. You do not upload a signed APK for the main repo.
- The app must comply with the [F-Droid inclusion policy](https://f-droid.org/docs/Inclusion_Policy/): FLOSS license (Apache-2.0 is fine), no proprietary tracking/ad SDKs, buildable with a free toolchain. This project uses only FLOSS dependencies and no Google Play Services or Firebase.
- If you use **fdroidserver** locally, you can verify the recipe:

  ```bash
  fdroid readmeta
  fdroid lint com.matrix.synapse.manager
  fdroid build -v -l com.matrix.synapse.manager
  ```

## After inclusion

- **Updates:** When you release a new version, create a new tag (e.g. `v1.1.0`) and push it. With `UpdateCheckMode: Tags` and `AutoUpdateMode: Version`, F-Droid can pick up new versions automatically.
- **Badge:** You can add an F-Droid badge to your README; see [F-Droid Badges](https://f-droid.org/docs/Badges/). Example (replace `APP.ID` if different):  
  `https://img.shields.io/f-droid/v/com.matrix.synapse.manager.svg?logo=F-Droid`

## Summary checklist

- [ ] App is in a **public** Git repository (https://github.com/sureserverman/matrix-synapse-manager-android)
- [ ] At least one **release tag** (e.g. `v1.0.0`) pushed
- [ ] Either an **RFP issue** opened or a **Merge Request** to fdroiddata with the metadata file
