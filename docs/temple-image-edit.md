# Temple background refinement

Tool: built-in ImageGen, precise-object-edit mode.

Input: `android/app/src/main/res/drawable/temple_photo.png`.
Output: `android/app/src/main/res/drawable/temple_clean.png`.

## Final prompt

Use case: precise-object-edit. Edit target: supplied square Indian home mandir photograph, used as an Android app background. Preserve exact camera framing, carved wooden temple, Om backdrop, wall decorations, and Ganesha and Lakshmi statues in their original positions and sizes. Remove all loose flowers and flower garlands at the statues' bases, all standing and small diya lamps on the altar, and the little bell on the lower wooden ledge. Restore immaculate clean natural wooden surfaces; leave empty space between the deities for an interactive oil lamp. Remove the foreground offering tray at lower right and loose floor lamps/vessels on both sides, leaving a clean foreground for interactive 3D objects. Improve the carpet to a premium richly detailed woven Indian rug, perfectly centered symmetrically on the mandir centerline, straight and evenly placed, fine burgundy and muted gold botanical pattern, realistic fibers and crisp quality. Keep original square composition and all architectural proportions so overlay positions remain valid. Do not add any new lamps, plates, flowers, bells or text. Photorealistic, refined warm natural light.

## Runtime objects

The supplied GLB files remain unmodified. The app copies them into its model assets. The oil flame is a separate procedural emissive mesh, hidden until the user lights the lamp. The scene uses a fixed square orthographic projection, with model positions aligned to the photograph. Deity flower offerings have a separate lifecycle from permanent objects.

## September 16 Android update

Built-in ImageGen edited the portrait background now saved in
`android/app/src/main/res/drawable-nodpi/temple_portrait.png`.
Prompt: remove the upper solid wooden crown as in the approved image, enlarge both
statues to 150% while preserving identities, shrine and portrait framing; retain a
clean shelf and foreground for interactive objects. A correction requested raising
the enlarged statues onto the altar shelf. Generated placement was visually checked
and water, tilak, touch and flower coordinates aligned to the final image.

The aarti diya uses the user's upright reference as a transparent image asset.
Final extraction prompt: remove the black background to genuine alpha, preserve the
brass bowl, slender ornate stem and circular foot; unlit small cotton wick, no fire,
no external shadow, centered with 5% padding. Built-in ImageGen was used.
The runtime overlays warm, layered flickering flames and keeps aarti lit after returning.
Both sunflower and peony GLBs are distributed across the plate and deity offerings.

Original offline instrumental music, water and offering chimes are reproducible with
`android/scripts/generate_ritual_audio.py`. Existing bell/conch recordings are reused.
Audio pauses when the app backgrounds or the temple closes. These are synthesized
instrumental cues, not sung aarti recordings; the existing Hindi recitation is retained.
