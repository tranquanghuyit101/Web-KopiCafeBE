package com.kopi.kopi.repository;

import com.kopi.kopi.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserAddressRepository extends JpaRepository<UserAddress, Integer> {

    // Lấy tất cả địa chỉ của user, JOIN FETCH Address để có dữ liệu, ưu tiên defaultAddress
    @Query("""
           select ua
           from UserAddress ua
           join fetch ua.address a
           where ua.user.userId = :userId
           order by ua.defaultAddress desc, ua.createdAt asc
           """)
    List<UserAddress> findAllWithAddressByUserId(@Param("userId") Integer userId);
}
