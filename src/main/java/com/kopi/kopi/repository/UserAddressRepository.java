package com.kopi.kopi.repository;

import com.kopi.kopi.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    // Lấy địa chỉ ưu tiên cho hiển thị: ưu tiên default, nếu không có thì lấy mới nhất
    @Query("""
           select ua
           from UserAddress ua
           join fetch ua.address a
           where ua.user.userId = :userId
           order by ua.defaultAddress desc, ua.createdAt desc
           """)
    List<UserAddress> findPreferredWithAddressByUserId(@Param("userId") Integer userId);

    @Modifying
    @Query("delete from UserAddress ua where ua.user.userId = :userId")
    void deleteByUserId(@Param("userId") Integer userId);

    @Modifying
    @Query("update UserAddress ua set ua.defaultAddress = false where ua.user.userId = :userId and ua.defaultAddress = true")
    int unsetDefaultForUser(@Param("userId") Integer userId);
}
