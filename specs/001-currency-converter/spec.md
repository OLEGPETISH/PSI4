# Feature Specification: BNM Android Currency Converter & Historical Analytics

**Feature Branch**: `001-currency-converter-spec`  
**Created**: 2026-11-11  
**Status**: Approved  
**Target Platform**: Android (Native Java, API 24+ to API 36)  
**Previous Lab Stack**: Python 3.10 + Tkinter (Lab 3)  
**Current Lab Stack**: Java + Android Views / Custom Canvas Chart (Lab 4)  
**Data Source**: Official XML API of the National Bank of Moldova (BNM) — `https://www.bnm.md/en/official_exchange_rates?get_xml=1&date=DD.MM.YYYY`

---

## 1. Overview & Objectives

This project implements **Variant B** of Laboratory Work #4: migrating the National Bank of Moldova (BNM) currency converter from Lab 3 onto the **Android mobile platform** using a **completely different programming language (Java instead of Python)** without reusing any prior code, and extending its functionality with **two dedicated analytical comparison screens**:
1. **Weekly Comparison (Сравнение за неделю — 7 дней)**
2. **Monthly Comparison (Сравнение за месяц — 30 дней)**

---

## 2. User Stories & Acceptance Scenarios

### User Story 1 - Mobile Currency Conversion (Priority: P1)
As a mobile user, I want to convert any monetary amount between Moldovan Leu (`MDL`) and foreign currencies (`EUR`, `USD`, `RON`, `UAH`, `GBP`, `CHF`, `JPY`, etc.) in both directions so that I can quickly calculate official BNM exchange amounts on my Android device.

**Acceptance Scenarios**:
1. **Given** the Converter screen is open and the amount field is empty, **Then** the "Конвертировать" button is disabled (`enabled = false`).
2. **Given** the user enters `250.50` (or `250,50` with a comma), selects `EUR` as source and `MDL` as target, and taps "Конвертировать", **Then** the app calculates and displays the converted amount rounded to 2 decimal places along with direct (`1 EUR = X MDL`) and reverse (`1 MDL = Y EUR`) unit rates to 4 decimal places.
3. **Given** the user taps the swap button (`⇄`), **Then** source and target currencies are exchanged and conversion updates immediately.
4. **Given** source and target currencies are identical (`USD -> USD`), **Then** the app returns the exact input amount (`1 USD = 1.0000 USD`) without error.

---

### User Story 2 - Weekly Dynamics Comparison Screen (7 Days) (Priority: P1)
As a mobile user, I want to open the **"Неделя (7 дней)"** screen and inspect the exchange rate dynamics of a selected currency pair over the last 7 calendar days so that I can evaluate short-term exchange rate fluctuations.

**Acceptance Scenarios**:
1. **Given** the user navigates to the "Неделя (7 дн.)" tab and selects a currency pair (e.g., `EUR / MDL`), **Then** the screen displays:
   - A line chart (`RateTrendChartView`) plotting the rate across the last 7 days with labeled axes and highlighted min/max points.
   - **Minimum rate** over the 7-day period (and its date).
   - **Maximum rate** over the 7-day period (and its date).
   - **Average (mean) rate** over the 7-day period.
   - **Period change** in **absolute units** (e.g., `+0.1120 MDL`) and **percentage** (e.g., `+0.58%`).
   - **Explicit trend direction indicator**: `↑ Рост` (Growth), `↓ Падение` (Decline), or `→ Без изменений` (No change).

---

### User Story 3 - Monthly Dynamics Comparison Screen (30 Days) (Priority: P1)
As a mobile user, I want to open the **"Месяц (30 дней)"** screen and analyze the exchange rate dynamics of a selected currency pair over the last 30 calendar days so that I can understand medium-term currency trends.

**Acceptance Scenarios**:
1. **Given** the user navigates to the "Месяц (30 дн.)" tab and selects any currency pair (e.g., `USD / MDL` or cross-pair `EUR / USD`), **Then** the screen displays:
   - A 30-day time-series chart (`RateTrendChartView`) with interactive point inspection.
   - **Minimum** and **Maximum** values over the 30-day window with their dates.
   - **Average (mean)** value over the 30-day window.
   - **Absolute change** and **percentage change** between the start and end of the 30-day period.
   - **Explicit trend direction**: `↑ Рост`, `↓ Падение`, or `→ Без изменений`.
   - Daily historical rate list and data source / cache freshness label.

