package com.nicehash.exchange.client.domain.general;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nicehash.exchange.client.constant.ExchangeConstants;
import com.nicehash.external.ClientException;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Current exchange trading rules and symbol information.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExchangeInfo {

    private String timezone;

    private Long serverTime;

    private List<RateLimit> rateLimits;

    // private List<String> exchangeFilters;

    private List<SymbolInfo> symbols;

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Long getServerTime() {
        return serverTime;
    }

    public void setServerTime(Long serverTime) {
        this.serverTime = serverTime;
    }

    public List<RateLimit> getRateLimits() {
        return rateLimits;
    }

    public void setRateLimits(List<RateLimit> rateLimits) {
        this.rateLimits = rateLimits;
    }

    public List<SymbolInfo> getSymbols() {
        return symbols;
    }

    public void setSymbols(List<SymbolInfo> symbols) {
        this.symbols = symbols;
    }

    /**
     * @param symbol the symbol to obtain information for (e.g. ETHBTC)
     * @return symbol exchange information
     */
    public SymbolInfo getSymbolInfo(String symbol) {
        return symbols.stream().filter(symbolInfo -> symbolInfo.getSymbol().equals(symbol))
            .findFirst()
            .orElseThrow(() -> new ClientException("Unable to obtain information for symbol " + symbol, null));
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ExchangeConstants.TO_STRING_BUILDER_STYLE)
            .append("timezone", timezone)
            .append("serverTime", serverTime)
            .append("rateLimits", rateLimits)
            .append("symbols", symbols)
            .toString();
    }
}
