package com.companydividend.Scheduler;

import com.companydividend.model.Company;
import com.companydividend.model.ScrapedResult;
import com.companydividend.model.constants.CacheKey;
import com.companydividend.persist.CompanyRepository;
import com.companydividend.persist.DividendRepository;
import com.companydividend.persist.entity.CompanyEntity;
import com.companydividend.persist.entity.DividendEntity;
import com.companydividend.scrapper.Scrapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@EnableCaching
@AllArgsConstructor
public class ScraperScheduler {

    private final CompanyRepository companyRepository;
    private final Scrapper yahooFinanceScrapper;
    private DividendRepository dividendRepository;

    @CacheEvict(value = CacheKey.KEY_FINANCE, allEntries = true)
    @Scheduled(cron = "${scheduler.scrap.yahoo}")
    public void yahooFinanceScheduling() {
        log.info("scraping scheduler has started");
        // 회사 목록 조회
        List<CompanyEntity> companies = this.companyRepository.findAll();

        // 업데이트된 배당금 정보 스크래핑
        for (CompanyEntity company : companies) {
            log.info("scraping scheduler has started -> " + company.getName());

            ScrapedResult scrapedResult = this.yahooFinanceScrapper.scrap(
                    new Company(company.getTicker(), company.getName())
            );

            // 스크래핑한 정보 중 없는 값을 저장
            scrapedResult.getDividendEntities().stream()
                    .map(e -> new DividendEntity(company.getId(), e))
                    .forEach(e ->
                    {
                        boolean exists = this.dividendRepository.existsByCompanyIdAndDate(e.getCompanyId(), e.getDate());
                        if (!exists) {
                            this.dividendRepository.save(e);
                        }
                    });

            //스크래핑 연속요청이 되지 않도록 일시정지
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
