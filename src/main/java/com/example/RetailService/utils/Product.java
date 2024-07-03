package com.example.RetailService.utils;

import lombok.*;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Product {
    @Id
    String id;
    String name;
    String category;
    BigDecimal mrp;
    BigDecimal cost;
    BigDecimal discount;
    Integer units;
    String brand;
}
