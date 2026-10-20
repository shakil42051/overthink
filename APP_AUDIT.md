# Overthink: product, design, frontend, and backend audit

Audit date: 19 October 2026. Status: analysis complete; implementation awaits the user's approval.

Historical baseline: the user authorized the rebuild on 20 October 2026. The obsolete source archive was removed at the user’s request after verifying it was excluded from all builds. Historical source paths below document the audited version; they are not live file links. See [REBUILD_STATUS.md](REBUILD_STATUS.md) for implemented work and remaining boundaries.

## 1. What this app is

Overthink is a native Android academic research workspace, written in Kotlin with Jetpack Compose and Material 3. Its intended workflow is **discover → evaluate → read → annotate → organize → compare → plan a thesis/project → export citations**. It combines a federated academic search engine, a reference library, a manuscript reader, an AI research assistant, and a student project workspace.

The strongest intended audience is university students doing thesis/FYDP work, researchers, and supervisors. Bangla queries, Bangladesh defaults, CSE departments, and viva preparation suggest an initial focus on Bangladeshi university students. That audience inference is supported by the implementation and the existing AI Studio planning document; it is not a new business requirement supplied by the user.

The useful product promise is: **Find credible research, understand the evidence, and turn it into an organized research project.** The present code is an ambitious prototype with some real API and local storage functionality, mixed with simulations presented as completed features. It is not currently a dependable research tool or a complete client/server system.

Keep the Overthink identity and academic focus. Rebuild the foundations and simplify the experience around the research workflow. A change of programming language or a conversion into a website is not justified by this audit.

## 2. Scope, evidence, and limits

Reviewed the repository inventory, Gradle configuration, manifest, resources, all feature areas, API orchestration, authentication/admin repositories, Room models/DAOs, shared state, navigation, theme, layout patterns, motion, and the existing tests. The main application has 44 Kotlin files and approximately 26,159 nonblank lines, measured with PowerShell. Several individual UI files exceed 100 KB.

This is a source-based audit plus local environment and binary-header checks. It is **not** a completed emulator, device, penetration, performance, or visual acceptance test. Exact clipping, frame timing, keyboard behavior, TalkBack traversal, real provider responses, and production service configuration remain to be verified at runtime. Design judgments below are distinguished from directly demonstrated code defects. No claim that every possible defect has been discovered is warranted.

No app source or build configuration was changed during this audit. Only this report was added.

### Verification performed

| Check | Result |
| --- | --- |
| Repository architecture and feature inventory | Native Android application; no server application or server deployment configuration found. |
| Reproducible build entrypoint | No `gradlew`, `gradlew.bat`, wrapper JAR, or wrapper properties in the supplied project. A machine-level cached Gradle 9.4.1 distribution exists, but is not a project-pinned build setup. |
| Java availability | `java` and `gradle` were not resolved on PATH. `JAVA_HOME` points to `C:/Program Files/Java/jdk-24`; its `bin/java.exe` does not exist. |
| Android tools | An Android SDK directory exists. The `adb devices` attempt failed because ADB could not create the user's `.android` directory under current permissions. No device interaction occurred. |
| Tests/build/lint | Not run. Build tooling is incomplete in this checkout/environment; no passing result is claimed. |
| Existing screenshot | The image viewer rejected it. Byte signature starts `C2 89 50 4E 47 0A 1A 0A`, rather than the PNG signature `89 50 4E 47 0D 0A 1A 0A`. It is not a usable visual baseline. |
| Launcher images | All ten supplied WebP launcher files have inconsistent RIFF sizes; three also lack `WEBP` at its required offset. Their headers show corruption consistent with binary data being transformed as text. Exact cause is unproven. Restore or regenerate and decode-check them. |
| Platform documentation | Android's Base64 API level and Room destructive migration behavior were verified against official documentation; links appear below. |

### Evidence index

References throughout this report point to these files and named functions. Line anchors are from the audited checkout.

