package md.bnm.converter.ui;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import md.bnm.converter.core.CurrencyConverter;
import md.bnm.converter.core.InputValidator;
import md.bnm.converter.data.BnmRateService;
import md.bnm.converter.data.HistoricalRatesCache;
import md.bnm.converter.model.ConversionResult;
import md.bnm.converter.model.CurrencyRate;
import md.bnm.converter.model.HistoricalRatePoint;
import md.bnm.converter.model.PeriodStats;
import md.bnm.converter.model.RateSheet;
import md.bnm.converter.model.TrendDirection;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main Android Activity written in Java implementing 3 distinct screens:
 * 1. Конвертер валют НБМ (Currency Converter)
 * 2. Сравнение за неделю (Weekly Analytics - 7 Days)
 * 3. Сравнение за месяц (Monthly Analytics - 30 Days)
 */
public class MainActivity extends Activity {
    private static final int TAB_CONVERTER = 0;
    private static final int TAB_WEEKLY = 1;
    private static final int TAB_MONTHLY = 2;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private BnmRateService rateService;
    private RateSheet currentRateSheet;
    private List<String> currencyCodes = new ArrayList<>(
            Arrays.asList("EUR", "USD", "MDL", "RON", "UAH", "GBP", "CHF", "JPY", "PLN", "CZK", "TRY", "CNY")
    );

    // Navigation & global status views
    private Button tabConverterBtn;
    private Button tabWeeklyBtn;
    private Button tabMonthlyBtn;
    private TextView globalStatusBanner;

    private View converterScreenView;
    private View weeklyScreenView;
    private View monthlyScreenView;

    // Screen 1: Converter views
    private EditText amountInput;
    private EditText dateInput;
    private Spinner fromCurrencySpinner;
    private Spinner toCurrencySpinner;
    private Button convertButton;
    private TextView resultHeadlineText;
    private TextView directRateText;
    private TextView reverseRateText;
    private TextView converterMetaText;

    // Screen 2: Weekly (7-Day) comparison views
    private Spinner weeklyBaseSpinner;
    private Spinner weeklyQuoteSpinner;
    private TextView weeklyTrendBadge;
    private TextView weeklyDeltaText;
    private RateTrendChartView weeklyChartView;
    private TextView weeklyMinText;
    private TextView weeklyMaxText;
    private TextView weeklyAvgText;
    private TextView weeklyChangeCardText;
    private TextView weeklyCacheMetaText;
    private LinearLayout weeklyHistoryList;

    // Screen 3: Monthly (30-Day) comparison views
    private Spinner monthlyBaseSpinner;
    private Spinner monthlyQuoteSpinner;
    private TextView monthlyTrendBadge;
    private TextView monthlyDeltaText;
    private RateTrendChartView monthlyChartView;
    private TextView monthlyMinText;
    private TextView monthlyMaxText;
    private TextView monthlyAvgText;
    private TextView monthlyChangeCardText;
    private TextView monthlyCacheMetaText;
    private LinearLayout monthlyHistoryList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        File cacheFile = new File(getFilesDir(), "rates_cache.json");
        HistoricalRatesCache cache = new HistoricalRatesCache(cacheFile);
        cache.ensureSeeded(LocalDate.now());
        this.rateService = new BnmRateService(cache);

