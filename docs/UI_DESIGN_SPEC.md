# Matrix Synapse Manager — UI Design Spec

## Current UI summary (before improvements)

- **Stack**: Jetpack Compose, Material 3 components, single-Activity with type-safe navigation.
- **Screens**: Server setup → Login → User list (hub), User detail/edit, Device list, Whois, Rooms (list/detail), Server dashboard (stats), Media (list/detail), Federation (list/detail), Audit log, App lock settings.
- **Theme**: XML theme was `Theme.Material.Light.NoActionBar`; Compose had no custom theme (default M3). No dark theme, no app-specific colors or type scale.
- **Pain points**: Bare forms (Add Server, Login) with no Scaffold/TopAppBar; User list top bar overcrowded with 6 text buttons; inconsistent spacing (8dp vs 16dp); no design tokens or shared spacing scale.

---

## Design direction

- **Material 3** with a **Matrix-inspired** palette (teal/green primary, clear hierarchy).
- **Light and dark** schemes, system-based by default.
- **Consistent spacing**: 8, 12, 16, 24 dp; screen padding 24 dp; section spacing 24 dp.
- **Clear hierarchy**: TopAppBar on every screen; primary actions prominent; secondary actions in overflow where appropriate.
- **Typography**: M3 type scale (Theme.kt Typography); use titleLarge for screen titles, bodyMedium for descriptions.

---

## Implemented changes

### 1. Theming (app module)

- **Color.kt**: Full M3 light and dark color sets. Primary teal (#006B54 light / #5DDDB9 dark), semantic roles for surface, error, outline, etc.
- **Type.kt**: M3 type scale (displayLarge through labelSmall) with default font family.
- **Theme.kt**: `MatrixSynapseManagerTheme(darkTheme, content)` — applies `lightColorScheme` / `darkColorScheme`, `Typography`, and status bar appearance (light/dark).
- **MainActivity**: Wraps content in `MatrixSynapseManagerTheme { ... }`.
- **themes.xml**: Transparent status/nav bar for edge-to-edge; window background from system.

### 2. Screen layout and spacing

- **ServerFormScreen**: Scaffold + TopAppBar (“Add Server”), 24 dp horizontal padding, short body copy, 16 dp between fields, 24 dp before primary button. Scrollable column.
- **LoginScreen**: Scaffold + TopAppBar (“Admin Login”), same padding and spacing scale; server URL as body text; scrollable.
- **UserListScreen**: TopAppBar title “Users” with `titleLarge`. Replaced 6 TextButtons with a single overflow (⋮) IconButton and `DropdownMenu` with DropdownMenuItems: Media, Federation, Rooms, Stats, Audit log, Settings. Search field padding 24 dp horizontal, 12 dp vertical.

### 3. Values to reuse elsewhere

| Token        | Value   |
|-------------|---------|
| Screen padding | 24 dp |
| Field spacing  | 16 dp |
| Section spacing| 24 dp |
| Primary (light) | #006B54 |
| Primary (dark)  | #5DDDB9 |

---

## Suggested next steps (not yet done)

- Apply the same **Scaffold + TopAppBar + 24 dp padding** pattern to remaining screens (User detail, Room list/detail, Dashboard, Media, Federation, Audit log, App lock settings).
- Use **MaterialTheme.typography.titleLarge** for screen titles and **bodyMedium** / **onSurfaceVariant** for secondary text.
- Consider **FAB or prominent CTA** on User list if “Add user” becomes a primary action.
- Optional: **Shape** tokens (e.g. 12 dp rounded corners for cards) in Theme.kt for consistency.

---

## Feedback

If you want to change colors, spacing, or add more screens to this spec, we can update the theme and screens accordingly. Figma mockups can be aligned to this spec (and vice versa) using the same tokens and structure.
