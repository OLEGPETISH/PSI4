# Tasks: BNM Android Currency Converter & Analytics (Lab 4)

**Feature Branch**: `001-currency-converter-spec`  
**Implementation Branch**: `002-android-implementation`  
**Testing & Report Branch**: `003-unit-tests-and-report`

---

## Phase 1: Specification & Spec Kit Setup (`001-currency-converter-spec`)
- [x] **T001**: Initialize Git repository (`main`) with `requarements.txt` and `.gitignore`.
- [x] **T002**: Initialize GitHub Spec Kit (`specify` v1.4.0) with `.specify/`, `.github/skills/`, `specify.cmd`, and `specify.ps1`.
- [x] **T003**: Author Project Constitution (`.specify/memory/constitution.md`) defining Java migration, 3-screen mobile architecture, and financial precision rules.
- [x] **T004**: Author Functional Specification (`specs/001-currency-converter/spec.md`), Technical Plan (`plan.md`), Tasks (`tasks.md`), and Checklist (`checklist.md`).

## Phase 2: Native Java Android Implementation (`002-android-implementation`)
- [x] **T005**: Configure Gradle 9.3.1 project structure (`settings.gradle`, `build.gradle`, `app/build.gradle`, `AndroidManifest.xml`).
- [x] **T006**: Implement domain models in Java (`CurrencyRate`, `RateSheet`, `ConversionResult`, `HistoricalRatePoint`, `TrendDirection`, `PeriodStats`).
- [x] **T007**: Implement core business logic in Java (`InputValidator`, `BnmXmlParser`, `CurrencyConverter`, `TrendAnalyticsCalculator`).
- [x] **T008**: Implement persistent multi-date JSON cache (`HistoricalRatesCache`), 35-day pre-seeded BNM historical dataset (`assets/seed_rates_cache.json` and `data/rates_cache.json`), and HTTP service with holiday/offline fallback (`BnmRateService`).
- [x] **T009**: Implement custom interactive Canvas chart `RateTrendChartView` and `MainActivity` with 3 screens (**Конвертер**, **Неделя 7 дн.**, **Месяц 30 дн.**) and Material-inspired responsive layouts.

## Phase 3: Unit Tests & Lab Report (`003-unit-tests-and-report`)
- [x] **T010**: Write comprehensive JUnit 4 unit test suite (`BnmXmlParserTest`, `TrendAnalyticsCalculatorTest`, `InputValidatorTest`, `CurrencyConverterAndCacheTest`) and verify execution via `./gradlew test`.
- [x] **T011**: Write `README.md` with build, run, and single-command test instructions.
- [x] **T012**: Write `REPORT.md` covering all 6 mandatory sections from Lab 4 requirements, including deep analysis of migrating from Python (Lab 3) to Java (Lab 4) with AI tools.
