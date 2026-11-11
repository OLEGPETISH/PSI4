# Project Constitution: BNM Android Currency Converter & Analytics

**Project Name**: BNM Android Currency Converter & Analytics (Lab 4)  
**Version**: 1.0.0  
**Target Platform**: Android (Native Java, Android Views & Custom Canvas Charts)  
**Methodology**: Spec-Driven Development (GitHub Spec Kit `specify` + Google Antigravity)  
**Date**: 2026-11-11  

---

## Core Principles

### I. Complete Language Migration (Python -> Java) & Clean Architecture
In compliance with Lab 4 requirements, the entire application MUST be implemented from scratch in **Java** for Android, replacing the **Python** (`Tkinter`) stack used in Lab 3. No code from Lab 3 may be reused or machine-translated. Business logic (BNM XML parsing, currency conversion math, historical time-series aggregation, trend statistics calculation, input validation, and JSON cache serialization) MUST be isolated in pure Java domain/service classes independent of `android.*` UI classes so that all core logic can be verified by fast, deterministic JUnit 4 unit tests via `./gradlew test`.

### II. Multi-Screen Mobile UX with 7-Day & 30-Day Trend Analytics
The Android application MUST provide three integrated screens accessible via top/bottom navigation tabs:
1. **Конвертер (Converter Screen)**: Instant currency conversion using official National Bank of Moldova (BNM) exchange rates, supporting amount presets, `.` and `,` decimal separators, currency swap (`⇄`), date selection (`DD.MM.YYYY`), disabled Convert button when input is empty, unit rates (direct & reverse), and status banners.
2. **Неделя / 7 дней (Weekly Comparison Screen)**: Historical dynamics of any selected currency pair (`Base / Quote`) over the last **7 calendar days**, featuring a custom-drawn time-series line chart (`RateTrendChartView`), minimum and maximum values with dates, arithmetic mean, absolute and percentage change, and an explicit trend badge (`↑ Рост`, `↓ Падение`, or `→ Без изменений`).
3. **Месяц / 30 дней (Monthly Comparison Screen)**: Historical dynamics of the selected currency pair over the last **30 calendar days**, displaying the full 30-day time-series chart, minimum, maximum, mean, absolute/percentage delta, explicit trend direction, and daily breakdown list.

### III. Offline-First Architecture & Pre-Seeded Historical Cache
The application MUST NEVER crash when the device is in Airplane Mode or when `bnm.md` is unreachable:
- All successfully fetched daily rate sheets and historical series MUST be persisted locally to `rates_cache.json` inside the app's persistent storage (`context.getFilesDir()`) and survive app restarts.
- Because accumulating 30 days of data purely from installation time would leave the monthly chart empty during defense, the application combines on-demand BNM XML historical queries with a persistent multi-date local cache pre-seeded with 35+ days of official BNM historical rates (`assets/seed_rates_cache.json` / `data/rates_cache.json`).
- Whenever cached data is displayed, the UI MUST explicitly show the effective cache date and data source (`Локальный кэш НБМ (актуально на DD.MM.YYYY)`).

### IV. Financial Precision (`BigDecimal`)
Floating-point inaccuracies (`double`/`float`) MUST NOT be used for monetary conversion or statistical aggregation. All currency rates, unit values (`Value / Nominal`), converted amounts, minimums, maximums, averages, and percentage deltas MUST be computed using `java.math.BigDecimal` with explicit `RoundingMode.HALF_UP` (4 decimal places for exchange rates and statistics, 2 decimal places for converted monetary totals and percentage deltas).

### V. Strict Input Validation & Defensive Error Handling
- Empty strings, whitespace, non-numeric characters, zero (`0`), negative numbers (`< 0`), and overflow values (`> 1,000,000,000,000`) MUST be rejected with clear, human-readable Russian error messages.
- Unreachable servers, HTTP errors, empty `<ValCurs>` responses on weekends/holidays, malformed XML/JSON, or empty historical datasets MUST be handled gracefully without unhandled exceptions or app crashes.

### VI. Comprehensive Unit Testing via Single Command
All required business logic MUST be covered by automated JUnit 4 unit tests executable via a single command (`./gradlew test` or `gradlew.bat test`):
1. Parsing BNM XML payloads (including `Nominal > 1`, comma decimal values, automatic `MDL` base entry, and malformed/empty XML rejection).
2. Calculating comparison statistics for 7-day and 30-day screens (minimum, maximum, average, absolute & percentage change, and trend classification: `UP`, `DOWN`, `FLAT`).
3. Handling empty datasets and single-point edge cases safely.
4. Validating user amount input across valid, boundary, and invalid formats.

---

## Governance
All specifications (`spec.md`), implementation plans (`plan.md`), task lists (`tasks.md`), and verification checklists (`checklist.md`) must adhere to this Constitution. Branching and Pull Request history must reflect each stage of work, and `REPORT.md` must document the Python-to-Java transition and AI agent collaboration analysis.