| Ref | Source |
| --- | --- |
| E01 | Admin authentication (`legacy/android-v1/src/main/java/com/example/data/repository/AdminRepository.kt:151`, removed): `authenticateAdmin`, defaults, provider settings, backup records. |
| E02 | Account repository (`legacy/android-v1/src/main/java/com/example/data/repository/AuthRepository.kt:28`, removed): seeding, login, verification/reset, roles, settings, deletion. |
| E03 | Password hashing (`legacy/android-v1/src/main/java/com/example/security/PasswordHasher.kt:1`, removed): Base64, PBKDF2, SHA fallback, legacy password exception. |
| E04 | Shared ViewModel (`legacy/android-v1/src/main/java/com/example/ui/viewmodel/ResearchViewModel.kt:121`, removed): initialization, search, reader, browser, AI, account/admin commands. |
| E05 | Academic API service (`legacy/android-v1/src/main/java/com/example/data/api/AcademicApiService.kt:271`, removed): provider federation, filtering, scoring, metadata adapters, local catalog. |
| E06 | AI client (`legacy/android-v1/src/main/java/com/example/data/api/GeminiResearchClient.kt:1`, removed): embedded key, generation prompts, fallback outputs. |
| E07 | PDF reader (`legacy/android-v1/src/main/java/com/example/ui/components/PdfViewerDialog.kt:2222`, removed): `buildManuscriptPages`. |
| E08 | Paper details (`legacy/android-v1/src/main/java/com/example/ui/screens/PaperDetailsScreen.kt:1497`, removed): `buildEditorialSectionContent`, citation and related tabs. |
| E09 | Portal explorer (`legacy/android-v1/src/main/java/com/example/ui/components/ResearchBrowserDialog.kt:1474`, removed): fetching, paragraph extraction, deterministic snapshots. |
| E10 | Project task parser (`legacy/android-v1/src/main/java/com/example/ui/screens/ProjectsScreen.kt:95`, removed), repository (`legacy/android-v1/src/main/java/com/example/data/repository/ResearchRepository.kt:326`, removed), ViewModel parser (`legacy/android-v1/src/main/java/com/example/ui/viewmodel/ResearchViewModel.kt:670`, removed). |
| E11 | Room database (`legacy/android-v1/src/main/java/com/example/data/AppDatabase.kt:56`, removed), entities (`legacy/android-v1/src/main/java/com/example/data/entities/Entities.kt:1`, removed), research DAO (`legacy/android-v1/src/main/java/com/example/data/dao/ResearchDao.kt:1`, removed). |
| E12 | Research repository (`legacy/android-v1/src/main/java/com/example/data/repository/ResearchRepository.kt:59`, removed): dual paper records, mutations, citations, workspace seeding. |
| E13 | Navigation shell (`legacy/android-v1/src/main/java/com/example/MainActivity.kt:125`, removed): booleans, navigation bar/rail, screen transitions and overlays. |
| E14 | Theme (`legacy/android-v1/src/main/java/com/example/ui/theme/Theme.kt:1`, removed), type (`legacy/android-v1/src/main/java/com/example/ui/theme/Type.kt:1`, removed), responsive helpers (`legacy/android-v1/src/main/java/com/example/ui/theme/ResponsiveUtils.kt:1`, removed). |
| E15 | Home (`legacy/android-v1/src/main/java/com/example/ui/screens/HomeScreen.kt:109`, removed), search (`legacy/android-v1/src/main/java/com/example/ui/screens/SearchScreen.kt:80`, removed), filters (`legacy/android-v1/src/main/java/com/example/ui/components/FilterBottomSheet.kt:1`, removed). |
| E16 | Library (`legacy/android-v1/src/main/java/com/example/ui/screens/LibraryScreen.kt:84`, removed), comparison (`legacy/android-v1/src/main/java/com/example/ui/screens/PaperComparisonScreen.kt:77`, removed). |
| E17 | Profile (`legacy/android-v1/src/main/java/com/example/ui/screens/ProfileScreen.kt:1818`, removed), 2FA setup (`legacy/android-v1/src/main/java/com/example/ui/screens/auth/AuthDialogs.kt:1336`, removed). |
| E18 | [Build](<D:/My Project/overthink/app/build.gradle.kts:1>), [version catalog](<D:/My Project/overthink/gradle/libs.versions.toml:1>), manifest (`legacy/android-v1/src/main/AndroidManifest.xml:1`, removed). |
| E19 | Screenshot test (`legacy/android-v1/src/test/java/com/example/GreetingScreenshotTest.kt:1`, removed), Room test (`legacy/android-v1/src/test/java/com/example/ExampleRobolectricTest.kt:1`, removed), instrumentation test (`legacy/android-v1/src/androidTest/java/com/example/ExampleInstrumentedTest.kt:1`, removed). |
| E20 | Original product plan (`.aistudio/artifacts/brain/d966c87b-ea29-464e-8568-fd718a9dd1ef/implementation_plan.md:1`, removed). Historical intent only; its claims of complete functionality and zero defects are not verification. |

## 3. Release blockers: correct these first

P0 means a trust/security blocker; P1 means high impact functional or reliability work; P2 means usability, quality, or maintainability work. Priority describes the current product, not an assertion that a remote attack has been demonstrated.

