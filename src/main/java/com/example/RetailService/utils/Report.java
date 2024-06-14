package com.example.RetailService.utils;

import lombok.*;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Report {
    String userId;
    String userName;
    String profitOrLoss;
    Double profitOrLossAmount;
    Double income;
    Double expenditure;
    Date fromDate;
    Date toDate;
}
