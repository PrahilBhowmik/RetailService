package com.example.RetailService.entity;

import com.example.RetailService.utils.Product;
import com.example.RetailService.utils.TransactionType;
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
@EqualsAndHashCode
public class Transaction {
    @Id
    String id;
    Product[] products;
    TransactionType type;
    Double total;
    Date date;
    String userId;
}
