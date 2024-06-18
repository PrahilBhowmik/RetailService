package com.example.RetailService.controller;

import com.example.RetailService.entity.Transaction;
import com.example.RetailService.entity.User;
import com.example.RetailService.service.UserService;
import com.example.RetailService.utils.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/retail-service")
public class RetailServiceController {
    @Autowired
    private UserService userService;

    @PostMapping("/user")
    public Mono<User> addUser(@RequestBody Mono<User> user){
        return userService.addUser(user);
    }

    @GetMapping("/user/{userId}")
    public Mono<User> getUser(@PathVariable String userId){
        return userService.getUser(userId);
    }

    @GetMapping("/transactions/{userId}")
    public Flux<Transaction> getTransactions(@PathVariable String userId){
        return userService.getTransactions(userId);
    }

    @PostMapping("/transactions")
    public Mono<Object> addTransaction(@RequestBody Mono<Transaction> transaction){
        return userService.addTransaction(transaction);
    }

    @PutMapping("/user/{userId}/productId={productId}/discount={discount}")
    public Mono<Product> setDiscountById(@PathVariable String userId, @PathVariable String productId, @PathVariable Double discount){
        return userService.setDiscountById(userId,productId,discount);
    }

    @PutMapping("/user/{userId}/brand={brand}/discount={discount}")
    public Flux<Product> setDiscountByBrand(@PathVariable String userId, @PathVariable String brand, @PathVariable Double discount){
        return userService.setDiscountByBrand(userId,brand,discount);
    }

    @PutMapping("/user/{userId}/category={category}/discount={discount}")
    public Flux<Product> setDiscountByCategory(@PathVariable String userId, @PathVariable String category, @PathVariable Double discount){
        return userService.setDiscountByCategory(userId,category,discount);
    }
}
