package com.example.RetailService.entity;

import com.example.RetailService.utils.Product;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;

@Data
@Document(collection = "users")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class User {
    @Id
    String id;
    String name;
    String email;
    HashMap<String, Product> products;
}
