package com.sds.springbootmultithreading.repository;

import com.sds.springbootmultithreading.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserRepository extends JpaRepository<User, Integer> {
}
