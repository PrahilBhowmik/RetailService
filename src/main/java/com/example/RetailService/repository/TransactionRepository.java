package com.example.RetailService.repository;

import com.example.RetailService.entity.Transaction;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.Date;

@Repository
public interface TransactionRepository extends ReactiveMongoRepository<Transaction,String> {
    Flux<Transaction> findByUserIdOrderByDateDesc(String userId);

    Flux<Transaction> findByUserIdAndDateBetween(String userId, Date fromDate, Date toDate);
}
