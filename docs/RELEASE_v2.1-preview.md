# Overthink 2.1 preview

Find real research sources, organize your library, and read original PDFs in a customizable Android workspace.

## Which file should I install?

**Most testers:** `Overthink-2.1-preview.apk` (1,444,519 bytes). This is the optimized Android APK.

**Developers:** `Overthink-2.1-debug.apk` (10,930,746 bytes). This larger APK retains development support.

Both files install the same app and are signed with Android's standard debug key. They are for preview/testing distribution, not a production or Play Store release. Accounts, sync, and AI require the separately hosted backend; AI also requires a configured server credential and model.

## Highlights

- Search research papers through OpenAlex, Crossref, and arXiv, datasets through DataCite, and code through GitHub.
- Save sources with collections, tags, favorites, notes, and reading status.
- Read and share original PDFs, with page navigation, zoom, sepia/night rendering, focus mode, and offline caching.
- Export sources as BibTeX, RIS, or Markdown; back up the workspace as JSON.
- Personalize 34 saved settings across appearance, motion, discovery, library, and reading.
- Use the optional Node/SQLite service for accounts, revision-aware sync, and abstract-based AI when configured.

## Verification

The final local check passed Android debug and release builds, 22 Android tests, 9 backend integration tests, and both lint variants with zero errors. The optimized APK installed and opened on the project emulator; saved library data and a real five-page PDF rendered with reader controls visible. More devices and a production signing/hosting pass are still needed.

## Checksums

| File | SHA-256 |
| --- | --- |
| `Overthink-2.1-preview.apk` | `9b2a62e6e6de74d2d73053a12691e3bde6dd58aadd9843a701aa39785097cc3d` |
| `Overthink-2.1-debug.apk` | `5526838b0485370578574554a1206a61f32611ca54a6c30a3bd9843bf43f6aa7` |

See the [README](../README.md) for screenshots, installation steps, developer setup, and current limits.
