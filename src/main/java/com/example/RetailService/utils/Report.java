package com.example.RetailService.utils;

import lombok.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Report {
    String userId;
    TransactionsStatus status;
    BigDecimal profitOrLossAmount;
    BigDecimal income;
    BigDecimal expenditure;
    Date fromDate;
    Date toDate;
    HashMap<String,BigDecimal> topBrands;
    HashMap<String,BigDecimal> topCategories;
    BigDecimal totalBuy;
    BigDecimal totalSell;
    BigDecimal totalBuyReturned;
    BigDecimal totalSellReturned;
    BigDecimal totalDispose;
}
