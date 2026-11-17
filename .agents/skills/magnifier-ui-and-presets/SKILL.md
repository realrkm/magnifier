---
name: magnifier-ui-and-presets
description: >-
  UI design system, Compose conventions, haptic feedback, and UseCasesDialog
  preset guidelines for the Magnifier Android application. Use when modifying
  MagnifierScreen, MagnifierViewModel, Theme, or README use cases.
---

# Magnifier UI & Use-Case Presets

## Visual Design System
- **Theme (`ui/theme/Theme.kt`)**: Dark-mode-first optical instrument palette:
  - `LoupeCyan` (`#00E5FF`) for primary actions, active toggles, focus reticles, and zoom readouts.
  - `LoupeAmber` (`#FFB300`) for illumination (torch), `AF LOCK`, frozen state, and `5×+ AI` badges.
  - `GlassBg` (`#CC0D1117`) and `GlassBorder` (`Color.White.copy(alpha = 0.14f)`) for floating HUD pills and the bottom control deck.
- **Tactile Feedback**: Trigger `LocalHapticFeedback.current.performHapticFeedback(...)` (`TextHandleMove` for chips/toggles, `LongPress` for shutter, freeze, AF lock, and preset application).
- **Zoom Slider**: Uses logarithmic mapping in `ZoomControlRow200x` so `1×–10×` retains fine macro travel while reaching `200×`.

## Adding or Updating Use-Case Presets
1. Add or update the entry in `UseCaseGuideItems` in `MagnifierScreen.kt` with `title`, `description`, `bestTools`, `presetFilter`, `presetTorch`, `presetZoom`, `presetGrid`, and `presetPeaking`.
2. Ensure `MagnifierViewModel.applyUseCasePreset(...)` supports any new preset flags.
3. Keep the **Ideal Use Cases & Recommended Tools** table in `README.md` synchronized with `UseCaseGuideItems`.
