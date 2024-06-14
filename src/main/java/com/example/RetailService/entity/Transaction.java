package com.example.RetailService.entity;

import com.example.RetailService.utils.Product;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "transactions")
@Getter
@Setter
public class Transaction {
    @Id
    String id;
    Product[] products;
    String type;
    Double total;
    Date date;
    String userId;
}
