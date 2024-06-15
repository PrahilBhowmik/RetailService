package com.example.RetailService.service;

import com.example.RetailService.utils.Product;
import com.example.RetailService.utils.Report;
import com.example.RetailService.entity.Transaction;
import com.example.RetailService.entity.User;
import com.example.RetailService.repository.TransactionRepository;
import com.example.RetailService.repository.UserRepository;
import com.example.RetailService.utils.Utility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashMap;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TransactionRepository transactionRepository;

    public Mono<User> addUser(Mono<User> user){
        return user.flatMap(user1->{
                    user1.setId(null);
                    return  Mono.just(user1);
                }).flatMap(userRepository::save);
    }

    public Mono<User> getUser(String userId){
        return userRepository.findById(userId);
    }

    public Flux<Transaction> getTransactions(String userId){
        return  transactionRepository.findByUserIdOrderByDateDesc(userId);
    }

    private Mono<Transaction> updateProducts(Transaction transaction){
        return this.getUser(transaction.getUserId())
                .flatMap(
                    user -> {
                        HashMap<String, Product> userProducts = user.getProducts();
                        Arrays.stream(transaction.getProducts()).forEach(
                                product -> {
                                    Product userProduct = userProducts.get(product.getId());
                                    if(userProduct!=null){
                                        userProduct.setUnits(userProduct.getUnits()+product.getUnits());
                                    }
                                    else{
                                        userProduct=product;
                                    }
                                    userProducts.put(product.getId(), userProduct);
                                }
                        );
                        user.setProducts(userProducts);
                        return Mono.just(user);
                    }
                )
                .flatMap(userRepository::save)
                .flatMap(user -> Mono.just(transaction));
    }

    public Mono<Transaction> addTransaction(Mono<Transaction> transactionMono){
        return transactionMono
                .flatMap(transaction -> {
                    transaction.setId(null);
                    return Mono.just(transaction);
                }).flatMap(this::updateProducts)
                .flatMap(transactionRepository::save);
    }

    public Mono<Report> generateReport(){
        return Mono.just(Utility.generateReport());
    }

    public Mono<Product> setDiscountById(String userId,String productId,Double discount){
        return getUser(userId).flatMap(
                user -> {
                    HashMap<String, Product> userProducts = user.getProducts();
                    Product product = userProducts.get(productId);
                    product.setDiscount(discount);
                    userProducts.put(productId,product);
                    user.setProducts(userProducts);
                    userRepository.save(user);
                    return Mono.just(product);
                }
        );
    }

    public Flux<Product> setDiscountByCategory(String userId,String category,Double discount){
        return getUser(userId).flatMap(
                user -> {
                    HashMap<String, Product> userProducts = user.getProducts();
                    userProducts.forEach(
                            (s, product) -> {
                                if(category.equalsIgnoreCase(product.getCategory())) {
                                    product.setDiscount(discount);
                                }
                            }
                    );
                    user.setProducts(userProducts);
                    userRepository.save(user);
                    return Mono.just(userProducts.values());
                }
        ).flatMapIterable(products -> products.stream().toList());
    }

    public Flux<Product> setDiscountByBrand(String userId,String brand,Double discount){
        return getUser(userId).flatMap(
                user -> {
                    HashMap<String, Product> userProducts = user.getProducts();
                    userProducts.forEach(
                            (s, product) -> {
                                if(brand.equalsIgnoreCase(product.getBrand())) {
                                    product.setDiscount(discount);
                                }
                            }
                    );
                    user.setProducts(userProducts);
                    userRepository.save(user);
                    return Mono.just(userProducts.values());
                }
        ).flatMapIterable(products -> products.stream().toList());
    }
}
