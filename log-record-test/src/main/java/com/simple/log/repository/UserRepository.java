package com.simple.log.repository;


import com.simple.log.model.User;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * @author fdrama
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {


}
