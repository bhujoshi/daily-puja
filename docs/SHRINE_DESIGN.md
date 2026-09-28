# Shrine screen design

## 2026-09-28 — Reduce visual clutter

- Removed the gold center stripe from Android curtains. The panels meet with matching fabric color; softer folds and color-matched outer shadows retain depth for red and saffron curtains.
- Removed the standalone login/streak and Hindi/English buttons from the top of the shrine screen. The options button remains the entry point for profile, login/logout, language, customization, package and music controls.
- Kept the mini music player visible during aarti and preserved the ritual controls and curtain animation.

## 2026-09-28 — Harmonize curtains and tilak motion

- Replaced the dark red curtain with warm ivory, matching the cream ritual panels. The alternate curtain uses muted champagne, with brass-tinted shadows and soft highlights instead of black folds. Both panels still meet without a center stripe.
- Reversed the tilak reveal to draw from the lower forehead upward over 900 ms, preserving the completed mark and dot.

## 2026-09-28 — Redesign the first impression

- Replaced the flat stripe layout with a dedicated native `TempleCurtains` component: six curved silk folds per panel, broad reflected light, and a scalloped top drape with fine curved stitching. Removed the decorative bottom hem lines after visual feedback; the rest of the curtain design is unchanged.
- Used warm champagne silk by default and deeper muted saffron as the alternate, with coordinated highlights and shadows.
- Added a small outlined lotus seal to the welcome state. It fades during the first fifth of the opening animation, while both curtain panels and their drapes slide outward together. There is no center trim.
- Fabric is drawn procedurally at the device resolution, without image downloads or additional assets.

## 2026-09-28 — Pour abhishek from the top front

- Centered the copper lota above the selected deity's crown and changed its tilt from sideways to front/back. Following visual feedback, reversed the tilt so the mouth faces away from the viewer toward the deity.
- Aligned the water stream and moving droplets vertically from the top to the crown. Runoff and settling ripples remain in place.
- The pour uses each idol's crown landmark, including paired idols and all altar variants.
- Closed the lota mesh's open underside with a copper base, so it remains solid when tilted toward the deity.

## Owner package access

The backend grants the temple customization package to `bhuwanchandra.it@gmail.com` after verified Google sign-in. The grant is persisted against the Google account subject and recorded once as `package_granted` in account history. It does not require a demo purchase or devotional streak. Other accounts follow the existing unlock rules; supplying this email to password registration does not grant access.

The local account store had no matching account at implementation time. Restart the backend with this change and sign in through the options menu with the specified Google account to activate the package. Existing signed-in sessions must sign out and sign in again to trigger the grant. Deployment is required for a remote backend.
