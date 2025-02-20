/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.crtsh;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * {@code CrtShPageParser} parses the html page fetched from crt.sh into a {@link CrtShPage}
 */
@Service
public class CrtShPageParser {

    /**
     * {@code parse} parses the provided {@code html} and converts the html page
     * into a {@link CrtShPage}
     *
     * @param html {@code html} to parse
     * @return {@link CrtShPage}
     */
    public CrtShPage parse(String html) {

        return new CrtShPage(revokationTable(Jsoup.parse(html))
            .filter(ocspRow())
            .map(revokationInfo())
            .findFirst()
            .map(isRevoked())
            .orElseThrow(() -> new CrtShPageParsingException("no revokation info found")));
    }

    private static Stream<Element> revokationTable(Document page) {
        return page.select("tr:has(table.options)")
            .stream()
            .filter(row -> row.selectFirst("th:has(div.small)") != null)
            .map(row -> row.select("table.options tr"))
            .findFirst()
            .orElseThrow(() -> new CrtShPageParsingException("revokation table not found"))
            .stream();
    }

    private static Function<String, Boolean> isRevoked() {
        return "Revoked"::equals;
    }

    private static Function<Element, String> revokationInfo() {
        return row -> row.getElementsByTag("span").stream().findFirst()
            .map(Element::text)
            .orElseThrow(() -> new CrtShPageParsingException("could not extract revokation info"));
    }

    private static Predicate<Element> ocspRow() {
        return row -> row.select("td:first-child").text().equals("OCSP");
    }
}
