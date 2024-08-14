package com.example.RetailService.controller;

import com.example.RetailService.entity.Transaction;
import com.example.RetailService.entity.User;
import com.example.RetailService.service.UserService;
import com.example.RetailService.utils.Product;
import com.example.RetailService.utils.Report;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
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
    public Mono<User> addUser(@RequestBody Mono<User> user, Authentication authentication){
        return userService.addUser(user, userService.authenticate(authentication));
    }

    @GetMapping("/user")
    public Mono<User> getUser(Authentication authentication){
        return userService.getUser(userService.authenticate(authentication));
    }

    @GetMapping("/transactions")
    public Flux<Transaction> getTransactions(Authentication authentication){
        return userService.getTransactions(userService.authenticate(authentication));
    }

    @PostMapping("/transactions")
    public Mono<Transaction> addTransaction(@RequestBody Mono<Transaction> transaction,Authentication authentication){
        return userService.addTransaction(transaction,userService.authenticate(authentication));
    }

    @PutMapping("/discount/productId={productId}/discount={discount}")
    public Mono<Product> setDiscountById(@PathVariable String productId, @PathVariable BigDecimal discount,Authentication authentication){
        return userService.setDiscountById(productId,discount,userService.authenticate(authentication));
    }

    @PutMapping("/discount/brand={brand}/discount={discount}")
    public Flux<Object> setDiscountByBrand(@PathVariable String brand, @PathVariable BigDecimal discount,Authentication authentication){
        return userService.setDiscountByBrand(brand,discount,userService.authenticate(authentication));
    }

    @PutMapping("/discount/category={category}/discount={discount}")
    public Flux<Object> setDiscountByCategory(@PathVariable String category, @PathVariable BigDecimal discount,Authentication authentication){
        return userService.setDiscountByCategory(category,discount,userService.authenticate(authentication));
    }

    @GetMapping("/report/from={fromDate}/to={toDate}")
    public Mono<Report> getReport(@PathVariable Long fromDate,@PathVariable Long toDate,Authentication authentication){
        return userService.generateReport(fromDate,toDate,userService.authenticate(authentication));
    }
}