        setContentView(buildRootLayout());
        selectTab(TAB_CONVERTER);
        loadInitialData(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private View buildRootLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#F1F5F9"));
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // Top App Bar
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setBackgroundColor(Color.parseColor("#1E3A8A"));
        header.setPadding(dp(18), dp(16), dp(18), dp(14));

        TextView title = new TextView(this);
        title.setText("Конвертер и аналитика валют НБМ");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Официальные курсы BNM (bnm.md) • Конвертация • Динамика 7 и 30 дней");
        subtitle.setTextColor(Color.parseColor("#DBEAFE"));
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        subtitle.setPadding(0, dp(3), 0, dp(10));
        header.addView(subtitle);

        // 3-Screen Navigation Bar
        LinearLayout navBar = new LinearLayout(this);
        navBar.setOrientation(LinearLayout.HORIZONTAL);
        navBar.setBackground(roundedRect("#172554", dp(10), 0, Color.TRANSPARENT));
        navBar.setPadding(dp(4), dp(4), dp(4), dp(4));

        tabConverterBtn = createNavTabButton("Конвертер", () -> selectTab(TAB_CONVERTER));
        tabWeeklyBtn = createNavTabButton("Неделя (7 дн.)", () -> selectTab(TAB_WEEKLY));
        tabMonthlyBtn = createNavTabButton("Месяц (30 дн.)", () -> selectTab(TAB_MONTHLY));

        LinearLayout.LayoutParams tabLp = new LinearLayout.LayoutParams(0, dp(38), 1f);
        navBar.addView(tabConverterBtn, tabLp);
        navBar.addView(tabWeeklyBtn, tabLp);
        navBar.addView(tabMonthlyBtn, tabLp);
        header.addView(navBar);

        root.addView(header);

        // Status / Offline / Validation Warning Banner
        globalStatusBanner = new TextView(this);
        globalStatusBanner.setVisibility(View.GONE);
        globalStatusBanner.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        globalStatusBanner.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams bannerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        bannerLp.setMargins(dp(14), dp(10), dp(14), 0);
        root.addView(globalStatusBanner, bannerLp);

        // Build the 3 Screens
        converterScreenView = buildConverterScreen();
        weeklyScreenView = buildAnalyticsScreen(true);
        monthlyScreenView = buildAnalyticsScreen(false);

        LinearLayout.LayoutParams screenLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        );
        root.addView(converterScreenView, screenLp);
        root.addView(weeklyScreenView, screenLp);
        root.addView(monthlyScreenView, screenLp);

