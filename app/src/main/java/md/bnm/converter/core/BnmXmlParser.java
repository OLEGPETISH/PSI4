package md.bnm.converter.core;

import md.bnm.converter.model.CurrencyRate;
import md.bnm.converter.model.RateSheet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure Java XML parser for official National Bank of Moldova (BNM) exchange rate payloads.
 */
public final class BnmXmlParser {

    private BnmXmlParser() {
    }

    public static RateSheet parseXml(String xmlContent, String requestedDate) {
        if (xmlContent == null || xmlContent.trim().isEmpty()) {
            throw new DataSourceException("Получен пустой ответ от сервера НБМ.", true);
        }

        Document document;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(new InputSource(new StringReader(xmlContent.trim())));
        } catch (Exception e) {
            throw new DataSourceException("Ошибка разбора XML-ответа НБМ: некорректный формат данных.", false, e);
        }

        Element root = document.getDocumentElement();
        if (root == null || !"ValCurs".equalsIgnoreCase(root.getTagName())) {
            throw new DataSourceException("Некорректная структура XML: отсутствует корневой элемент <ValCurs>.");
        }

        String effectiveDate = root.getAttribute("Date");
        if (effectiveDate != null) {
            effectiveDate = effectiveDate.trim();
        }
        String normalizedRequested = (requestedDate == null || requestedDate.trim().isEmpty())
                ? effectiveDate
                : requestedDate.trim();
        if (effectiveDate == null || effectiveDate.isEmpty()) {
            effectiveDate = normalizedRequested != null ? normalizedRequested : "";
        }

        NodeList valuteNodes = root.getElementsByTagName("Valute");
        if (valuteNodes == null || valuteNodes.getLength() == 0) {
            throw new DataSourceException(
                    "На дату " + (normalizedRequested != null ? normalizedRequested : effectiveDate)
                            + " курсы валют в ответе НБМ отсутствуют (выходной или праздничный день).",
                    true
            );
        }

        List<CurrencyRate> parsedRates = new ArrayList<>();
        boolean hasMdl = false;

        for (int i = 0; i < valuteNodes.getLength(); i++) {
            if (!(valuteNodes.item(i) instanceof Element)) {
                continue;
            }
            Element valuteEl = (Element) valuteNodes.item(i);
            String charCode = getTagText(valuteEl, "CharCode");
            String nominalStr = getTagText(valuteEl, "Nominal");
            String name = getTagText(valuteEl, "Name");
            String valueStr = getTagText(valuteEl, "Value");
            String numCode = getTagText(valuteEl, "NumCode");

            if (charCode.isEmpty() || nominalStr.isEmpty() || valueStr.isEmpty()) {
                continue;
            }

            int nominal;
            BigDecimal value;
            try {
                nominal = Integer.parseInt(nominalStr.trim());
                value = new BigDecimal(valueStr.trim().replace(',', '.'));
            } catch (NumberFormatException e) {
                throw new DataSourceException(
                        "Некорректное числовое значение курса для валюты " + charCode + " в XML НБМ.",
                        false,
                        e
                );
            }

            if (nominal <= 0 || value.compareTo(BigDecimal.ZERO) <= 0) {
                throw new DataSourceException(
                        "Недопустимый номинал или неположительный курс для валюты " + charCode + "."
                );
            }

            CurrencyRate rate = new CurrencyRate(charCode, nominal, name, value, numCode);
            if ("MDL".equals(rate.getCharCode())) {
                hasMdl = true;
            }
            parsedRates.add(rate);
        }

        if (parsedRates.isEmpty()) {
            throw new DataSourceException("В ответе НБМ не найдено ни одной валидной котировки валют.", true);
        }

        if (!hasMdl) {
            parsedRates.add(new CurrencyRate(
                    "MDL",
                    1,
                    "Молдавский лей (Moldovan Leu)",
                    new BigDecimal("1.0000"),
                    "498"
            ));
        }

        boolean isFallback = normalizedRequested != null
                && !normalizedRequested.isEmpty()
                && !effectiveDate.isEmpty()
                && !normalizedRequested.equals(effectiveDate);

        String notice = isFallback
                ? "На дату " + normalizedRequested + " официальный курс не устанавливался (выходной/праздник). "
                + "Используется курс НБМ за ближайший рабочий день: " + effectiveDate + "."
                : "";

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return new RateSheet(
                effectiveDate,
                normalizedRequested,
                "НБМ (bnm.md)",
                timestamp,
                false,
                isFallback,
                notice,
                parsedRates
        );
    }

    private static String getTagText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes == null || nodes.getLength() == 0 || nodes.item(0) == null) {
            return "";
        }
        String text = nodes.item(0).getTextContent();
        return text == null ? "" : text.trim();
    }
}
