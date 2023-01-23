package com.companydividend.service;

import com.companydividend.exception.impl.NoCompanyException;
import com.companydividend.model.Company;
import com.companydividend.model.Dividend;
import com.companydividend.model.ScrapedResult;
import com.companydividend.model.constants.CacheKey;
import com.companydividend.persist.CompanyRepository;
import com.companydividend.persist.DividendRepository;
import com.companydividend.persist.entity.CompanyEntity;
import com.companydividend.persist.entity.DividendEntity;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class FinanceService {

    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;


    @Cacheable(key = "#companyName", value = CacheKey.KEY_FINANCE)
    public ScrapedResult getDividendByCompanyName(String companyName) {
        log.info("Search company -> " + companyName);

        // 1.회사명을 기준으로 회사 정보를 조회
        CompanyEntity company = this.companyRepository.findByName(companyName)
                .orElseThrow(() -> new NoCompanyException());

        // 2. 조회된 회사 ID로 배당금을 조회
        List<DividendEntity> dividendEntities = this.dividendRepository.findAllByCompanyId(company.getId());

        // 3. 결과 조합 후 반환

        List<Dividend> dividends =
                dividendEntities.stream()
                        .map(e -> new Dividend(e.getDate(), e.getDividend()))
                        .collect(Collectors.toList());

        return new ScrapedResult(new Company(company.getTicker(), company.getName()), dividends);
    }

}
