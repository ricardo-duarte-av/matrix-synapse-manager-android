# Audit log tabs & bulk delete/removal – UX proposals

## 1. How “tabs” work elsewhere in this app

- **Main navigation**: Bottom bar with **icon + label** (Users, Rooms, Stats, Settings, More). No horizontal text tabs.
- **More screen**: Vertical **list of rows** – each row = icon (leading) + label. Tap a row to open that section. No horizontal tab strip.
- **List filters**: Users and Rooms use a **dropdown** for “Sort by” – one row showing current choice (e.g. “Name (A→Z)”), tap to open a menu of options. Same padding and style as the rest of the screen.

So “tabs” in this app are either **bottom nav**, **list rows**, or **dropdowns** – not a horizontal strip of text labels in the middle of the screen.

---

## 2. Audit log: match other screens

**Current (problem):** Horizontal `ScrollableTabRow` with text: “All | Users | Rooms | Media | Federation” – feels different from the rest of the app.

**Proposed: filter dropdown (same pattern as Sort on Users/Rooms)**

- Below the top bar and “Export JSON”, add **one row**: label “Filter” + current value (e.g. “All”) + chevron down.
- Tap opens a **dropdown menu**: All, Users, Rooms, Media, Federation.
- Same layout as “Sort by” on User list and Room list (padding, typography, `DropdownMenu`).
- Result: Audit log filter uses the **same control type** as other list screens – no horizontal tab strip.

**Alternative:** A vertical list of filter options (like More screen) with icon + label. Uses more vertical space; dropdown is more compact and consistent with Sort.

**Recommendation:** Filter **dropdown** for Audit log.

---

## 3. Bulk delete/removal: the “press Delete to see another Delete” problem

**Current flow:**

1. User selects items → top bar shows **“Delete”** and **“Cancel”**.
2. User taps **“Delete”** → dialog opens: “Delete rooms?” with **“Delete”**, **“Delete with media”**, **“Cancel”**.

So the bar says “Delete” but that only opens a dialog; the **real** choice (delete vs delete with media) is inside the dialog. That feels like “press Delete to see another Delete” and is confusing.

**Best practices (short):**

- One tap should map to one clear intent: either “I’m choosing what to do” or “I’m confirming this exact action.”
- Avoid two identical-looking “Delete” labels for two different steps (open dialog vs confirm).
- Make scope clear (e.g. “3 rooms”) and use clear, destructive wording.

---

## 4. Option A: One bar control → one dialog with two choices

- **Bar:** One action: e.g. **“Remove”** (or trash icon). Plus **“Cancel”** to exit selection.
- **Dialog:** Title e.g. “Remove 3 rooms?” Body: short explanation. Then **two** actions:
  - **“Delete”** – delete rooms only.
  - **“Delete with media”** – delete rooms and their media (styled as destructive).
  - **“Cancel”** – close dialog.
- **No** “Delete” in the bar; the bar means “I want to remove these, show me how.” The dialog is where the user picks **which** kind of delete.

**Pros:** Single tap to get to the choice; no double “Delete”.  
**Cons:** Bar label “Remove” might be slightly less direct than “Delete” (we can tune wording).

---

## 5. Option B: Two bar actions, each with its own confirmation

- **Bar:** Two actions: **“Delete”** and **“Delete with media”** (plus **“Cancel”**). Both visible (e.g. as text or icon+text).
- Tapping **“Delete”** → **one** confirmation dialog: “Delete 3 rooms? This cannot be undone.” → **[Cancel]** **[Delete]**.
- Tapping **“Delete with media”** → **one** confirmation dialog: “Delete 3 rooms and their media? This cannot be undone.” → **[Cancel]** **[Delete with media]**.
- So: bar = choose **which** action; dialog = confirm **that** action only. No second “Delete” that means something different.

**Pros:** Very clear: first tap = pick action, second = confirm. No wording duplication.  
**Cons:** Two buttons in the bar (can use overflow if we want fewer visible buttons).

---

## 6. Option C: Overflow menu for the action, then confirm

- **Bar:** **Overflow (⋮)** and **“Cancel”**.
- Overflow opens menu: **“Delete rooms”**, **“Delete rooms and media”**.
- Tapping a menu item opens **one** confirmation for that action (like in Option B).
- No “Delete” or “Remove” in the bar at all – only the menu and then the confirmation.

**Pros:** Keeps the bar minimal; matches “actions in menu” pattern.  
**Cons:** One extra tap (open menu, then pick), and we previously moved away from overflow for this flow.

---

## 7. Summary and what I need from you

1. **Audit log “tabs”**  
   - Proposed: **filter dropdown** (like Sort on Users/Rooms), with options: All, Users, Rooms, Media, Federation.  
   - Do you want that, or prefer a vertical list of filters (More-style) instead?

2. **Bulk delete/removal**  
   - Option **A**: Bar = “Remove” (or icon) + Cancel → one dialog with “Delete”, “Delete with media”, “Cancel”.  
   - Option **B**: Bar = “Delete” + “Delete with media” + Cancel → each opens a single confirmation for that action.  
   - Option **C**: Bar = overflow (⋮) + Cancel → menu “Delete rooms” / “Delete rooms and media” → then one confirmation each.  
   - Which option (or combination) do you want for **Rooms**? Same for **Users** (Deactivate / Delete with media)?

Once you pick:
- Audit log: dropdown vs list, and
- Delete flow: A, B, or C (and same for users),

I’ll implement that and not change the pattern on my own.
