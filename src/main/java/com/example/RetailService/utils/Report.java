package com.example.RetailService.utils;

import lombok.*;

import java.util.Date;
import java.util.HashMap;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Report {
    String userId;
    String profitOrLoss;
    Double profitOrLossAmount;
    Double income;
    Double expenditure;
    Date fromDate;
    Date toDate;
    HashMap<String,Double> topBrands;
    HashMap<String,Double> topCategories;
    Double totalBuy;
    Double totalSell;
    Double totalBuyReturned;
    Double totalSellReturned;
    Double totalDispose;
}