---

### User Story 4 - Offline Mode (Airplane Mode) & Persistent Historical Cache (Priority: P1)
As a user, if my phone has no internet connection (e.g., Airplane Mode during lab defense), the application must not crash; it must load the latest cached daily and historical rates from persistent storage, allow full use of the Converter, 7-day, and 30-day screens, and clearly show the effective cache date.

**Acceptance Scenarios**:
1. **Given** the device is switched to Airplane Mode and the app is restarted, **When** the user uses the Converter or switches to the 7-day and 30-day screens, **Then** the app operates smoothly using cached historical data (`rates_cache.json` / seeded 35-day history), shows an offline warning banner, and displays the exact cache date (`Локальный кэш НБМ • актуально на DD.MM.YYYY`).

---

### User Story 5 - Input Validation & Source Error Handling (Priority: P1)
As a user, if I enter invalid input (`-10`, `0`, `abc`, empty spaces) or if the external data source returns an empty or malformed response, the application must show a clear Russian warning message and never crash.

**Acceptance Scenarios**:
1. **Given** invalid amount strings (`""`, `"   "`, `"-25"`, `"0"`, `"0,00"`, `"abc"`, `"12..3"`), **When** validated, **Then** `InputValidator` rejects the input with an informative Russian error message.
2. **Given** an empty historical dataset or malformed XML payload, **When** parsed or analyzed, **Then** a controlled domain exception is raised and handled cleanly by the UI layer.

---

## 3. Functional Requirements

- **FR-001 (Three-Screen Android Navigation)**: Single-activity architecture (`MainActivity`) hosting three distinct screen views switched via top/bottom navigation tabs:
  1. **Конвертер** (Currency Converter)
  2. **Сравнение: 7 дней** (Weekly Analytics)
  3. **Сравнение: 30 дней** (Monthly Analytics)
- **FR-002 (BNM XML Parser)**: Parse `https://www.bnm.md/en/official_exchange_rates?get_xml=1&date=DD.MM.YYYY` using `javax.xml.parsers.DocumentBuilderFactory`, extracting `Date`, `CharCode`, `Nominal`, `Name`, and `Value`, and automatically injecting `MDL` (`Nominal=1`, `Value=1.0000`).
- **FR-003 (Cross-Currency Conversion Formula)**:
  $$\text{UnitRate}(C) = \frac{\text{Value}(C)}{\text{Nominal}(C)}$$
  $$\text{PairRate}(\text{Base}, \text{Quote}) = \frac{\text{UnitRate}(\text{Base})}{\text{UnitRate}(\text{Quote})}$$
  $$\text{ConvertedAmount} = \text{Amount} \times \text{PairRate}(\text{Source}, \text{Target})$$
- **FR-004 (Trend Statistics Engine)**:
  Given an ordered list of $N$ daily rate points $(d_1, r_1), \dots, (d_N, r_N)$ for period $P \in \{7, 30\}$:
  - $\text{Min} = \min_{1 \le i \le N} r_i$, $\text{Max} = \max_{1 \le i \le N} r_i$
  - $\text{Mean} = \frac{1}{N} \sum_{i=1}^{N} r_i$
  - $\Delta_{\text{abs}} = r_N - r_1$
  - $\Delta_{\%} = \frac{r_N - r_1}{r_1} \times 100\%$
  - Trend direction:
    - `UP` (`↑ Рост`) if $\Delta_{\text{abs}} > 0.00005$
    - `DOWN` (`↓ Падение`) if $\Delta_{\text{abs}} < -0.00005$
    - `FLAT` (`→ Без изменений`) otherwise.
- **FR-005 (Historical Data Strategy)**: Combine live BNM XML requests by date with a persistent multi-date local JSON cache (`rates_cache.json`) initialized from a 35-day official BNM historical dataset (`assets/seed_rates_cache.json` and `data/rates_cache.json`).
