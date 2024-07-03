package com.example.RetailService.service;

import com.example.RetailService.errors.*;
import com.example.RetailService.utils.*;
import com.example.RetailService.entity.Transaction;
import com.example.RetailService.entity.User;
import com.example.RetailService.repository.TransactionRepository;
import com.example.RetailService.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TransactionRepository transactionRepository;

    public Mono<User> addUser(Mono<User> user){
        return user.flatMap(user1->{
                    user1.setId(null);
                    if(user1.getProducts()==null){
                        user1.setProducts(new HashMap<>());
                    }
                    return  Mono.just(user1);
                }).flatMap(userRepository::save);
    }

    public Mono<User> getUser(String userId){
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new UserNotFoundException()));
    }

    public Flux<Transaction> getTransactions(String userId){
        return  transactionRepository.findByUserIdOrderByDateDesc(userId)
                .switchIfEmpty(this.getUser(userId).then(Mono.error(new NoTransactionsMadeException())));
    }

    private Mono<Transaction> updateProducts(Transaction transaction){
        return this.getUser(transaction.getUserId())
                .flatMap(user -> {
                    if(transaction.getType()==TransactionType.BUY){
                        return this.updateForBuy(user,transaction);
                    } else if (transaction.getType()==TransactionType.SELL) {
                        return this.updateForSell(user,transaction);
                    } else if (transaction.getType()==TransactionType.RETURN_BUY || transaction.getType()==TransactionType.DISPOSE) {
                        return this.updateForReturnBuyOrDispose(user,transaction);
                    } else if (transaction.getType()==TransactionType.RETURN_SELL) {
                        return this.updateForReturnSell(user,transaction);
                    }
                    return Mono.error(new InvalidTransactionException());
                })
                .flatMap(user -> userRepository.save(user)
                        .flatMap(_ -> Mono.just(transaction)));
    }

    private Mono<User> updateForBuy(User user,Transaction transaction){
        HashMap<String, Product> userProducts = user.getProducts();
        AtomicReference<BigDecimal> amount = new AtomicReference<>(BigDecimal.ZERO);
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
                    amount.updateAndGet(v-> v.add(product.getCost().multiply(BigDecimal.valueOf(product.getUnits()))));
                }
        );
        transaction.setTotal(amount.get());
        user.setProducts(userProducts);
        return Mono.just(user);
    }

    private Mono<User> updateForReturnSell(User user, Transaction transaction) {
        HashMap<String, Product> userProducts = user.getProducts();
        AtomicReference<BigDecimal> amount = new AtomicReference<>(BigDecimal.ZERO);
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
                    amount.updateAndGet(v -> v.add(product.getMrp().multiply(product.getDiscount().negate().add(BigDecimal.ONE)).multiply(BigDecimal.valueOf(product.getUnits()))));
                }
        );
        transaction.setTotal(amount.get());
        user.setProducts(userProducts);
        return Mono.just(user);
    }

    private Mono<User> updateForReturnBuyOrDispose(User user, Transaction transaction) {
        HashMap<String, Product> userProducts = user.getProducts();
        AtomicReference<BigDecimal> amount = new AtomicReference<>(BigDecimal.ZERO);
        AtomicReference<Boolean> invalid = new AtomicReference<>(false);
        Arrays.stream(transaction.getProducts()).takeWhile(_->!invalid.get()).forEach(
                product -> {
                    Product userProduct = userProducts.get(product.getId());
                    if(userProduct!=null){
                        userProduct.setUnits(userProduct.getUnits()-product.getUnits());
                        if(userProduct.getUnits()<0.00){
                            invalid.updateAndGet(_ -> true);
                        }
                    }
                    else{
                        invalid.updateAndGet(_ -> true);
                    }
                    userProducts.put(product.getId(), userProduct);
                    amount.updateAndGet(v -> v.add(product.getCost().multiply(BigDecimal.valueOf(product.getUnits()))));
                }
        );
        if(invalid.get()){
            return Mono.error(new NotEnoughProductsException());
        }
        transaction.setTotal(amount.get());
        user.setProducts(userProducts);
        return Mono.just(user);
    }

    private Mono<User> updateForSell(User user, Transaction transaction) {
        HashMap<String, Product> userProducts = user.getProducts();
        AtomicReference<BigDecimal> amount = new AtomicReference<>(BigDecimal.ZERO);
        AtomicReference<Boolean> invalid = new AtomicReference<>(false);
        Arrays.stream(transaction.getProducts()).takeWhile(_->!invalid.get()).forEach(
                product -> {
                    Product userProduct = userProducts.get(product.getId());
                    if(userProduct!=null){
                        userProduct.setUnits(userProduct.getUnits()-product.getUnits());
                        if(userProduct.getUnits()<0.00){
                            invalid.updateAndGet(_ -> true);
                        }
                    }
                    else{
                        invalid.updateAndGet(_ -> true);
                    }
                    userProducts.put(product.getId(), userProduct);
                    amount.updateAndGet(v -> v.add(product.getMrp().multiply(product.getDiscount().negate().add(BigDecimal.ONE)).multiply(BigDecimal.valueOf(product.getUnits()))));
                }
        );
        if(invalid.get()){
            return Mono.error(new NotEnoughProductsException());
        }
        transaction.setTotal(amount.get());
        user.setProducts(userProducts);
        return Mono.just(user);
    }

    public Mono<Transaction> addTransaction(Mono<Transaction> transactionMono){
        return transactionMono
                .flatMap(transaction -> {
                    transaction.setId(null);
                    return Mono.just(transaction);
                }).flatMap(this::updateProducts)
                .flatMap(transactionRepository::save);
    }

    public Mono<Report> generateReport(String userId, Long fromDate, Long toDate){
        return Utility.generateReport(userId, new Date(fromDate),new Date(toDate),
                transactionRepository.findByUserIdAndDateBetween(userId,new Date(fromDate),new Date(toDate)));
    }

    public Mono<Product> setDiscountById(String userId,String productId,BigDecimal discount){
        return getUser(userId).flatMap(
                user -> {
                    HashMap<String, Product> userProducts = user.getProducts();
                    Product product = userProducts.get(productId);
                    product.setDiscount(discount);
                    userProducts.put(productId,product);
                    user.setProducts(userProducts);
                    return userRepository.save(user);
                }
        ).flatMap(user -> Mono.just(user.getProducts().get(productId)));
    }

    public Flux<Object> setDiscountByCategory(String userId,String category,BigDecimal discount){
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
                    return userRepository.save(user);
                }
        ).flatMap(user -> Mono.just(user.getProducts().values()))
                .flatMapIterable(products -> products.stream().filter(product -> category.equalsIgnoreCase(product.getCategory())).toList());
    }

    public Flux<Object> setDiscountByBrand(String userId,String brand,BigDecimal discount){
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
                            return userRepository.save(user);
                        }
                ).flatMap(user -> Mono.just(user.getProducts().values()))
                .flatMapIterable(products -> products.stream().filter(product -> brand.equalsIgnoreCase(product.getBrand())).toList());
    }
}
