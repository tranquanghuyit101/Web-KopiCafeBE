package com.kopi.kopi.repository;

import com.kopi.kopi.entity.AddOn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AddOnRepository extends JpaRepository<AddOn, Integer> {
	Optional<AddOn> findByName(String name);
}


