package md.bnm.converter.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Represents a complete daily rate sheet from BNM for a specific effective date.
 */
public final class RateSheet {
    private static final List<String> PRIORITY_CODES = Arrays.asList(
            "MDL", "EUR", "USD", "RON", "UAH", "GBP", "CHF", "JPY", "PLN", "CZK", "TRY", "CNY"
    );

    private final String effectiveDate;
    private final String requestedDate;
    private final String source;
    private final String savedAt;
    private final boolean fromCache;
    private final boolean fallbackDate;
    private final String noticeMessage;
    private final Map<String, CurrencyRate> rates;

    public RateSheet(
            String effectiveDate,
            String requestedDate,
            String source,
            String savedAt,
            boolean fromCache,
            boolean fallbackDate,
            String noticeMessage,
            List<CurrencyRate> rateList
    ) {
        this.effectiveDate = effectiveDate == null ? "" : effectiveDate.trim();
        this.requestedDate = (requestedDate == null || requestedDate.trim().isEmpty())
                ? this.effectiveDate
                : requestedDate.trim();
        this.source = (source == null || source.trim().isEmpty()) ? "НБМ (bnm.md)" : source.trim();
        this.savedAt = savedAt == null ? "" : savedAt.trim();
        this.fromCache = fromCache;
        this.fallbackDate = fallbackDate;
        this.noticeMessage = noticeMessage == null ? "" : noticeMessage.trim();

        Map<String, CurrencyRate> map = new LinkedHashMap<>();
        if (rateList != null) {
            for (CurrencyRate rate : rateList) {
                if (rate != null) {
                    map.put(rate.getCharCode(), rate);
                }
            }
        }
        this.rates = Collections.unmodifiableMap(map);
    }

    public String getEffectiveDate() {
        return effectiveDate;
    }

    public String getRequestedDate() {
        return requestedDate;
    }

    public String getSource() {
        return source;
    }

    public String getSavedAt() {
        return savedAt;
    }

    public boolean isFromCache() {
        return fromCache;
    }

    public boolean isFallbackDate() {
        return fallbackDate;
    }

    public String getNoticeMessage() {
        return noticeMessage;
    }

    public Map<String, CurrencyRate> getRatesMap() {
        return rates;
    }

    public List<CurrencyRate> getRatesList() {
        return new ArrayList<>(rates.values());
    }

    public CurrencyRate getRate(String charCode) {
        if (charCode == null) {
            return null;
        }
        return rates.get(charCode.trim().toUpperCase(Locale.ROOT));
    }

    public List<String> getSortedCurrencyCodes() {
        List<String> result = new ArrayList<>();
        for (String priority : PRIORITY_CODES) {
            if (rates.containsKey(priority)) {
                result.add(priority);
            }
        }
        List<String> others = new ArrayList<>();
        for (String code : rates.keySet()) {
            if (!result.contains(code)) {
                others.add(code);
            }
        }
        Collections.sort(others);
        result.addAll(others);
        return result;
    }

    public RateSheet withMetadata(
            String newRequestedDate,
            String newSource,
            boolean newFromCache,
            boolean newFallbackDate,
            String newNoticeMessage
    ) {
        return new RateSheet(
                this.effectiveDate,
                newRequestedDate,
                newSource,
                this.savedAt,
                newFromCache,
                newFallbackDate,
                newNoticeMessage,
                getRatesList()
        );
    }
}
