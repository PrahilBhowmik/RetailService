package com.example.RetailService.controller;

import com.example.RetailService.entity.Transaction;
import com.example.RetailService.entity.User;
import com.example.RetailService.service.UserService;
import com.example.RetailService.utils.Product;
import com.example.RetailService.utils.Report;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RestController
@RequestMapping("/retail-service")
public class RetailServiceController {
    @Autowired
    private UserService userService;

    @PostMapping("/user")
    public Mono<User> addUser(@RequestBody Mono<User> user){
        return userService.addUser(user);
    }

    @GetMapping("/user")
    public Mono<User> getUser(){
        return userService.getUser();
    }

    @GetMapping("/transactions")
    public Flux<Transaction> getTransactions(){
        return userService.getTransactions();
    }

    @PostMapping("/transactions")
    public Mono<Transaction> addTransaction(@RequestBody Mono<Transaction> transaction){
        return userService.addTransaction(transaction);
    }

    @PutMapping("/discount/productId={productId}/discount={discount}")
    public Mono<Product> setDiscountById(@PathVariable String productId, @PathVariable BigDecimal discount){
        return userService.setDiscountById(productId,discount);
    }

    @PutMapping("/discount/brand={brand}/discount={discount}")
    public Flux<Object> setDiscountByBrand(@PathVariable String brand, @PathVariable BigDecimal discount){
        return userService.setDiscountByBrand(brand,discount);
    }

    @PutMapping("/discount/category={category}/discount={discount}")
    public Flux<Object> setDiscountByCategory(@PathVariable String category, @PathVariable BigDecimal discount){
        return userService.setDiscountByCategory(category,discount);
    }

    @GetMapping("/report/from={fromDate}/to={toDate}")
    public Mono<Report> getReport(@PathVariable Long fromDate,@PathVariable Long toDate){
        return userService.generateReport(fromDate,toDate);
    }
}
