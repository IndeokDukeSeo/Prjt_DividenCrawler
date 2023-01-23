package com.companydividend.scrapper;

import com.companydividend.model.Company;
import com.companydividend.model.Dividend;
import com.companydividend.model.ScrapedResult;
import com.companydividend.model.constants.Month;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class YahooFinanceScrapper implements Scrapper{
    private static final String STATIC_URL = "https://finance.yahoo.com/quote/%s/history?period1=%d&period2=%d&interval=capitalGain%%7Cdiv%%7Csplit&filter=div&frequency=1mo&includeAdjustedClose=true";
    private static final String SUMMARY_URL ="https://finance.yahoo.com/quote/%s/?p=%s";
    private static final long START_TIME = 86400; // 60 * 60 * 24 **하

    @Override
    public ScrapedResult scrap(Company company) {
        var scrapResult = new ScrapedResult();
        scrapResult.setCompany(company);

        try {
            long now = System.currentTimeMillis() / 1000;

            String url = String.format(STATIC_URL, company.getTicker(), START_TIME, now);
            Connection connection = Jsoup.connect(url);
            connection.timeout(5000);
            Document document = connection.get();

            Elements parsingDivs = document.getElementsByAttributeValue("data-test", "historical-prices");
            Element tableElement = parsingDivs.get(0); //  테이블 전체

            Element tbody = tableElement.children().get(1);  //thead: 0 , tbody: 1 , tfoot:2

            List<Dividend> dividends = new ArrayList<>();
            for (Element e : tbody.children()) {
                String txt = e.text();

                String[] splits = txt.split(" ");
                int month = Month.strToNumber(splits[0]);
                int day = Integer.parseInt(splits[1].replace(",", ""));
                int year = Integer.parseInt(splits[2]);
                String dividend = splits[3];

                if (month < 0) {
                    throw new RuntimeException("Unexpected Month enum value-> " + splits[0]);
                }
                dividends.add(new Dividend(LocalDateTime.of(year, month, day, 0, 0), dividend));
            }
            scrapResult.setDividendEntities(dividends);
        } catch (IOException e) {
            //TODO
            e.printStackTrace();
        }
        return scrapResult;
    }

    @Override
    public Company scrapCompanyByTicker(String ticker) {
        String url = String.format(SUMMARY_URL, ticker, ticker);

        try {
            Document document = Jsoup.connect(url).get();
            Element titleElement = document.getElementsByTag("h1").get(0);
            String title = titleElement.text().split("-")[1].trim();

            return new Company(ticker,title);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


}
