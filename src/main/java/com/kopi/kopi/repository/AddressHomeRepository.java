package com.kopi.kopi.repository;

import com.kopi.kopi.entity.AddressHome;
import com.kopi.kopi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressHomeRepository extends JpaRepository<AddressHome, Integer> {
    List<AddressHome> findByUser(User user);

    List<AddressHome> findByUserUserId(Integer userId);

    // helper to get the latest address for a user
    java.util.Optional<AddressHome> findTopByUserUserIdOrderByCreatedAtDesc(Integer userId);

}
