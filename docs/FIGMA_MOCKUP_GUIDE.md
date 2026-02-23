# Figma mockup guide — Matrix Synapse Manager

Step-by-step instructions to recreate the app screens in Figma using the design spec and reference images.

---

## 1. Setup (do once)

### Frame
- Create a **Frame** (F): **360 × 780** (phone portrait). Name it e.g. `Android – 360`.
- Optional: use a device frame from Community (e.g. “Android Frame”) or keep a simple rectangle.

### Color styles (Variables or Local styles)
Create these so you can reuse and switch to dark later:

| Name | Hex | Use |
|------|-----|-----|
| **Surface** | `#F6FBF9` | Screen background |
| **On Surface** | `#191C1B` | Primary text |
| **On Surface Variant** | `#3F4947` | Secondary text |
| **Primary** | `#006B54` | Buttons, key actions |
| **On Primary** | `#FFFFFF` | Text on primary buttons |
| **Outline** | `#6F7977` | Borders, outlined fields |

### Text styles
- **Title / Screen title**: 22px, Medium (500), On Surface. Use for “Add Server”, “Admin Login”, “Users”.
- **Body**: 14px, Regular, On Surface Variant. Use for descriptions and secondary text.
- **Label**: 12px, Medium, On Surface Variant. Use for field labels if needed.

### Spacing (remember these)
- **Screen padding**: 24px left/right (and top below app bar).
- **Between sections**: 24px.
- **Between fields**: 16px.

---

## 2. Screen 1 — Add Server

Reference image: `assets/matrix-synapse-manager-add-server-mockup.png`

1. **Background**  
   Fill frame with **Surface** (#F6FBF9).

2. **Top app bar**  
   - Rectangle: full width × 64px, top. Fill **Surface** (or white #FFFFFF for contrast).  
   - Text: **“Add Server”**, Screen title style, left padding 24px, vertically centered in the bar.

3. **Body copy**  
   - Text: *“Connect to a Synapse homeserver to manage users, rooms, and media.”*  
   - Body style, On Surface Variant.  
   - Position: 24px from left/right, 24px below app bar.

4. **Spacer**  
   24px gap.

5. **Server URL field**  
   - Rectangle with 1px stroke **Outline**, corner radius 4px (or 8px). Height ~56px, full width minus 48px (24px × 2).  
   - Inside: label “Server URL” (Label style, above or floating), placeholder “e.g. matrix.example.com” (Body, lighter).  
   - 24px horizontal padding from frame.

6. **16px gap.**

7. **Display name field**  
   - Same as above. Label: “Display name (optional)”.

8. **24px gap.**

9. **Button**  
   - Full-width rectangle (width frame − 48px, centered), height 40px, corner radius 20px (pill) or 8px.  
   - Fill **Primary** (#006B54), text **“Add Server”** in **On Primary**, 16px or Medium.  
   - 24px from sides, 24px below last field.

10. **Optional**  
    Place the reference PNG on an adjacent frame or hidden layer to compare.

---

## 3. Screen 2 — Admin Login

Reference image: `assets/matrix-synapse-manager-login-mockup.png`

1. **Background**  
   Surface (#F6FBF9).

2. **Top app bar**  
   Same as Add Server: 64px height, title **“Admin Login”**.

3. **Server URL**  
   Body text showing the server (e.g. “https://matrix.example.com”) in On Surface Variant, 24px padding below app bar.

4. **24px gap.**

5. **Username field**  
   Outlined rectangle, label “Username”, same style as Add Server fields.

6. **16px gap.**

7. **Password field**  
   Same, label “Password” (optionally show dots for password).

8. **24px gap.**

9. **Button**  
   Full-width **Primary** button, text **“Sign In”**.

Use the same spacing (24px screen padding, 16px between fields, 24px before button).

---

## 4. Screen 3 — Users list

Reference image: `assets/matrix-synapse-manager-users-list-mockup.png`

1. **Background**  
   Surface (#F6FBF9).

2. **Top app bar**  
   - Same height 64px.  
   - Left: title **“Users”** (Screen title style).  
   - Right: **overflow icon** (three vertical dots, ⋮). Use an icon from Figma (e.g. “more vert”) or a simple 3-dot shape, On Surface, ~24×24px, 16px from right edge.

3. **Search field**  
   - 24px horizontal padding, 12px below app bar.  
   - Outlined field, label “Search users”, full width minus 48px.

4. **List**  
   - Rows: each row has a main line (e.g. `@alice:example.com`) in Body/On Surface and optional supporting line (e.g. “Alice”) in smaller/secondary style.  
   - Thin divider (1px, Outline or light gray) between rows.  
   - Row height ~72px, tap area comfortable.  
   - Example rows:
     - `@alice:example.com` / Alice  
     - `@bob:example.com` / Bob  
     - `@charlie:example.com` / Charlie  

5. **Overflow menu (optional)**  
   If you want to show the open state: a small floating card below the ⋮ icon with items “Media”, “Federation”, “Rooms”, “Stats”, “Audit log”, “Settings” (Body style, one per row).

---

## 5. Checklist

- [ ] Frame 360×780, colors and text styles set up  
- [ ] Add Server: app bar, body copy, 2 fields, primary button  
- [ ] Admin Login: app bar, server URL, 2 fields, Sign In button  
- [ ] Users: app bar + overflow icon, search, list rows + dividers  
- [ ] All screens: 24px horizontal padding, 16px between fields, 24px section gaps  
- [ ] Primary actions use #006B54, text on Surface/On Surface Variant  

---

## 6. Optional: dark theme

Duplicate your frames and apply dark tokens:

| Name | Hex (dark) |
|------|------------|
| Surface | `#191C1B` |
| On Surface | `#E0E3E1` |
| On Surface Variant | `#BEC9C6` |
| Primary | `#5DDDB9` |
| On Primary | `#00382C` |

Use the same layout; only swap the color styles.

---

Reference images are in **`assets/`**:
- `matrix-synapse-manager-add-server-mockup.png`
- `matrix-synapse-manager-login-mockup.png`
- `matrix-synapse-manager-users-list-mockup.png`

You can drag them into Figma as references or keep them open in another window while you build the frames.