| ID | Priority | Finding and consequence | Required correction |
| --- | --- | --- | --- |
| T01 | P0 | Admin authentication never checks `passOrPin` or whether the account is enabled; unknown names fall back to the admin account. Any supplied password can succeed. E01. | Remove the bypass. Authenticate with a trusted identity service and authorize every privileged operation on the server. |
| T02 | P0 | Profile exposes “1-Click Super Admin”; the ViewModel elevates the current user, or unlocks admin for a guest. Admin command methods also substitute a super-admin name when no admin is authenticated. E04/E17. | Remove self-elevation, default privileges, and client-controlled authorization. Keep administration outside ordinary researcher workflows. |
| T03 | P0 | Both reader experiences build invented manuscript sections, equations, benchmark tables, and numerical results from templates. They do not render the linked PDF. E07/E08. | Replace with actual document rendering/extraction. If only an abstract exists, display only that abstract and clearly state full text is unavailable. |
| T04 | P0 | Portal fetch failures produce invented scholarly paragraphs and links. Even the deterministic fallback reports HTTP 200 and `isLiveFetched=true`. Partial HTML parsing also reuses fabricated links/headings. E09. | Show honest retrieval errors or explicitly labeled cached content with provenance. Use a real browser handoff for the original site. |
| T05 | P0 | Missing AI configuration, request failures, and invalid generated JSON produce plausible generic research conclusions labeled as grounded summaries. E06. | Return typed unavailable/error states. Separate templates from AI output. Attach source passages to supported claims. |
| T06 | P0 | Verification/reset codes are returned to the same requesting UI; no email delivery exists. Password reset exposes a usable reset code after looking up an email. E02/E04/E17. | Use managed verification and recovery delivered through an independently controlled channel. Never return recovery secrets to the requesting app. |
| T07 | P0 | 2FA setup uses a shared fixed secret and fixed recovery codes. Login does not challenge 2FA at all. E02/E17. | Remove the misleading protection immediately; implement server-verified enrollment/challenge/recovery before offering 2FA. |
| T08 | P0 | Research tables contain no account ownership; DAOs return all local records. Different accounts use the same library/projects/notes/history. Logout leaves research and other global state intact. E04/E11. | Introduce account/workspace ownership and scoped queries, server access rules, guest isolation, and account-switch cleanup. |
| T09 | P1 | Room version 6 uses destructive migration fallback and disables schema export. An upgrade without a migration can erase the research workspace. E11. | Export schemas and implement/test non-destructive migrations before changing data structures. |
| T10 | P1 | `java.util.Base64` is called during startup account seeding, but minSdk is 24 and no core-library desugaring is configured. Android documents this class as API 26. E02/E03/E18. | Use compatible encoding APIs or explicitly configured support; test API 24/25 startup. This is a compatibility defect identified statically, not an observed device crash. |
| T11 | P1 | Binary resource checks show damaged launcher images and an invalid screenshot baseline. | Restore binary assets from valid originals or regenerate; verify decoding and resource compilation. Inspect the fonts with a font parser as well before relying on them. |

