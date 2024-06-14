package com.example.RetailService.utils;

import lombok.*;
import org.springframework.data.annotation.Id;

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
    Double mrp;
    Double cost;
    Double discount;
    Integer units;
    String brand;
}
