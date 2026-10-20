# Overthink 2.1 rebuild status

Updated 20 October 2026. The core rebuild and customization pass are implemented and verified: a new native Android application and an application-owned backend replace the original prototype. The advanced and production boundaries below remain explicit.

## Implemented

| Area | New behavior |
| --- | --- |
| Build & cleanup | Removed 75 obsolete source/config files, AI Studio scaffolding, a redundant JDK download, unused preview dependency and full extended icon catalog. Retained 21 used vectors with licenses. Debug and optimized release builds, code/resource shrinking, and both variants in CI/local checks. |
| Design | Refreshed dashboard, five accent palettes, modern/editorial typography, system/light/dark themes, adjustable text size, spacing and corners, animated transitions/progress, reduced motion and haptics, adaptive source columns, tab form retention. |
| Customization | 34 validated, persistent device preferences, searchable settings hub, home section controls, launch destination, search/library defaults, source previews, exports and reading comfort. Reset preserves research and credentials. |
| Discovery | Real OpenAlex, Crossref, arXiv, DataCite and GitHub adapters; canonical DOI/arXiv lookup; provider, explicit optional year and direct-PDF filters; local result sorting; pagination; cancellation/reset protection; typed provider outcomes. Paper defaults never block dataset/code records. |
| Research integrity | No invented manuscript pages, benchmark tables, live browser content, fallback AI summaries, or completed downloads. Unknown metadata stays unknown. Sources open in the system browser. |
| Library | Collections, favorites, three reading statuses, editable tags and notes, source/status/favorite filters, sorting, PDF imports and filtered BibTeX/RIS/Markdown exports. Atomic export staging survives helper/activity recreation. Configurable removal confirmation. |
| Reader | Original PDFs, pinned controls in the activity viewport, page jump, bounded zoom/pan and double-tap, original/sepia/night rendering, focus mode, keep-awake and resume preferences, original-file sharing, offline cache, and page notes. Detail drafts persist while the reader is open. |
| Projects | Manual project creation, research question and deadline, linked sources, typed tasks with real status/progress. Both old task formats have a conversion path. |
| Persistence | Transactional SQLite workspace snapshots, distinct guest/account databases, validated JSON backup/merge, original device database preserved. Legacy paper import also carries linked notes/highlight quotes and non-default projects. |
| Accounts | Real backend password checking, salted scrypt hashes, hashed expiring/revocable sessions, rate limits, account ownership, password-confirmed deletion, encrypted Android session storage. No admin self-elevation. |
| Synchronization | Revision-based writes, conflict detection, explicit server-restore confirmation. Guest data is never uploaded implicitly. |
| AI | Server-held API credentials, limits, selected abstract context, source-ID checks, references, persisted per-source conversation, explicit errors when unavailable. |
| Operations | Node 24 service without npm dependencies, Dockerfile, real SQLite backup command, backend regression tests, deployment instructions. |

## Validation record

- Final `scripts/check.ps1 -Offline`: BUILD SUCCESSFUL for debug and release APKs, Android unit/UI tests, and both lint variants. **22 Android tests passed**, with no failures/skips. **9 backend integration tests passed**.
- Lint: **zero errors** in both variants; 28 debug and 14 release warnings remain for pinned dependency versions and optional Kotlin-extension usage. Required checks remain enabled.
- Tests cover preference migration/persistence/validation/reset, organization metadata and older backups, sorting, export escaping and recovery, history opt-out, search cancellation/reset, real year-filter semantics, tab form retention, large-text customization interactions, and reader control visibility, alongside the core account/data tests.
- Light Home, dark Discover, and customization at 130% text scale were rendered and inspected. Device screenshots are under `local-artifacts/screenshots`.
- Optimized installable preview: **1,444,519 bytes** (about 1.44 MB). Debug APK: 10,930,746 bytes, down from the prior 17,207,932 bytes. The optimized APK is signed with the standard Android **debug key for local preview only**; normal release output remains unsigned. It is not a production-signing configuration.
- Final optimized-preview emulator smoke checks confirmed startup, the existing saved library, details, real five-page PDF rendering, and the corrected toolbar entirely within the display. The previous preview also exercised favorite persistence. Additional device interactions for next-page/page-jump/Night rendering were not executed after the permission prompt declined that tool call; the customization interaction and toolbar layout have automated coverage.
- Release validation exposed OkHttp references to optional JVM TLS providers. Nine exact R8-generated rules were added after checking the version-specific platform selector; Android TLS verification remains enabled. The release variant is now part of routine CI/local checks.
- Baseline rebuild checks reached all five public providers (HTTP 200), exercised live search/save/PDF download on Android 36.1, and found no application crash in the inspected AndroidRuntime log. Availability is query/provider dependent; these checks are not a performance benchmark.

The emulator experienced a System UI startup delay during the first smoke pass. This was separate from the app; it was dismissed before testing. The test emulator was shut down after verification.

## Remaining boundaries

This is a runnable replacement of the core research workspace, not a claim that every proposed advanced capability is finished.

- Hosting, HTTPS, a production AI model/key and release signing must be configured for a connected production deployment. Real Gemini generation has not been tested with a live credential here; its contracts and failure paths use mocked integration tests.
- AI is abstract-only. PDF text extraction/search, text-coordinate highlighting, OCR and freehand tools are not implemented. The reader uses page notes and does not advertise simulated versions of those tools.
- Citation exports use metadata-based BibTeX `@misc` or generic RIS records; Markdown exports include the source record. Complete APA/IEEE/CSL formatting is not claimed.
- Team memberships/collaboration, email verification/recovery, 2FA, social login, native notifications, and comprehensive EN/BN localization remain future work. Unsupported switches were removed.
- Existing research is preserved. Legacy import carries papers, their linked notes/highlight quotes, non-default projects and recent search history, excluding known sample paper IDs and the default sample project. Detached/project notes, legacy browser state, and every older schema variation have not been validated. Retain the original database for further recovery.
- Sync deliberately stops on simultaneous divergent edits. It offers an explicit restore-and-merge path rather than an automatic field-level conflict resolver.
- Full TalkBack traversal, tablet/foldable hardware, API 24/25, and production release acceptance need broader device testing. Automated large-text rendering and the project emulator cover a narrower set of conditions.

## Main files

- [Android source](app/src/main/kotlin/com/example/MainActivity.kt)
- [Backend](server/server.mjs)
- [Run instructions](README.md)
- [Original audit](APP_AUDIT.md)
- [Optimized preview download](../../releases/tag/v2.1-preview)
- Preview SHA-256: `9b2a62e6e6de74d2d73053a12691e3bde6dd58aadd9843a701aa39785097cc3d`
- [Customization and cleanup details](CUSTOMIZATION.md)
