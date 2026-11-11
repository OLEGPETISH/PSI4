# Technical Implementation Plan: BNM Android Currency Converter & Analytics

**Branch**: `001-currency-converter-spec`  
**Language / SDK**: Java 17 bytecode target (compiled with JDK 21), Android SDK 36 (`minSdk 24`, `targetSdk 36`)  
**Build System**: Gradle 9.3.1 + Android Gradle Plugin (AGP)  
**Test Framework**: JUnit 4 (`./gradlew test`)

---

## 1. Architectural Overview

To satisfy both Android platform requirements and fast headless unit testing via `./gradlew test`, the project follows a layered Clean Architecture inside `app/src/main/java/md/bnm/converter/`:

```text
app/src/main/java/md/bnm/converter/
├── model/
│   ├── CurrencyRate.java          # Immutable currency entry (charCode, nominal, name, value, unitRate)
│   ├── RateSheet.java             # Daily rate sheet (rateDate, fetchedAt, source, fromCache, notice, ratesMap)
│   ├── ConversionResult.java      # Conversion output (amount, convertedAmount, unitRate, inverseRate, metadata)
│   ├── HistoricalRatePoint.java   # Single date+rate point for 7-day / 30-day series
│   ├── TrendDirection.java        # Enum: UP ("↑ Рост"), DOWN ("↓ Падение"), FLAT ("→ Без изменений")
│   └── PeriodStats.java           # Analytics DTO (periodDays, min, max, minDate, maxDate, average, absChange, pctChange, trend, points)
├── core/
│   ├── ValidationException.java   # Domain exception for invalid user input
│   ├── DataSourceException.java   # Domain exception for XML/network/empty data errors
│   ├── InputValidator.java        # Pure Java validator for amount strings & button readiness
│   ├── BnmXmlParser.java          # Pure Java DOM XML parser for BNM <ValCurs> payloads
│   ├── CurrencyConverter.java     # Pure Java BigDecimal conversion & cross-rate engine
│   └── TrendAnalyticsCalculator.java # Pure Java 7-day & 30-day statistics & trend calculator
├── data/
│   ├── HistoricalRatesCache.java  # Persistent JSON multi-date cache (filesDir/rates_cache.json + seed fallback)
│   └── BnmRateService.java        # HTTP client (HttpURLConnection) + 7-day holiday fallback + historical loader
└── ui/
    ├── MainActivity.java          # Android Activity managing Converter, 7-Day, and 30-Day screens
    └── RateTrendChartView.java    # Custom Android View (Canvas) rendering time-series line charts with touch inspection
```

---

## 2. Historical Data Strategy & Offline Resilience

1. **Multi-Date Local Cache (`HistoricalRatesCache`)**:
   - Stores a map of `DD.MM.YYYY -> RateSheet` along with `lastUpdatedDate` and `latestDate` inside a single JSON file (`rates_cache.json`).
   - On first launch (or if `filesDir/rates_cache.json` does not exist yet), `HistoricalRatesCache` initializes from bundled `assets/seed_rates_cache.json` (and `data/rates_cache.json` in CLI/test mode), which contains 35 consecutive days of official BNM exchange rates (`MDL`, `EUR`, `USD`, `RON`, `UAH`, `GBP`, `CHF`, `JPY`, `PLN`, `CZK`, `TRY`, `CNY`).
2. **Online Refresh with Graceful Offline Fallback (`BnmRateService`)**:
   - On startup or refresh, background threads fetch the requested date (and any missing recent dates for the 7-day / 30-day window) from `https://www.bnm.md/en/official_exchange_rates?get_xml=1&date=DD.MM.YYYY`.
   - Every fetched `RateSheet` is immediately merged into `rates_cache.json` on disk.
   - If the device is in Airplane Mode or `bnm.md` times out, `BnmRateService` transparently serves both current conversion and 7-day / 30-day comparison series from `HistoricalRatesCache`, tagging the result with `fromCache = true` and the exact cache freshness date.

---

## 3. Unit Testing Plan (`app/src/test/java/md/bnm/converter/`)

All unit tests run on the local JVM via `./gradlew test` (`gradlew.bat test` on Windows):
- `BnmXmlParserTest.java`: Valid XML parsing, `Nominal=100` (`JPY`), comma vs. dot decimal values, automatic `MDL` inclusion, empty `<ValCurs>` error, malformed XML error.
- `TrendAnalyticsCalculatorTest.java`: 7-day and 30-day statistics calculation (min, max, min/max dates, average, absolute delta, percentage delta), upward trend (`UP`), downward trend (`DOWN`), unchanged trend (`FLAT`), cross-currency series generation, and empty dataset error handling.
- `InputValidatorTest.java`: Valid integer/decimal amounts with `.` and `,`, button readiness (`isConvertReady`), rejection of empty, whitespace, zero, negative, alphabetic, double-dot, and overflow inputs.
- `CurrencyConverterAndCacheTest.java`: Forward, reverse, cross-currency, and identical currency (`USD -> USD`) conversions, persistent JSON cache save/load across restarts, and offline fallback behavior.
