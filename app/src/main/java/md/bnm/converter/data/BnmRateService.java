package md.bnm.converter.data;

import md.bnm.converter.core.BnmXmlParser;
import md.bnm.converter.core.DataSourceException;
import md.bnm.converter.core.InputValidator;
import md.bnm.converter.core.TrendAnalyticsCalculator;
import md.bnm.converter.model.PeriodStats;
import md.bnm.converter.model.RateSheet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service coordinating online BNM XML requests, weekend/holiday fallback resolution,
 * and persistent offline cache fallback for both daily conversions and 7/30-day analytics.
 */
public class BnmRateService {
    public static final String BNM_XML_ENDPOINT = "https://www.bnm.md/en/official_exchange_rates?get_xml=1&date=";
    private static final int MAX_FALLBACK_DAYS = 7;
    private static final int CONNECT_TIMEOUT_MS = 4500;
    private static final int READ_TIMEOUT_MS = 4500;

    public interface XmlTransport {
        String fetchXml(String dateStr) throws IOException;
    }

    private final HistoricalRatesCache cache;
    private final XmlTransport transport;

    public BnmRateService(HistoricalRatesCache cache) {
        this(cache, BnmRateService::defaultHttpFetch);
    }

    public BnmRateService(HistoricalRatesCache cache, XmlTransport transport) {
        if (cache == null) {
            throw new IllegalArgumentException("HistoricalRatesCache не может быть null.");
        }
        this.cache = cache;
        this.transport = (transport != null) ? transport : BnmRateService::defaultHttpFetch;
    }

    public HistoricalRatesCache getCache() {
        return cache;
    }

    /**
     * Fetches the official BNM RateSheet for requestedDateStr (format DD.MM.YYYY).
     * Automatically steps back up to 7 days if requestedDate falls on a weekend/holiday with empty XML.
     * Falls back to local persistent cache if offline or if BNM server is unreachable.
     */
    public RateSheet getRateSheet(String requestedDateStr, boolean forceNetwork) {
        LocalDate requestedDate = (requestedDateStr == null || requestedDateStr.trim().isEmpty())
                ? LocalDate.now()
                : InputValidator.validateDate(requestedDateStr);
        String formattedRequested = InputValidator.formatDate(requestedDate);

        if (!forceNetwork) {
            RateSheet cachedExact = cache.loadExactDate(formattedRequested);
            if (cachedExact != null) {
                return cachedExact;
            }
        }

        try {
            RateSheet liveSheet = fetchWithHolidayFallback(requestedDate);
            cache.saveSheet(liveSheet);
            return liveSheet;
        } catch (Exception networkOrParseError) {
            RateSheet fallbackCached = cache.loadSheetOrLatest(formattedRequested);
            if (fallbackCached != null) {
                String offlineNotice = "Отсутствует подключение к сети (режим офлайн). "
                        + "Загружены сохранённые курсы из локального кэша (актуально на "
                        + fallbackCached.getEffectiveDate() + ").";
                return fallbackCached.withMetadata(
                        formattedRequested,
                        "Локальный кэш НБМ (" + fallbackCached.getEffectiveDate() + ")",
                        true,
                        fallbackCached.isFallbackDate(),
                        offlineNotice
                );
            }
            throw new DataSourceException(
                    "Не удалось получить курсы валют: сервер НБМ недоступен, а локальный кэш пуст. "
                            + "Проверьте подключение к интернету.",
                    false,
                    networkOrParseError
            );
        }
    }

    /**
     * Builds the 7-day or 30-day PeriodStats for the selected (baseCode / quoteCode) pair.
     * Attempts to refresh the latest date from BNM first; if offline, seamlessly computes analytics
     * from the persistent multi-date local cache.
     */
    public PeriodStats getPeriodAnalytics(String baseCode, String quoteCode, int periodDays, String anchorDateStr) {
        LocalDate anchorDate = (anchorDateStr == null || anchorDateStr.trim().isEmpty())
                ? LocalDate.now()
                : InputValidator.validateDate(anchorDateStr);

        boolean usedOfflineCache = false;
        String effectiveCacheDate = InputValidator.formatDate(anchorDate);

        // Ensure multi-date historical cache is seeded
        cache.ensureSeeded(anchorDate);

        // Try refreshing the latest day and key recent points online
        try {
            RateSheet latestOnline = fetchWithHolidayFallback(anchorDate);
            cache.saveSheet(latestOnline);
            effectiveCacheDate = latestOnline.getEffectiveDate();
        } catch (Exception e) {
            usedOfflineCache = true;
            String latestCached = cache.getLatestEffectiveDate();
            if (!latestCached.isEmpty()) {
                effectiveCacheDate = latestCached;
            }
        }

        List<RateSheet> historySheets = cache.loadHistoryWindow(anchorDate, periodDays);
        if (historySheets.isEmpty()) {
            throw new DataSourceException("Нет исторических данных для построения графика за " + periodDays + " дн.", true);
        }

        String sourceLabel = usedOfflineCache
                ? "Локальный кэш НБМ (актуально на " + effectiveCacheDate + ")"
                : "Официальные данные НБМ + локальная история (" + effectiveCacheDate + ")";

        return TrendAnalyticsCalculator.analyzeFromSheets(
                historySheets,
                baseCode,
                quoteCode,
                periodDays,
                effectiveCacheDate,
                usedOfflineCache,
                sourceLabel
        );
    }

    private RateSheet fetchWithHolidayFallback(LocalDate requestedDate) throws IOException {
        String originalDateStr = InputValidator.formatDate(requestedDate);
        DataSourceException lastEmptyError = null;

        for (int offset = 0; offset <= MAX_FALLBACK_DAYS; offset++) {
            LocalDate candidateDate = requestedDate.minusDays(offset);
            String candidateStr = InputValidator.formatDate(candidateDate);
            String xml = transport.fetchXml(candidateStr);

            try {
                RateSheet parsed = BnmXmlParser.parseXml(xml, originalDateStr);
                if (offset > 0 && !parsed.isFallbackDate()) {
                    String notice = "На дату " + originalDateStr + " официальный курс не публиковался. "
                            + "Показан ближайший доступный курс НБМ за " + parsed.getEffectiveDate() + ".";
                    return parsed.withMetadata(
                            originalDateStr,
                            parsed.getSource(),
                            false,
                            true,
                            notice
                    );
                }
                return parsed;
            } catch (DataSourceException e) {
                if (e.isEmptyResponse()) {
                    lastEmptyError = e;
                    continue;
                }
                throw e;
            }
        }

        if (lastEmptyError != null) {
            throw lastEmptyError;
        }
        throw new DataSourceException("Не удалось получить курсы НБМ за дату " + originalDateStr + ".");
    }

    private static String defaultHttpFetch(String dateStr) throws IOException {
        URL url = new URL(BNM_XML_ENDPOINT + dateStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", "BNM-Android-Converter-Lab4/1.0 (Java)");
        conn.setRequestProperty("Accept", "application/xml, text/xml, */*");

        try {
            int status = conn.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("Сервер НБМ вернул HTTP-код " + status);
            }
            try (InputStream in = conn.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                return sb.toString();
            }
        } finally {
            conn.disconnect();
        }
    }
}