        return root;
    }

    private View buildConverterScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(14), dp(12), dp(14), dp(24));

        // Input Card
        LinearLayout inputCard = createCard();

        TextView cardTitle = createSectionTitle("Параметры конвертации");
        inputCard.addView(cardTitle);

        // Amount label + input
        TextView amountLabel = createFieldLabel("Сумма для конвертации:");
        inputCard.addView(amountLabel);

        amountInput = new EditText(this);
        amountInput.setHint("Например: 100 или 250.50");
        amountInput.setText("100");
        amountInput.setInputType(InputType.TYPE_CLASS_TEXT);
        amountInput.setSingleLine(true);
        amountInput.setBackground(roundedRect("#F8FAFC", dp(8), dp(1), Color.parseColor("#CBD5E1")));
        amountInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        inputCard.addView(amountInput);

        // Quick amount presets
        HorizontalScrollView presetsScroll = new HorizontalScrollView(this);
        presetsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout presetsRow = new LinearLayout(this);
        presetsRow.setOrientation(LinearLayout.HORIZONTAL);
        presetsRow.setPadding(0, dp(8), 0, dp(6));

        for (String preset : new String[]{"50", "100", "250", "500", "1000", "5000"}) {
            Button chip = createChipButton(preset, () -> {
                amountInput.setText(preset);
                amountInput.setSelection(preset.length());
                performConversion();
            });
            LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(32)
            );
            chipLp.setMargins(0, 0, dp(6), 0);
            presetsRow.addView(chip, chipLp);
        }
        presetsScroll.addView(presetsRow);
        inputCard.addView(presetsScroll);

        // Currency Selectors Row (From -> Swap -> To)
        TextView pairLabel = createFieldLabel("Валютная пара (Исходная ⇄ Целевая):");
        inputCard.addView(pairLabel);

        LinearLayout pairRow = new LinearLayout(this);
        pairRow.setOrientation(LinearLayout.HORIZONTAL);
        pairRow.setGravity(Gravity.CENTER_VERTICAL);

        fromCurrencySpinner = createCurrencySpinner(0);
        toCurrencySpinner = createCurrencySpinner(2); // Default: EUR -> MDL

        Button swapBtn = new Button(this);
        swapBtn.setText("⇄");
        swapBtn.setAllCaps(false);
        swapBtn.setTextColor(Color.parseColor("#1E3A8A"));
        swapBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        swapBtn.setTypeface(Typeface.DEFAULT_BOLD);
        swapBtn.setBackground(roundedRect("#DBEAFE", dp(8), dp(1), Color.parseColor("#93C5FD")));
        swapBtn.setOnClickListener(v -> {
            int fromPos = fromCurrencySpinner.getSelectedItemPosition();
            int toPos = toCurrencySpinner.getSelectedItemPosition();
            fromCurrencySpinner.setSelection(toPos);
            toCurrencySpinner.setSelection(fromPos);
            performConversion();
        });

        LinearLayout.LayoutParams spinnerLp = new LinearLayout.LayoutParams(0, dp(44), 1f);
        LinearLayout.LayoutParams swapLp = new LinearLayout.LayoutParams(dp(48), dp(44));
        swapLp.setMargins(dp(8), 0, dp(8), 0);

        pairRow.addView(fromCurrencySpinner, spinnerLp);
        pairRow.addView(swapBtn, swapLp);
        pairRow.addView(toCurrencySpinner, spinnerLp);
        inputCard.addView(pairRow);

        // Date row + Refresh button
        TextView dateLabel = createFieldLabel("Дата официального курса НБМ (ДД.ММ.ГГГГ):");
        inputCard.addView(dateLabel);

        LinearLayout dateRow = new LinearLayout(this);
        dateRow.setOrientation(LinearLayout.HORIZONTAL);
        dateRow.setGravity(Gravity.CENTER_VERTICAL);

        dateInput = new EditText(this);
        dateInput.setText(InputValidator.formatDate(LocalDate.now()));
        dateInput.setSingleLine(true);
        dateInput.setBackground(roundedRect("#F8FAFC", dp(8), dp(1), Color.parseColor("#CBD5E1")));
        dateInput.setPadding(dp(12), dp(10), dp(12), dp(10));

        Button refreshBtn = new Button(this);
        refreshBtn.setText("Обновить");
        refreshBtn.setAllCaps(false);
        refreshBtn.setTextColor(Color.parseColor("#1E3A8A"));
        refreshBtn.setBackground(roundedRect("#EFF6FF", dp(8), dp(1), Color.parseColor("#93C5FD")));
        refreshBtn.setOnClickListener(v -> loadInitialData(true));

        LinearLayout.LayoutParams dateEditLp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        LinearLayout.LayoutParams refreshLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(42));
        refreshLp.setMargins(dp(8), 0, 0, 0);

        dateRow.addView(dateInput, dateEditLp);
        dateRow.addView(refreshBtn, refreshLp);
        inputCard.addView(dateRow);

        // Convert Button (disabled automatically when amount is empty)
        convertButton = new Button(this);
        convertButton.setText("Конвертировать");
        convertButton.setAllCaps(false);
        convertButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f);
        convertButton.setTypeface(Typeface.DEFAULT_BOLD);
        convertButton.setOnClickListener(v -> performConversion());

        LinearLayout.LayoutParams convertLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)
        );
        convertLp.setMargins(0, dp(14), 0, 0);
        inputCard.addView(convertButton, convertLp);

        amountInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateConvertButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        updateConvertButtonState();

        container.addView(inputCard);

        // Result Card
        LinearLayout resultCard = createCard();
        resultCard.addView(createSectionTitle("Результат расчёта"));

        resultHeadlineText = new TextView(this);
        resultHeadlineText.setText("—");
        resultHeadlineText.setTextColor(Color.parseColor("#0F172A"));
        resultHeadlineText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        resultHeadlineText.setTypeface(Typeface.DEFAULT_BOLD);
        resultHeadlineText.setPadding(0, dp(4), 0, dp(8));
        resultCard.addView(resultHeadlineText);

        directRateText = new TextView(this);
        directRateText.setTextColor(Color.parseColor("#334155"));
        directRateText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        resultCard.addView(directRateText);

        reverseRateText = new TextView(this);
        reverseRateText.setTextColor(Color.parseColor("#475569"));
        reverseRateText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        reverseRateText.setPadding(0, dp(2), 0, dp(10));
        resultCard.addView(reverseRateText);

        converterMetaText = new TextView(this);
        converterMetaText.setTextColor(Color.parseColor("#1E40AF"));
        converterMetaText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        converterMetaText.setBackground(roundedRect("#EFF6FF", dp(8), dp(1), Color.parseColor("#BFDBFE")));
        converterMetaText.setPadding(dp(10), dp(8), dp(10), dp(8));
        resultCard.addView(converterMetaText);

        container.addView(resultCard);
        scroll.addView(container);
        return scroll;
    }

    private View buildAnalyticsScreen(boolean isWeekly) {
        int periodDays = isWeekly ? 7 : 30;
        String screenTitle = isWeekly
                ? "Сравнение за неделю (последние 7 дней)"
                : "Сравнение за месяц (последние 30 дней)";

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(14), dp(12), dp(14), dp(24));

        // Top Selector & Trend Card
        LinearLayout headerCard = createCard();
        headerCard.addView(createSectionTitle(screenTitle));

        TextView pairHint = createFieldLabel("Выберите валютную пару для анализа динамики:");
        headerCard.addView(pairHint);

        LinearLayout pairRow = new LinearLayout(this);
        pairRow.setOrientation(LinearLayout.HORIZONTAL);
        pairRow.setGravity(Gravity.CENTER_VERTICAL);

        Spinner baseSpinner = createCurrencySpinner(isWeekly ? 0 : 1); // EUR on Weekly, USD on Monthly by default
        Spinner quoteSpinner = createCurrencySpinner(2);               // MDL

        Button swapBtn = new Button(this);
        swapBtn.setText("⇄");
        swapBtn.setAllCaps(false);
        swapBtn.setTextColor(Color.parseColor("#1E3A8A"));
        swapBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        swapBtn.setTypeface(Typeface.DEFAULT_BOLD);
        swapBtn.setBackground(roundedRect("#DBEAFE", dp(8), dp(1), Color.parseColor("#93C5FD")));
        swapBtn.setOnClickListener(v -> {
            int bPos = baseSpinner.getSelectedItemPosition();
            int qPos = quoteSpinner.getSelectedItemPosition();
            baseSpinner.setSelection(qPos);
            quoteSpinner.setSelection(bPos);
            refreshAnalyticsScreen(isWeekly);
        });

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshAnalyticsScreen(isWeekly);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        baseSpinner.setOnItemSelectedListener(listener);
        quoteSpinner.setOnItemSelectedListener(listener);

        LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        LinearLayout.LayoutParams swLp = new LinearLayout.LayoutParams(dp(46), dp(42));
        swLp.setMargins(dp(8), 0, dp(8), 0);

        pairRow.addView(baseSpinner, spLp);
        pairRow.addView(swapBtn, swLp);
        pairRow.addView(quoteSpinner, spLp);
        headerCard.addView(pairRow);

        // Trend Badge & Summary Row
        LinearLayout trendRow = new LinearLayout(this);
        trendRow.setOrientation(LinearLayout.HORIZONTAL);
        trendRow.setGravity(Gravity.CENTER_VERTICAL);
        trendRow.setPadding(0, dp(12), 0, dp(4));

        TextView trendBadge = new TextView(this);
        trendBadge.setText("→ Без изменений");
        trendBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        trendBadge.setTypeface(Typeface.DEFAULT_BOLD);
        trendBadge.setPadding(dp(12), dp(6), dp(12), dp(6));

        TextView deltaSummary = new TextView(this);
        deltaSummary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        deltaSummary.setTypeface(Typeface.DEFAULT_BOLD);
        deltaSummary.setTextColor(Color.parseColor("#0F172A"));
        deltaSummary.setPadding(dp(10), 0, 0, 0);

        trendRow.addView(trendBadge);
        trendRow.addView(deltaSummary);
        headerCard.addView(trendRow);

        container.addView(headerCard);

        // Chart Card
        LinearLayout chartCard = createCard();
        chartCard.addView(createSectionTitle("График изменения курса за " + periodDays + " дн."));

        RateTrendChartView chartView = new RateTrendChartView(this);
        LinearLayout.LayoutParams chartLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(220)
        );
        chartLp.setMargins(0, dp(6), 0, dp(4));
        chartCard.addView(chartView, chartLp);

        TextView chartHint = new TextView(this);
        chartHint.setText("Нажмите или проведите по графику для просмотра точного курса на выбранную дату");
        chartHint.setTextColor(Color.parseColor("#64748B"));
        chartHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        chartCard.addView(chartHint);

        container.addView(chartCard);

        // Statistical Metrics Grid (Min, Max, Average, Absolute & % Change)
        LinearLayout statsCard = createCard();
        statsCard.addView(createSectionTitle("Статистика за период (" + periodDays + " дн.)"));

        TextView minValText = createMetricRow(statsCard, "Минимальное значение:");
        TextView maxValText = createMetricRow(statsCard, "Максимальное значение:");
        TextView avgValText = createMetricRow(statsCard, "Среднее значение за период:");
        TextView changeValText = createMetricRow(statsCard, "Изменение (абс. и %):");

        TextView cacheMeta = new TextView(this);
        cacheMeta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        cacheMeta.setTextColor(Color.parseColor("#1E40AF"));
        cacheMeta.setBackground(roundedRect("#EFF6FF", dp(8), dp(1), Color.parseColor("#BFDBFE")));
        cacheMeta.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        metaLp.setMargins(0, dp(10), 0, 0);
        statsCard.addView(cacheMeta, metaLp);

        container.addView(statsCard);

        // Daily values breakdown card
        LinearLayout historyCard = createCard();
        historyCard.addView(createSectionTitle("Посуточная таблица значений (" + periodDays + " дн.)"));
        LinearLayout historyList = new LinearLayout(this);
        historyList.setOrientation(LinearLayout.VERTICAL);
        historyCard.addView(historyList);
        container.addView(historyCard);

        if (isWeekly) {
            weeklyBaseSpinner = baseSpinner;
            weeklyQuoteSpinner = quoteSpinner;
            weeklyTrendBadge = trendBadge;
            weeklyDeltaText = deltaSummary;
            weeklyChartView = chartView;
            weeklyMinText = minValText;
            weeklyMaxText = maxValText;
            weeklyAvgText = avgValText;
            weeklyChangeCardText = changeValText;
            weeklyCacheMetaText = cacheMeta;
            weeklyHistoryList = historyList;
        } else {
            monthlyBaseSpinner = baseSpinner;
            monthlyQuoteSpinner = quoteSpinner;
            monthlyTrendBadge = trendBadge;
            monthlyDeltaText = deltaSummary;
            monthlyChartView = chartView;
            monthlyMinText = minValText;
            monthlyMaxText = maxValText;
            monthlyAvgText = avgValText;
            monthlyChangeCardText = changeValText;
            monthlyCacheMetaText = cacheMeta;
            monthlyHistoryList = historyList;
        }

        scroll.addView(container);
        return scroll;
    }

    private void selectTab(int tabIndex) {
        converterScreenView.setVisibility(tabIndex == TAB_CONVERTER ? View.VISIBLE : View.GONE);
        weeklyScreenView.setVisibility(tabIndex == TAB_WEEKLY ? View.VISIBLE : View.GONE);
        monthlyScreenView.setVisibility(tabIndex == TAB_MONTHLY ? View.VISIBLE : View.GONE);

        styleTabButton(tabConverterBtn, tabIndex == TAB_CONVERTER);
        styleTabButton(tabWeeklyBtn, tabIndex == TAB_WEEKLY);
        styleTabButton(tabMonthlyBtn, tabIndex == TAB_MONTHLY);

        if (tabIndex == TAB_WEEKLY) {
            refreshAnalyticsScreen(true);
        } else if (tabIndex == TAB_MONTHLY) {
            refreshAnalyticsScreen(false);
        }
    }

    private void loadInitialData(boolean forceNetwork) {
        String requestedDate = dateInput.getText().toString().trim();
        executor.execute(() -> {
            try {
                RateSheet sheet = rateService.getRateSheet(requestedDate, forceNetwork);
                mainHandler.post(() -> {
                    currentRateSheet = sheet;
                    updateCurrencySpinnersIfNeeded(sheet);
                    performConversion();
                    refreshAnalyticsScreen(true);
                    refreshAnalyticsScreen(false);
                });
            } catch (Exception e) {
                mainHandler.post(() -> showBanner(e.getMessage(), true));
            }
        });
    }

    private void performConversion() {
        updateConvertButtonState();
        if (currentRateSheet == null) {
            return;
        }

        String rawAmount = amountInput.getText().toString();
        String fromCode = getSelectedCode(fromCurrencySpinner, "EUR");
        String toCode = getSelectedCode(toCurrencySpinner, "MDL");

        try {
            ConversionResult result = CurrencyConverter.convert(rawAmount, fromCode, toCode, currentRateSheet);
            resultHeadlineText.setText(String.format(
                    Locale.US,
                    "%s %s = %s %s",
                    result.getAmount().toPlainString(),
                    result.getFromCode(),
                    result.getConvertedAmount().toPlainString(),
                    result.getToCode()
            ));
            directRateText.setText(String.format(
                    Locale.US,
                    "Прямой курс: 1 %s = %s %s",
                    result.getFromCode(),
                    result.getUnitRate().toPlainString(),
                    result.getToCode()
            ));
            reverseRateText.setText(String.format(
                    Locale.US,
                    "Обратный курс: 1 %s = %s %s",
                    result.getToCode(),
                    result.getInverseUnitRate().toPlainString(),
                    result.getFromCode()
            ));
            converterMetaText.setText(
                    "Дата актуальности курса: " + result.getEffectiveDate()
                            + "\nИсточник данных: " + result.getSource()
            );

            if (!result.getNoticeMessage().isEmpty()) {
                showBanner(result.getNoticeMessage(), result.isFromCache());
            } else {
                globalStatusBanner.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            showBanner(e.getMessage(), true);
        }
    }

    private void refreshAnalyticsScreen(boolean isWeekly) {
        int periodDays = isWeekly ? 7 : 30;
        Spinner baseSp = isWeekly ? weeklyBaseSpinner : monthlyBaseSpinner;
        Spinner quoteSp = isWeekly ? weeklyQuoteSpinner : monthlyQuoteSpinner;

        String baseCode = getSelectedCode(baseSp, isWeekly ? "EUR" : "USD");
        String quoteCode = getSelectedCode(quoteSp, "MDL");
        String anchorDate = dateInput != null ? dateInput.getText().toString().trim() : "";

        executor.execute(() -> {
            try {
                PeriodStats stats = rateService.getPeriodAnalytics(baseCode, quoteCode, periodDays, anchorDate);
                mainHandler.post(() -> bindPeriodStats(stats, isWeekly));
            } catch (Exception e) {
                mainHandler.post(() -> showBanner(e.getMessage(), true));
            }
        });
    }

    private void bindPeriodStats(PeriodStats stats, boolean isWeekly) {
        TextView trendBadge = isWeekly ? weeklyTrendBadge : monthlyTrendBadge;
        TextView deltaText = isWeekly ? weeklyDeltaText : monthlyDeltaText;
        RateTrendChartView chartView = isWeekly ? weeklyChartView : monthlyChartView;
        TextView minText = isWeekly ? weeklyMinText : monthlyMinText;
        TextView maxText = isWeekly ? weeklyMaxText : monthlyMaxText;
        TextView avgText = isWeekly ? weeklyAvgText : monthlyAvgText;
        TextView changeCardText = isWeekly ? weeklyChangeCardText : monthlyChangeCardText;
        TextView cacheMetaText = isWeekly ? weeklyCacheMetaText : monthlyCacheMetaText;
        LinearLayout historyList = isWeekly ? weeklyHistoryList : monthlyHistoryList;

        TrendDirection dir = stats.getTrendDirection();
        trendBadge.setText(dir.getDisplayBadge());
        if (dir == TrendDirection.UP) {
            trendBadge.setTextColor(Color.parseColor("#15803D"));
            trendBadge.setBackground(roundedRect("#DCFCE7", dp(8), dp(1), Color.parseColor("#86EFAC")));
        } else if (dir == TrendDirection.DOWN) {
            trendBadge.setTextColor(Color.parseColor("#B91C1C"));
            trendBadge.setBackground(roundedRect("#FEE2E2", dp(8), dp(1), Color.parseColor("#FCA5A5")));
        } else {
            trendBadge.setTextColor(Color.parseColor("#334155"));
            trendBadge.setBackground(roundedRect("#E2E8F0", dp(8), dp(1), Color.parseColor("#CBD5E1")));
        }

        String sign = stats.getAbsoluteChange().compareTo(BigDecimal.ZERO) > 0 ? "+" : "";
        String changeFormatted = String.format(
                Locale.US,
                "%s%s %s (%s%s%%)",
                sign,
                stats.getAbsoluteChange().toPlainString(),
                stats.getQuoteCode(),
                sign,
                stats.getPercentChange().toPlainString()
        );
        deltaText.setText(changeFormatted);

        chartView.setStats(stats);

        minText.setText(stats.getMinRate().toPlainString() + " " + stats.getQuoteCode()
                + " (" + stats.getMinDate() + ")");
        maxText.setText(stats.getMaxRate().toPlainString() + " " + stats.getQuoteCode()
                + " (" + stats.getMaxDate() + ")");
        avgText.setText(stats.getAverageRate().toPlainString() + " " + stats.getQuoteCode());
        changeCardText.setText(changeFormatted + " • " + dir.getDisplayBadge());

        cacheMetaText.setText(
                "Период: " + stats.getStartDate() + " — " + stats.getEndDate()
                        + " (" + stats.getPeriodDays() + " дн.)\n"
                        + "Дата актуальности данных: " + stats.getEffectiveCacheDate()
                        + " • Источник: " + stats.getSourceLabel()
        );

        // Populate daily breakdown list (most recent first)
        historyList.removeAllViews();
        List<HistoricalRatePoint> pts = stats.getPoints();
        for (int i = pts.size() - 1; i >= 0; i--) {
            HistoricalRatePoint pt = pts.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(dp(4), dp(6), dp(4), dp(6));

            TextView dView = new TextView(this);
            dView.setText(pt.getDate());
            dView.setTextColor(Color.parseColor("#475569"));
            dView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);

            TextView rView = new TextView(this);
            rView.setText("1 " + stats.getBaseCode() + " = " + pt.getRate().toPlainString() + " " + stats.getQuoteCode());
            rView.setTextColor(Color.parseColor("#0F172A"));
            rView.setTypeface(Typeface.DEFAULT_BOLD);
            rView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
            rView.setGravity(Gravity.END);

            row.addView(dView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(rView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.3f));
            historyList.addView(row);
        }
    }

    private void updateConvertButtonState() {
        if (convertButton == null || amountInput == null) {
            return;
        }
        String rawAmount = amountInput.getText().toString();
        String fromCode = getSelectedCode(fromCurrencySpinner, "EUR");
        String toCode = getSelectedCode(toCurrencySpinner, "MDL");
        boolean ready = InputValidator.isConvertReady(rawAmount, fromCode, toCode);

        convertButton.setEnabled(ready);
        if (ready) {
            convertButton.setTextColor(Color.WHITE);
            convertButton.setBackground(roundedRect("#2563EB", dp(10), 0, Color.TRANSPARENT));
        } else {
            convertButton.setTextColor(Color.parseColor("#94A3B8"));
            convertButton.setBackground(roundedRect("#E2E8F0", dp(10), 0, Color.TRANSPARENT));
        }
    }

    private void updateCurrencySpinnersIfNeeded(RateSheet sheet) {
        if (sheet == null) {
            return;
        }
        List<String> newCodes = sheet.getSortedCurrencyCodes();
        if (newCodes.isEmpty() || newCodes.equals(currencyCodes)) {
            return;
        }
        currencyCodes = newCodes;
        refreshSpinnerAdapter(fromCurrencySpinner, "EUR");
        refreshSpinnerAdapter(toCurrencySpinner, "MDL");
        refreshSpinnerAdapter(weeklyBaseSpinner, "EUR");
        refreshSpinnerAdapter(weeklyQuoteSpinner, "MDL");
        refreshSpinnerAdapter(monthlyBaseSpinner, "USD");
        refreshSpinnerAdapter(monthlyQuoteSpinner, "MDL");
    }

    private void refreshSpinnerAdapter(Spinner spinner, String defaultCode) {
        if (spinner == null) {
            return;
        }
        String current = getSelectedCode(spinner, defaultCode);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                buildSpinnerLabels()
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        int idx = currencyCodes.indexOf(current);
        if (idx >= 0) {
            spinner.setSelection(idx);
        }
    }

    private List<String> buildSpinnerLabels() {
        List<String> labels = new ArrayList<>(currencyCodes.size());
        for (String code : currencyCodes) {
            CurrencyRate r = currentRateSheet != null ? currentRateSheet.getRate(code) : null;
            if (r != null) {
                labels.add(code + " — " + r.getName());
            } else {
                labels.add(code);
            }
        }
        return labels;
    }

    private String getSelectedCode(Spinner spinner, String fallback) {
        if (spinner == null || spinner.getSelectedItemPosition() < 0
                || spinner.getSelectedItemPosition() >= currencyCodes.size()) {
            return fallback;
        }
        return currencyCodes.get(spinner.getSelectedItemPosition());
    }

    private void showBanner(String message, boolean isWarningOrError) {
        if (message == null || message.trim().isEmpty()) {
            globalStatusBanner.setVisibility(View.GONE);
            return;
        }
        globalStatusBanner.setText(message);
        if (isWarningOrError) {
            globalStatusBanner.setTextColor(Color.parseColor("#9A3412"));
            globalStatusBanner.setBackground(roundedRect("#FFEDD5", dp(8), dp(1), Color.parseColor("#FDBA74")));
        } else {
            globalStatusBanner.setTextColor(Color.parseColor("#1E40AF"));
            globalStatusBanner.setBackground(roundedRect("#DBEAFE", dp(8), dp(1), Color.parseColor("#93C5FD")));
        }
        globalStatusBanner.setVisibility(View.VISIBLE);
    }

    private Spinner createCurrencySpinner(int initialSelection) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                buildSpinnerLabels()
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        if (initialSelection >= 0 && initialSelection < currencyCodes.size()) {
            spinner.setSelection(initialSelection);
        }
        spinner.setBackground(roundedRect("#F8FAFC", dp(8), dp(1), Color.parseColor("#CBD5E1")));
        spinner.setPadding(dp(8), dp(4), dp(8), dp(4));
        return spinner;
    }

    private Button createNavTabButton(String title, Runnable onClick) {
        Button btn = new Button(this);
        btn.setText(title);
        btn.setAllCaps(false);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setOnClickListener(v -> onClick.run());
        return btn;
    }

    private void styleTabButton(Button btn, boolean active) {
        if (active) {
            btn.setTextColor(Color.parseColor("#1E3A8A"));
            btn.setBackground(roundedRect("#FFFFFF", dp(8), 0, Color.TRANSPARENT));
        } else {
            btn.setTextColor(Color.parseColor("#BFDBFE"));
            btn.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private Button createChipButton(String text, Runnable onClick) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setAllCaps(false);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        btn.setTextColor(Color.parseColor("#1E40AF"));
        btn.setPadding(dp(10), 0, dp(10), 0);
        btn.setBackground(roundedRect("#EFF6FF", dp(16), dp(1), Color.parseColor("#BFDBFE")));
        btn.setOnClickListener(v -> onClick.run());
        return btn;
    }

    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(roundedRect("#FFFFFF", dp(12), dp(1), Color.parseColor("#E2E8F0")));
        card.setPadding(dp(16), dp(14), dp(16), dp(16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(lp);
        return card;
    }

    private TextView createSectionTitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#0F172A"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(0, 0, 0, dp(8));
        return tv;
    }

    private TextView createFieldLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#475569"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        tv.setPadding(0, dp(8), 0, dp(4));
        return tv;
    }

    private TextView createMetricRow(LinearLayout parent, String label) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(5), 0, dp(5));

        TextView labelTv = new TextView(this);
        labelTv.setText(label);
        labelTv.setTextColor(Color.parseColor("#475569"));
        labelTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);

        TextView valueTv = new TextView(this);
        valueTv.setText("—");
        valueTv.setTextColor(Color.parseColor("#0F172A"));
        valueTv.setTypeface(Typeface.DEFAULT_BOLD);
        valueTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        valueTv.setGravity(Gravity.END);

        row.addView(labelTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.1f));
        row.addView(valueTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f));
        parent.addView(row);
        return valueTv;
    }

    private static GradientDrawable roundedRect(String hexColor, int radiusPx, int strokeWidthPx, int strokeColor) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setColor(Color.parseColor(hexColor));
        d.setCornerRadius(radiusPx);
        if (strokeWidthPx > 0) {
            d.setStroke(strokeWidthPx, strokeColor);
        }
        return d;
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()
        ));
    }
}
