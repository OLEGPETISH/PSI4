package md.bnm.converter.model;

import java.math.BigDecimal;

/**
 * Immutable DTO containing the result of a currency conversion operation.
 */
public final class ConversionResult {
    private final BigDecimal amount;
    private final String fromCode;
    private final String toCode;
    private final BigDecimal convertedAmount;
    private final BigDecimal unitRate;
    private final BigDecimal inverseUnitRate;
    private final String effectiveDate;
    private final String source;
    private final boolean fromCache;
    private final String noticeMessage;

    public ConversionResult(
            BigDecimal amount,
            String fromCode,
            String toCode,
            BigDecimal convertedAmount,
            BigDecimal unitRate,
            BigDecimal inverseUnitRate,
            String effectiveDate,
            String source,
            boolean fromCache,
            String noticeMessage
    ) {
        this.amount = amount;
        this.fromCode = fromCode;
        this.toCode = toCode;
        this.convertedAmount = convertedAmount;
        this.unitRate = unitRate;
        this.inverseUnitRate = inverseUnitRate;
        this.effectiveDate = effectiveDate;
        this.source = source;
        this.fromCache = fromCache;
        this.noticeMessage = noticeMessage == null ? "" : noticeMessage;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getFromCode() {
        return fromCode;
    }

    public String getToCode() {
        return toCode;
    }

    public BigDecimal getConvertedAmount() {
        return convertedAmount;
    }

    public BigDecimal getUnitRate() {
        return unitRate;
    }

    public BigDecimal getInverseUnitRate() {
        return inverseUnitRate;
    }

    public String getEffectiveDate() {
        return effectiveDate;
    }

    public String getSource() {
        return source;
    }

    public boolean isFromCache() {
        return fromCache;
    }

    public String getNoticeMessage() {
        return noticeMessage;
    }
}
