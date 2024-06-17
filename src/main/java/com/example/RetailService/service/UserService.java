package com.example.RetailService.service;

import com.example.RetailService.utils.Product;
import com.example.RetailService.utils.Report;
import com.example.RetailService.entity.Transaction;
import com.example.RetailService.entity.User;
import com.example.RetailService.repository.TransactionRepository;
import com.example.RetailService.repository.UserRepository;
import com.example.RetailService.utils.TransactionType;
import com.example.RetailService.utils.Utility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
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
                    return  Mono.just(user1);
                }).flatMap(userRepository::save);
    }

    public Mono<User> getUser(String userId){
        return userRepository.findById(userId);
    }

    public Flux<Transaction> getTransactions(String userId){
        return  transactionRepository.findByUserIdOrderByDateDesc(userId);
    }

    private Mono<Object> updateProducts(Transaction transaction){
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
                    return Mono.error(new RuntimeException("Invalid TransactionType"));
                })
                .flatMap(object -> {
                    if (object instanceof User) {
                        return userRepository.save((User) object)
                                .flatMap(savedUser -> Mono.just(transaction));
                    } else {
                        return Mono.error((RuntimeException) object);
                    }
                });
    }

    private Mono<Object> updateForBuy(User user,Transaction transaction){
        HashMap<String, Product> userProducts = user.getProducts();
        AtomicReference<Double> amount = new AtomicReference<>(0.00);
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
                    amount.updateAndGet(v -> v + product.getCost());
                }
        );
        transaction.setTotal(amount.get());
        user.setProducts(userProducts);
        return Mono.just(user);
    }


    private Mono<Object> updateForReturnSell(User user, Transaction transaction) {
        HashMap<String, Product> userProducts = user.getProducts();
        AtomicReference<Double> amount = new AtomicReference<>(0.00);
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
                    amount.updateAndGet(v -> v + product.getMrp()*product.getDiscount());
                }
        );
        transaction.setTotal(amount.get());
        user.setProducts(userProducts);
        return Mono.just(user);
    }

    private Mono<Object> updateForReturnBuyOrDispose(User user, Transaction transaction) {
        HashMap<String, Product> userProducts = user.getProducts();
        AtomicReference<Double> amount = new AtomicReference<>(0.00);
        Arrays.stream(transaction.getProducts()).forEach(
                product -> {
                    Product userProduct = userProducts.get(product.getId());
                    if(userProduct!=null){
                        userProduct.setUnits(userProduct.getUnits()-product.getUnits());
                    }
                    else{
                        userProduct=product;
                    }
                    userProducts.put(product.getId(), userProduct);
                    amount.updateAndGet(v -> v + product.getCost());
                }
        );
        transaction.setTotal(amount.get());
        user.setProducts(userProducts);
        return Mono.just(user);
    }

    private Mono<Object> updateForSell(User user, Transaction transaction) {
        HashMap<String, Product> userProducts = user.getProducts();
        AtomicReference<Double> amount = new AtomicReference<>(0.00);
        Arrays.stream(transaction.getProducts()).forEach(
                product -> {
                    Product userProduct = userProducts.get(product.getId());
                    if(userProduct!=null){
                        userProduct.setUnits(userProduct.getUnits()-product.getUnits());
                    }
                    else{
                        userProduct=product;
                    }
                    userProducts.put(product.getId(), userProduct);
                    amount.updateAndGet(v -> v + product.getMrp()*product.getDiscount());
                }
        );
        transaction.setTotal(amount.get());
        user.setProducts(userProducts);
        return Mono.just(user);
    }

    public Mono<Object> addTransaction(Mono<Transaction> transactionMono){
        return transactionMono
                .flatMap(transaction -> {
                    transaction.setId(null);
                    return Mono.just(transaction);
                }).flatMap(this::updateProducts)
                .flatMap(object->{
                    if(object instanceof Transaction){
                        return transactionRepository.save((Transaction) object);
                    }else {
                        return Mono.error((RuntimeException) object);
                    }
                });
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
