package com.kopi.kopi.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kopi.kopi.entity.User;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
// Bổ sung JpaSpecificationExecutor<User> Khả năng viết query động, truy vấn linh hoạt (Specification)
public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    long deleteByEmailVerifiedIsFalseAndCreatedAtBefore(LocalDateTime before);


    java.util.List<User> findByRoleNameIn(java.util.Collection<String> names);

    @Query(value = "SELECT u.user_id, u.full_name, p.position_name " +
            "FROM dbo.users u " +
            "LEFT JOIN dbo.positions p ON u.position_id = p.position_id " +
            "LEFT JOIN dbo.roles r ON u.role_id = r.role_id " +
            "WHERE r.name IN (:names)", nativeQuery = true)
    List<Object[]> findUserIdFullNamePositionByRoleNames(@Param("names") Collection<String> names);

    @Query(value = "SELECT u.user_id, u.full_name, p.position_name " +
            "FROM dbo.users u " +
            "LEFT JOIN dbo.positions p ON u.position_id = p.position_id " +
            "WHERE u.role_id = :roleId", nativeQuery = true)
    List<Object[]> findUserIdFullNamePositionByRoleId(@Param("roleId") Integer roleId);

    java.util.List<User> findByRoleRoleId(Integer roleId);

    java.util.List<User> findByRoleRoleIdAndStatusNot(Integer roleId, com.kopi.kopi.entity.enums.UserStatus status);


    org.springframework.data.domain.Page<User> findByRoleRoleIdNot(Integer roleId,
            org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<User> findByRoleRoleIdNotAndStatusNot(Integer roleId,
            com.kopi.kopi.entity.enums.UserStatus status, org.springframework.data.domain.Pageable pageable);


    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u LEFT JOIN u.position p " +
            "WHERE (:positionName IS NULL OR p.positionName = :positionName) " +
            "AND (:phone IS NULL OR u.phone LIKE CONCAT('%', :phone, '%')) " +
            "AND (:email IS NULL OR u.email LIKE CONCAT('%', :email, '%')) " +
            "AND (:username IS NULL OR u.username LIKE CONCAT('%', :username, '%'))")
    java.util.List<User> searchByFilters(
            @org.springframework.data.repository.query.Param("positionName") String positionName,
            @org.springframework.data.repository.query.Param("phone") String phone,
            @org.springframework.data.repository.query.Param("email") String email,
            @org.springframework.data.repository.query.Param("username") String username);
}