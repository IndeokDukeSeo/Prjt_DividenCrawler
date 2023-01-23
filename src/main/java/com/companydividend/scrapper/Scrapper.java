package com.companydividend.scrapper;

import com.companydividend.model.Company;
import com.companydividend.model.ScrapedResult;

public interface Scrapper {

    Company scrapCompanyByTicker(String ticker);
    ScrapedResult scrap(Company company);
}
