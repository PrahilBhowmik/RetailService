package com.example.RetailService.service;

import com.example.RetailService.errors.*;
import com.example.RetailService.utils.*;
import com.example.RetailService.entity.Transaction;
import com.example.RetailService.entity.User;
import com.example.RetailService.repository.TransactionRepository;
import com.example.RetailService.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    IAuthenticationFacade authenticationFacade;

    public Authentication authenticate(Authentication authentication){
        if(authentication != null){
            return authentication;
        }
        return authenticationFacade.getAuthentication();
    }

    public Mono<User> addUser(Mono<User> user,Authentication authentication){
        AtomicReference<Boolean> userAdded = new AtomicReference<>(false);

        return user.flatMap(user1->{
                    user1.setId(null);
                    user1.setEmail(Utility.getEmailFromAuthentication(authentication));
                    if(user1.getProducts()==null){
                        user1.setProducts(new HashMap<>());
                    }
                    return  Mono.just(user1);
                }).flatMap(user1 -> this.getUser(authentication)
                .onErrorResume(throwable -> {
                            if(UserNotFoundException.class.equals(throwable.getClass())){
                                userAdded.updateAndGet(_ -> true);
                                return userRepository.save(user1);
                            }
                            return Mono.error(throwable);
                        }))
                .flatMap(user1 -> {
                    if(userAdded.get()){
                        return Mono.just(user1);
                    }
                    return Mono.error(new UserAlreadyExistsException());
                });
    }

    public Mono<User> getUser(Authentication authentication){
        return userRepository.findByEmail(Utility.getEmailFromAuthentication(authentication))
                .switchIfEmpty(Mono.error(new UserNotFoundException()));
    }

    public Flux<Transaction> getTransactions(Authentication authentication){
        return this.getUser(authentication).flatMapMany(user -> transactionRepository.findByUserIdOrderByDateDesc(user.getId())
                .switchIfEmpty(Mono.error(new NoTransactionsMadeException())));
    }

    private boolean hasNegative(Product product){
        return product.getUnits() < 0 || product.getCost().compareTo(BigDecimal.ZERO) < 0 || product.getMrp().compareTo(BigDecimal.ZERO) < 0
                || product.getDiscount().compareTo(BigDecimal.ZERO) < 0 || product.getDiscount().compareTo(BigDecimal.ONE) > 0;
    }

    private Mono<Transaction> updateProducts(Transaction transaction,User user){
        AtomicReference<Boolean> invalid = new AtomicReference<>(false);

        if(transaction.getType()==TransactionType.BUY){
            this.updateForBuy(user,transaction,invalid);
        } else if (transaction.getType()==TransactionType.SELL) {
            this.updateForSell(user,transaction,invalid);
        } else if (transaction.getType()==TransactionType.RETURN_BUY || transaction.getType()==TransactionType.DISPOSE) {
            this.updateForReturnBuyOrDispose(user,transaction,invalid);
        } else if (transaction.getType()==TransactionType.RETURN_SELL) {
            this.updateForReturnSell(user,transaction,invalid);
        }else{
            return Mono.error(new InvalidTransactionException());
        }

        if(invalid.get()){
            return Mono.error(new NotEnoughProductsException());
        }

        return userRepository.save(user)
                .flatMap(_ -> Mono.just(transaction));
    }

    private void updateForBuy(User user,Transaction transaction, AtomicReference<Boolean> invalid){
        HashMap<String, Product> userProducts = user.getProducts();
        AtomicReference<BigDecimal> amount = new AtomicReference<>(BigDecimal.ZERO);
        Arrays.stream(transaction.getProducts()).takeWhile(_->!invalid.get()).forEach(
                product -> {
                    if(this.hasNegative(product)){
                        invalid.updateAndGet(_->true);
                    }
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
    }

    private void updateForReturnSell(User user, Transaction transaction, AtomicReference<Boolean> invalid) {
        HashMap<String, Product> userProducts = user.getProducts();
        AtomicReference<BigDecimal> amount = new AtomicReference<>(BigDecimal.ZERO);
        Arrays.stream(transaction.getProducts()).takeWhile(_->!invalid.get()).forEach(
                product -> {
                    if(this.hasNegative(product)){
                        invalid.updateAndGet(_->true);
                    }
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
    }

    private void updateForReturnBuyOrDispose(User user, Transaction transaction,AtomicReference<Boolean> invalid) {
        HashMap<String, Product> userProducts = user.getProducts();
        AtomicReference<BigDecimal> amount = new AtomicReference<>(BigDecimal.ZERO);
        Arrays.stream(transaction.getProducts()).takeWhile(_->!invalid.get()).forEach(
                product -> {
                    if(this.hasNegative(product)){
                        invalid.updateAndGet(_->true);
                    }
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
        transaction.setTotal(amount.get());
        user.setProducts(userProducts);
    }

    private void updateForSell(User user, Transaction transaction,AtomicReference<Boolean> invalid) {
        HashMap<String, Product> userProducts = user.getProducts();
        AtomicReference<BigDecimal> amount = new AtomicReference<>(BigDecimal.ZERO);
        Arrays.stream(transaction.getProducts()).takeWhile(_->!invalid.get()).forEach(
                product -> {
                    if(this.hasNegative(product)){
                        invalid.updateAndGet(_->true);
                    }
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
        transaction.setTotal(amount.get());
        user.setProducts(userProducts);
    }

    public Mono<Transaction> addTransaction(Mono<Transaction> transactionMono,Authentication authentication){
        return this.getUser(authentication).flatMap(user -> transactionMono
                .flatMap(transaction -> {
                    transaction.setId(null);
                    transaction.setUserId(user.getId());
                    return Mono.just(transaction);
                }).flatMap(transaction->this.updateProducts(transaction,user))
                .flatMap(transactionRepository::save));
    }

    public Mono<Report> generateReport(Date fromDate, Date toDate,Authentication authentication){
        return this.getUser(authentication).flatMap(user -> Utility.generateReport(user.getId(), fromDate, toDate,
                transactionRepository.findByUserIdAndDateBetween(user.getId(), fromDate, toDate)));
    }

    public Mono<Product> setDiscountById(String productId,BigDecimal discount,Authentication authentication){
        return this.getUser(authentication).flatMap(
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

    public Flux<Object> setDiscountByCategory(String category,BigDecimal discount,Authentication authentication){
        return this.getUser(authentication).flatMap(
                user -> {
                    HashMap<String, Product> userProducts = user.getProducts();
                    userProducts.forEach(
                            (_, product) -> {
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

    public Flux<Object> setDiscountByBrand(String brand,BigDecimal discount,Authentication authentication){
        return this.getUser(authentication).flatMap(
                        user -> {
                            HashMap<String, Product> userProducts = user.getProducts();
                            userProducts.forEach(
                                    (_, product) -> {
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
