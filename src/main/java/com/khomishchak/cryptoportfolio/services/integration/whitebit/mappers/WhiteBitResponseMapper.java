package com.khomishchak.cryptoportfolio.services.integration.whitebit.mappers;

import com.khomishchak.cryptoportfolio.model.exchanger.trasaction.DepositWithdrawalTransaction;
import com.khomishchak.cryptoportfolio.model.TransactionType;
import com.khomishchak.cryptoportfolio.model.exchanger.Currency;
import com.khomishchak.cryptoportfolio.services.integration.whitebit.model.WhiteBitBalanceResp;
import com.khomishchak.cryptoportfolio.services.integration.whitebit.model.WhiteBitDepositWithdrawalHistoryResp;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class WhiteBitResponseMapper {

    public List<Currency> mapToCurrencies (WhiteBitBalanceResp resp) {
        List<Currency> result  = new ArrayList<>();

        for(Map.Entry<String, String> unit: resp.getCurrencies().entrySet()) {
            if(unit.getValue().equals("0")) {
                continue;
            }
            Currency currency = new Currency();
            currency.setCurrencyCode(unit.getKey());
            currency.setAmount(Double.parseDouble(unit.getValue()));

            result.add(currency);
        }

        return result;
    }

    public List<DepositWithdrawalTransaction> mapWithdrawalDepositHistoryToTransactions(WhiteBitDepositWithdrawalHistoryResp resp) {
        List<DepositWithdrawalTransaction> result = new ArrayList<>();
        resp.getRecords()
                .forEach(record -> {
                    DepositWithdrawalTransaction transaction = DepositWithdrawalTransaction.depositWithdrawalTransactionBuilder()
                            .transactionId(record.getTransactionId())
                            .transactionHash(record.getTransactionHash())
                            .createdAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(record.getCreatedAt() * 1000), ZoneId.systemDefault()))
                            .amount(BigDecimal.valueOf(record.getAmount()))
                            .ticker(record.getTicker())
                            .transactionType(record.getMethod() == 1 ? TransactionType.DEPOSIT : TransactionType.WITHDRAWAL)
                            .build();

                    result.add(transaction);
                });

        return result;
    }
}
