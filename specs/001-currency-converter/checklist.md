# Quality & Acceptance Checklist: BNM Android Currency Converter (Lab 4)

**Feature**: `001-currency-converter`  
**Verified Against**: `requarements.txt` (Parts 1–6)

---

## 1. Language Change & Zero Code Reuse (Part 2)
- [x] Lab 3 language was **Python** (`Tkinter`); Lab 4 is written 100% in **Java** for **Android**.
- [x] Zero code copied or machine-translated from Lab 3; all parsers, calculators, cache managers, and UI views are newly designed in idiomatic Java.

## 2. Functional & Analytical Comparison Screens (Part 3)
- [x] **Screen 1 — Currency Converter**: Amount input (`.` and `,`), currency pair spinners, swap button (`⇄`), date selector, disabled "Конвертировать" button when input is empty, unit rates, and source/cache badge.
- [x] **Screen 2 — Weekly Comparison (7 Days)**:
  - [x] Time-series rate chart (`RateTrendChartView`).
  - [x] Minimum and maximum values over the 7-day period (with dates).
  - [x] Average (mean) value over the 7-day period.
  - [x] Absolute and percentage change over the 7-day period.
  - [x] Explicit trend indicator (`↑ Рост`, `↓ Падение`, `→ Без изменений`).
- [x] **Screen 3 — Monthly Comparison (30 Days)**:
  - [x] Time-series rate chart (`RateTrendChartView`) over 30 days.
  - [x] Minimum and maximum values over the 30-day period (with dates).
  - [x] Average (mean) value over the 30-day period.
  - [x] Absolute and percentage change over the 30-day period.
  - [x] Explicit trend indicator (`↑ Рост`, `↓ Падение`, `→ Без изменений`).

## 3. Inherited Requirements from Lab 3 (Part 4)
- [x] **Offline Mode**: App does not crash in Airplane Mode; cached rates and 35-day historical series persist across app restarts and display the exact cache date.
- [x] **Error Handling**: Unreachable server, empty `<ValCurs>`, malformed XML/JSON, and invalid user inputs produce clear Russian messages without crashing.
- [x] **Unit Tests**: JUnit 4 suite covers XML parsing, 7/30-day statistics calculation (min, max, mean, % change, trend), empty dataset behavior, and input validation via `./gradlew test`.
- [x] **Git & Pull Requests**: Separate feature branches merged into `main` via Pull Requests on GitHub (`https://github.com/OLEGPETISH/PSI4`).
- [x] **Report (`REPORT.md`)**: Contains all 6 required sections including detailed reflection on working with AI in a new language.
