package com.example.RetailService.repository;

import com.example.RetailService.entity.User;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends ReactiveMongoRepository<User,String> {

}