Official references: [Base64 API reference](https://developer.android.com/reference/java/util/Base64) establishes API 26 availability. [Room migration guidance](https://developer.android.com/training/data-storage/room/migrating-db-versions) explains that destructive fallback recreates tables when a migration path is missing.

## 4. Search, metadata, and AI audit

| ID | Priority | Current problem | Improvement |
| --- | --- | --- | --- |
| S01 | P1 | `performSearch` launches a new independent job on every submission. An older response can overwrite a newer query's results. Paper summary/highlight loads have similar stale-selection risks. E04. | Cancel obsolete work, bind results to request/paper IDs, and ignore stale completions. |
| S02 | P1 | Papers, then datasets, then repositories are awaited sequentially at the ViewModel level. The user waits for unrelated categories. Every search fetches all three. E04. | Run independent categories concurrently, publish partial results, and load selected categories on demand. |
| S03 | P1 | Provider fetchers catch exceptions and return empty lists. Offline, HTTP failure, bad credentials, malformed responses, and genuine zero matches become indistinguishable. E05. | Typed provider outcomes: success/empty/timeout/rate-limited/unauthorized/parse-error, with retry guidance. |
| S04 | P1 | Provider health is inferred from whether results exist; measured latency starts immediately before `await`, after requests have already run. Admin “ping” chooses a random health status. E04/E05. | Measure the actual request and probe endpoints; distinguish service health from query matches. |
| S05 | P1 | Dataset availability is inferred from citation count; code availability from title words; several adapters blanket-mark records as peer reviewed. Metadata defaults also assume open access and peer review. E05/E11. | Use `Known/Unknown` evidence-backed fields, explicit URLs, source timestamps, and conservative defaults. |
| S06 | P1 | Hardcoded catalog entries are mixed into remote results and described as verified. Seeded titles/DOIs/PDF links and numerical claims are not established as matching records. E05/E12. | Keep demo fixtures in debug/demo mode. Resolve canonical metadata and require identifier/title consistency for production records. |
| S07 | P1 | “Direct Full-Text PDF Available” changes `freePdfOnly`, which is never applied by the search engine. Empty queries bypass all filters. `searchMode` is accepted but unused in federation. E05/E15. | Implement each visible control end to end or remove it. Apply filters consistently to defaults, results, and categories. |
| S08 | P1 | Dataset/repository searches receive no active `FilterState` from the ViewModel. Admin provider configuration is a separate data path from actual search settings. E04/E05. | One validated provider/configuration contract, propagated to all relevant searches. |
| S09 | P1 | DOI/arXiv entry is sent through broad text search rather than an identifier-specific resolver. E13/E15. | Normalize DOI URLs/arXiv versions, validate input, and fetch canonical records by identifier. |
| S10 | P1 | `pdfUrl` sometimes contains a DOI or generic landing page. “Read PDF” can point to unrelated fallback URLs when the record has no PDF. E05/E16. | Separate landing page, DOI, full-text URL, and verified PDF URL. Validate content type and access before opening. |
| S11 | P2 | Results are small fixed first pages; no user pagination/cursor flow was found. Dates/default year range are hardcoded. E05 and academic models. | Cursor-based load more, current-year defaults, configurable historical range, deduplication across pages. |
| S12 | P2 | “Semantic Search” is rule/dictionary expansion and lexical scoring. Bangla support substitutes a small dictionary; a separate AI fallback maps Bangla input to generic ML research. E05/E06. | Label keyword expansion accurately, preserve original queries, preview translations, and support genuine bilingual relevance evaluation. |
| S13 | P1 | Gemini API key is compiled into `BuildConfig` and used directly in requests. There is no app-owned service enforcing per-user quotas or protecting the shared AI credential. E06/E18. | Route protected AI calls through an authenticated backend with quotas, model configuration, budget controls, and redacted logging. |
| S14 | P1 | Paper analysis has title/authors/abstract only; “grounded” claims lack passage/page citations. General chat requests contain the current prompt, not persistent conversation history. E06. | Define abstract-only vs full-text modes, source-linked retrieval, structured response validation, and persisted contextual conversations. |
| S15 | P2 | Preview model ID is hardcoded; request behavior has no explicit output schema, token/cost controls, or user cancel flow. E06. | Validate a supported model during implementation, configure centrally, enforce schemas and output limits, support cancel/retry. No model availability was tested here. |
| S16 | P1 | Comparison's “AI Comparative Synthesis” is a local string using citation totals and dates; blank methods/algorithms/limitations are filled with invented defaults. E16. | Label calculated metadata as metadata. Use “not reported” for unknowns; only offer source-backed AI comparison when available. |

## 5. Reader, library, and project audit

| ID | Priority | Current problem | Improvement |
| --- | --- | --- | --- |
| W01 | P1 | Reader search returns the same page list for any nonempty query, including page 12 when default total pages is 10. E04. | Search actual document text and return real matches/locations; no synthetic results. |
| W02 | P1 | Offline saving flips a Boolean. Browser downloading adds an in-memory item with a fixed size and 100% progress. No corresponding file download occurs. E04. | Download real bytes with progress/cancellation, storage limits, file validation, retry, and persistent download state. |
| W03 | P1 | Notebook export only shows an “Exported” snackbar. Admin backups only insert completed job records. E01/E04. | Write an actual export/backup artifact, let the user save/share it, and verify restore before claiming success. |
| W04 | P1 | Reader bookmarks/settings/timer/browser history are held in shared memory. Notes are written to Room but reader reopening does not restore the complete per-paper note state through its loader. E04. | Persist per-document state, restore all annotations and reading location, and separate browser/app/account state. |
| W05 | P1 | `openPdfViewer` resets page count/progress to guessed values. Reader notes/bookmarks/settings can outlive a document selection. E04. | Key reader state by document ID and revision; derive page counts from the document. |
| W06 | P1 | Freehand and Pomodoro commands toggle flags; the inspected code has no timer countdown engine or durable drawing data path. E04. | Implement real behavior with lifecycle handling or defer these controls. |
| W07 | P1 | Project creation stores task JSON arrays, while the project screen parses `||` entries and `::` statuses. A second ViewModel parser expects JSON with pipe-delimited booleans. E10. | One typed Task model/table, one status enum, and a migration for old formats. |
| W08 | P1 | Initial project progress is hardcoded at 25%, even for new plans. Blank tasks produce sample completed milestones. Department/field handling defaults to CSE/AI. E10/E12. | Compute progress from actual tasks, start fresh projects honestly, and preserve the chosen discipline and requirements. |
| W09 | P2 | Deadlines are free text; team size is passed to planning without a membership/assignment backend. “Collaboration” flags overstate current capability. E01/E10. | Structured dates, task due dates and assignees; implement workspace invites/roles only with real shared storage. |
| W10 | P1 | Papers exist in both `saved_items` and `research_papers`; save/move/delete mutations are multiple nontransactional writes. Re-saving reconstructs a paper entity with default reading state. E11/E12. | Canonical record plus collection membership, transactional mutations, preserve reading progress/favorites. |
| W11 | P1 | Collection deletion deletes the collection row without reassigning its items. Moving saved papers does not move related highlight collection metadata. No foreign keys enforce integrity. E11/E12. | Stable collection IDs, membership relations, clear deletion policy, cascading/reassignment rules, transactional updates. |
| W12 | P2 | Stored collection counts can drift; items have one collection string, and search history is keyed by query alone, so one query cannot independently belong to several projects. E11. | Derive counts; support explicit many-to-many item/project/collection links and distinct saved searches. |
| W13 | P1 | Citation output is hand-built and incomplete for the labeled styles. Library export treats datasets/repos as articles, uses non-year values as years, does not escape bibliographic characters, and can generate duplicate keys. E12/E16. | Typed bibliographic metadata, reliable style formatting, correct entry types, escaping, unique keys, and export fixture validation. |
| W14 | P2 | Destructive operations such as library removal execute immediately; recovery/undo is inconsistent. E16/E04. | Undo for reversible removal and explicit confirmation for permanent project/account destruction. |

## 6. Accounts, backend, and operational audit

There is no application-owned backend in this repository. Room repositories are local persistence code, not a service capable of enforcing remote identity, collaboration, global administration, email delivery, or synchronization. Public academic endpoints and Gemini are real external dependencies, but do not supply those missing app services.

| ID | Priority | Current problem | Improvement |
| --- | --- | --- | --- |
| B01 | P1 | Startup seeds accounts and automatically authenticates the default researcher without checking credentials, status, or a valid session. Account deletion can be followed by reseeding on a later launch. E02/E04. | Explicit guest/demo mode, managed sign-in, legitimate session restoration, and permanent deletion semantics. |
| B02 | P1 | Login rejects suspended/locked accounts but not `DEACTIVATED`. Login case handling differs from registration. Session expiry/validity is not used as a continuing access boundary. E02 and AuthDao. | Consistent active-state validation, normalized identifiers, token expiry/revocation enforcement, and session-aware routes. |
| B03 | P1 | Password hashing permits a SHA-256 fallback and a legacy plain-password exception. Password changes do not revoke other sessions. E02/E03. | Prefer managed identity; remove downgrade/legacy exceptions and apply appropriate session revocation policies. |
| B04 | P1 | Recovery codes, TOTP material, sessions and account data are ordinary Room fields. Backup is enabled with sample exclusion rules. E11/E18. | Minimize local secrets, protect needed credentials with platform-backed storage, hash recovery tokens, and exclude sensitive state from backup/transfer. |
| B05 | P1 | Linked Google/GitHub/ORCID switches merely change Boolean fields. Notifications, autosync, ad blocking, privacy and AI settings mostly store preferences without corresponding service behavior. E02/E04. | Implement verifiable OAuth connections and actual behavior, or hide/defer unsupported options. |
| B06 | P1 | Admin-managed app users use a different entity/table from actual login accounts. Suspending/deleting an admin-list user does not enforce actual account access. E01/E02/E11. | One authoritative identity and role model with server-side administration. |
| B07 | P1 | Admin seed loops unconditionally reinsert providers/websites/announcements/users/reports/backup records on startup, overwriting local edits. E01. | One-time versioned defaults and explicit configuration migration; never overwrite user/admin changes as startup seeding. |
| B08 | P1 | Account deletion only deletes account/session records, leaving research data and potentially related tokens/audits; no ownership model permits a clear selective wipe. E02/E11. | Define retention and deletion behavior, account-scoped cascading cleanup, and consented export before permanent deletion. |
| B09 | P2 | No app-owned quota service, sync/conflict protocol, crash reporting integration, CI workflow, or deployment/runbook was found. | Add only the necessary production operations with privacy-aware telemetry, health metrics, backups and restore drills. |

## 7. UI, UX, layout, accessibility, and animation

### Present visual direction

The design already has a recognizable academic direction: navy/slate headings, white cards, teal actions, Plus Jakarta Sans UI text, Merriweather reading text, and monospace identifiers. Keep that foundation, but reduce the density and repeated administrative/academic labels. A paper should be easy to scan and evaluate before encountering five different ways to analyze it.

| ID | Priority | Finding | Design correction |
| --- | --- | --- | --- |
| U01 | P2 | Home mixes search, domain filters, portals, research cards, history, statistics, and assistant shortcuts. Some domain filters silently return all papers if nothing matches. E15. | Put search and “continue your work” first. Show a truthful empty domain state; group optional discovery below. |
| U02 | P2 | Search history and workspace panels compete with results, and loading replaces results with a full-area spinner. E15. | Keep submitted query/filter context visible, collapse history after submission, show skeleton/partial results and per-provider status. |
| U03 | P2 | Dense badges, small metadata, multiple inline actions, and repeated “verified/grounded” language inflate cognitive load. E08/E15/E16. | One primary action per card, two lightweight secondary actions, overflow for the rest. Evidence labels must have explainable meaning. |
| U04 | P1 | Theme defaults to light at the root; profile theme preference is not supplied to it. Many screens hardcode white/background/text colors. E13/E14/E17. | Semantic theme tokens throughout; apply system/light/dark preference at the root and test contrast in each theme. |
| U05 | P2 | Common body styles are 13–15sp; labels are often 10–11sp. Several pills/buttons are 30–36dp tall. E14/E15/E16. | Increase primary reading/scanning text, provide at least 48dp interactive targets, verify large font and display scaling. Small decorative text can remain small where nonessential. |
| U06 | P2 | Teal/amber/muted slate are used on light surfaces, sometimes with small text. Contrast has not been measured across all states. | Separate accent fills from accessible text colors, preserve status with text/icons, measure every foreground/background pair. |
| U07 | P2 | Rail/bottom-navigation adaptation exists, but wide layouts often use `fillMaxSize()` before `widthIn(max=...)`; a tight incoming width can prevent the intended cap. E14 plus project/assistant screens. | Apply width constraints in an effective order and verify measured bounds; use list/detail panes on tablets instead of stretched phone columns. |
| U08 | P2 | Comparison uses a 150dp label column plus 260dp per paper and horizontal scrolling. Some action heights are 30dp. E16. | On phones, compare two papers with selectable attributes or stacked rows; on tablets, freeze row labels and maintain readable columns. |
| U09 | P1 | Navigation is integer tabs and independent remembered Booleans, not a durable route/back stack. Rotation/process death can lose routes/forms and replay splash. E13. | Typed navigation, saved route state, per-tab back stacks, predictable back/up behavior, deep links. |
| U10 | P2 | Most transient form/filter state uses `remember`; main streams use `collectAsState` instead of lifecycle-aware collection. E13/E15/E17. | Save meaningful drafts and selection; use lifecycle-aware collection and screen-scoped state. |
| U11 | P2 | Splash always waits roughly one second and animates progress independently of actual initialization. E13 and SplashScreen. | Use startup splash tied to readiness; eliminate fake progress, support repeat launches and restored state. |
| U12 | P2 | Screen changes use essentially the same crossfade. Shimmer exists but search currently shows a spinner; motion has no explicit reduced-motion design. E13/E15 and SkeletonShimmerLoader. | Consistent task-based motion: short feedback, directional drill-down, preserved scroll, static loading alternative. Measure jank on devices. |
| U13 | P2 | UI language is hardcoded English except selected Bangla strings/preferences. Only app name is in the main string resource. E18. | Resource-based EN/BN strings, pluralization, translated errors/content labels, locale-aware dates/numbers and font coverage. |
| U14 | P2 | Profile/admin occupy huge screens and expose technical configuration irrelevant to normal research. E17/E01. | Compact account/settings groups; separate authorized admin tools. Put research activity ahead of infrastructure controls. |
| U15 | P2 | Portal explorer resembles a full browser but only extracts a few HTML paragraphs; DOI “scanner” has no camera path. E09 and DoiScannerDialog. | Call these “Open source” and “Add by DOI” unless real browsing/scanning is implemented. Set capability expectations accurately. |
| U16 | P2 | No runtime accessibility audit exists; clickable surfaces may lack button/selected/state semantics, beyond content descriptions on icons. | TalkBack traversal, headings, selected states, keyboard focus, announcements for errors/loading, switch access and non-color cues. Verify rather than assuming semantics from appearance. |

### Screen-by-screen replacement specification

| Surface | Proposed experience |
| --- | --- |
| First launch | Brief product explanation, guest start, optional sign-in. A fresh real workspace; demo data only through an explicit demo choice. |
| Home | Greeting/context, large unified search, continue reading, active project, recent saved items. Keep recommendations secondary and evidence-backed. |
| Search | Persistent query, Papers/Datasets/Code tabs, compact filter chips, result list, load more. Separate external portals from actual indexed results. Show partial service failures without hiding successful results. |
| Result card | Title; authors/year/venue; short abstract; access/source information. Primary “View paper”; secondary save/compare; overflow citation/share. Explicit “Unknown” where metadata is missing. |
| Paper details | Verified metadata/abstract first. Clear actions for actual PDF, source, save and cite. AI summary is a distinct section with source coverage and citations. |
| Reader | Actual pages, honest load/error states, page navigation, search, zoom, bookmarks and notes. Collapse toolbars while reading. Annotations must point to real page coordinates or extracted text offsets. |
| Library | Search, collections/tags, reading status, sort, bulk selection, export. Distinct representations for papers, datasets, code and web pages. Undo removal. |
| Projects | Project overview, evidence/library links, real task board, dated milestones and notes. Allow manual projects without AI. Add collaboration only when memberships work. |
| Comparison | Explicitly selected papers and source-backed attributes; “not reported” for missing fields; usable two-paper phone mode. Explain which summaries are computed and which are AI generated. |
| AI assistant | Explicit paper/project context chips, retained conversation, source links, cancel/retry and error states. Explain abstract-only coverage. Avoid automatic selection of an unrelated first project. |
| Source browsing | Hand off to a real browser/custom tab. Optional extracted reader mode only for successfully extracted content; show original source and retrieval timestamp. |
| Settings | Account, appearance/language, reading/citation defaults, storage/downloads, privacy/export/delete, help. Offer only functioning settings. |
| Admin | Separate authorized surface for real users, provider configuration, measured service health and auditable changes. No self-granting role controls. |

### Design and motion targets

Use a small shared token system for spacing, typography, color roles, shape, elevation and motion. Start with an 8dp spacing rhythm, 16dp phone gutters, readable 16sp body text, 12–14sp secondary metadata, and 48dp minimum touch targets. Long-form reading should have adjustable type, line height, and a comfortable line length. Preserve familiar navy/teal branding while validating actual contrast.

Phone screens should prioritize one task and avoid multiple rows of competing controls. Medium/expanded windows should gain a navigation rail and true list/detail panes where useful. Test compact widths, landscape, split screen and large text. The exact breakpoints and component dimensions must be validated against rendered screens.

Suggested motion specification, to validate during implementation: 100–150ms for immediate selection feedback, 180–240ms for navigation, 200–300ms for sheets. Use direction for entering/leaving detail views, subtle save confirmation, and real download progress. Avoid decorative motion in the reader, fabricated progress, and whole-screen reloads for local actions. Respect system animation settings and offer static loading states. These are design targets, not measured current performance.

## 8. Engineering and QA gaps

| ID | Priority | Current problem | Improvement |
| --- | --- | --- | --- |
| Q01 | P1 | Project lacks a Gradle wrapper and usable configured Java path; debug signing expects a missing custom keystore. README tells the user to remove `debugConfig` from a location that actually contains release signing. E18/README. | Restore a pinned wrapper and documented supported JDK/SDK setup, use standard debug signing, isolate release signing, and provide exact setup commands. |
| Q02 | P1 | Instrumentation test expects `com.example`, while application ID is `com.aistudio.researchverse.ai`; the assertion is inconsistent with configuration. E18/E19. | Verify the correct application ID and test real behavior. |
| Q03 | P1 | Existing tests cover arithmetic, context, one Room path and a logo screenshot. No tests exercise auth/admin, task serialization, provider errors, races, PDF truthfulness, migrations or account isolation. E19. | Add targeted regression and integration tests for those risks before expanding features. |
| Q04 | P2 | One AndroidViewModel owns nearly every domain; UI screens and service files are very large. Concrete singletons are created directly, limiting substitution in tests. E04/E05/E17. | Separate screen ViewModels and repositories/use cases; inject interfaces and clocks/dispatchers; extract shared components and parsers. |
| Q05 | P1 | Broad exception handling suppresses meaningful failures, including cancellation; blocking OkHttp calls are not wired to coroutine cancellation. Some asynchronous operations lack robust failure/finally state cleanup. E04/E05/E06. | Propagate cancellation, use cancellable network adapters, typed errors and reliable loading cleanup; dispatch UI callbacks safely on the main thread. |
| Q06 | P2 | A single snackbar string can overwrite concurrent events; identical messages may be lost, and errors are frequently converted into success-like messages. E04/E13. | Event queue/channel plus persistent inline error state for actionable failures. |
| Q07 | P2 | Navigation/Firebase/Retrofit dependencies exist alongside manual navigation/direct JSON/networking; release minification is disabled. Dependency compatibility has not been established by a build. E18. | Remove unused dependencies, validate the actual AGP/Kotlin/KSP/SDK matrix, enable safe release shrinking after testing. Do not blindly upgrade everything. |
| Q08 | P2 | No trustworthy screen baselines, performance benchmarks, or accessibility test matrix. | Render all major states and screen sizes; add startup/scroll benchmarks and a focused acceptance checklist. |

## 9. Concrete rebuild proposal

### Product boundary

Rebuild this as **Overthink, an Android research and thesis workspace**. Retain the product concept and useful domain models; replace simulated behavior and oversized coupled implementations. Prefer a controlled replacement in working slices over deleting the whole app before its replacement is usable.

The first complete version should deliver: trustworthy search, actual document reading, a durable organized library, notes/highlights, evidence-backed AI, useful projects, and correct citation/export flows. A full browser, gamified analytics, elaborate admin UI, freehand tools and Pomodoro should not delay these core capabilities. They can return when their behavior is real and useful.

### Target architecture

```text
Compose screens + shared design system
  → screen-specific state/ViewModels + typed navigation
  → domain actions/repository interfaces
      → Room cache + preferences + durable file storage
      → authenticated application API
          → managed identity + workspace authorization
          → academic provider adapters + canonical metadata
          → AI gateway + retrieval + quotas
          → relational workspace storage + document/object storage
          → sync jobs, notifications, health metrics and backups
```

Keep offline research available through local persistence. Server authority governs accounts, memberships, protected credentials and shared data. Avoid operating both local and remote competing sources of truth without a defined sync protocol.

Use one production backend stack rather than mixing several incomplete systems. A pragmatic candidate is managed authentication plus a small API and relational database/object storage; the exact provider should be chosen during implementation based on deployment access and budget. Existing Firebase dependency declarations alone do not mean Firebase is configured. A functioning local/guest mode can be delivered before remote account features are enabled.

### Data changes

Define `User`, `Workspace`, `Membership`, `Paper`, `ProviderRecord`, `Document`, `Collection`, `CollectionItem`, `Project`, `ProjectPaper`, `Task`, `Annotation`, `Note`, `SavedQuery`, `Conversation`, and `Message` with stable identifiers. Scope private records to a workspace. Model source provenance, fetched time, canonical IDs, verified document URLs and unknown metadata explicitly.

Store task status and due dates as typed values. Store annotation locations against a document/revision rather than fabricated pages. Persist local file locations and verified download states. Use unique constraints, foreign keys, transactions and migrations. Use conflict/version timestamps for sync; do not silently overwrite edited notes.

Before migration, back up existing research records with a real export and verify a restore. Preserve genuine user work. Existing synthetic sample records need explicit identification and exclusion from real research rather than silent promotion into canonical records.

### Delivery sequence and acceptance gates

| Phase | Deliverable | Must be demonstrated before continuing |
| --- | --- | --- |
| 1. Recover and contain | Reproducible build, valid binary assets, remove auth/admin bypasses and fabricated research presentation, isolate demo mode. | Debug build/test/lint runs; no fake manuscript, live status, download or export success remains. |
| 2. Design foundation | Theme tokens, reusable components, typed navigation, phone/tablet layouts, functional preference shell. | Main flows render at compact and expanded sizes, large text, light/dark; back/rotation/restoration behave correctly. |
| 3. Search and library | Canonical metadata/provider adapters, real filters, partial failures, cancellation, scoped transactional storage. | Exact DOI lookup, all visible filters, race handling, save/move/remove and relaunch persistence pass. |
| 4. Reading and exports | Actual PDF/text loading, real page search, anchored notes/highlights, real files/downloads/citations. | Original document matches displayed content; offline reading works; exported files open; annotations survive restart. |
| 5. AI and projects | Context-aware cited AI with honest errors, typed project/tasks, manual project creation. | No-source cases refuse unsupported claims; changing papers cannot cross-wire summaries; tasks/progress survive round trips. |
| 6. Connected production services | Real identity, account/workspace isolation, synchronization, quotas and authorized administration. | Two-account isolation, denied unauthorized access, recovery, deletion, sync conflicts and quota handling pass against the deployed service. |
| 7. Release hardening | Migration tests, critical flow regressions, accessibility/performance/device checks, release signing and operations docs. | Install/upgrade/restart/offline flows pass; backup restores; signed release passes agreed checks. |

### Required regression scenarios

1. Wrong admin password is rejected; ordinary/guest users cannot elevate or invoke privileged actions.
2. Two users never see each other's private library, notes, queries or projects.
3. Deactivated/deleted users cannot be auto-signed in or silently recreated.
4. Search A followed by search B never shows A's late results under B.
5. Empty results, offline, HTTP 429 and provider failures have distinct truthful states.
6. Direct PDF, peer review, date and provider filters are effective, including initial/default views.
7. Missing metadata stays unknown; no fabricated methods, metrics, code links or summaries appear.
8. Switching paper A to B during loading cannot attach A's summary/highlights to B.
9. An actual supplied PDF has matching page count, text, search locations and annotations.
10. Downloads and exports create verifiable files; failed writes never report success.
11. A project plan's tasks display, update and reopen correctly through one serialization/schema.
12. Saving/moving/deleting papers preserves integrity, reading state and annotation associations.
13. Schema upgrade preserves a populated research workspace and supports restore.
14. API 24/25 startup works if that minimum SDK is retained; current target-device behavior is verified.
15. Keyboard, TalkBack, large text, split screen, rotation, process recreation and reduced animation behave sensibly.

## 10. Decision requested

The audit supports a substantial rebuild of both frontend and data/service foundations. Cosmetic changes alone will leave the most serious problems intact.

Approve the phased rebuild above to begin implementation. App code has been left unchanged pending that decision. A production backend deployment will also need working hosting/account configuration when that phase is reached; its integration cannot be claimed complete from source changes alone.
