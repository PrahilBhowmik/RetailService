package com.example.RetailService.utils;

import lombok.*;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReportRequestBody {
    Date fromDate;
    Date toDate;
}
