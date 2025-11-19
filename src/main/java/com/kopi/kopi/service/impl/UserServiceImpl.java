package com.kopi.kopi.service.impl;

import com.kopi.kopi.dto.CustomerListDto;
import com.kopi.kopi.dto.EmployeeDetailDto;
import com.kopi.kopi.dto.EmployeeSimpleDto;
import com.kopi.kopi.dto.UpdateEmployeeRequest;
import com.kopi.kopi.entity.Address;
import com.kopi.kopi.entity.User;
import com.kopi.kopi.repository.UserRepository;
import com.kopi.kopi.repository.PositionRepository;
import com.kopi.kopi.repository.RoleRepository;
import com.kopi.kopi.repository.UserAddressRepository;
import com.kopi.kopi.security.ForceChangeStore;
import com.kopi.kopi.service.EmailService;
import com.kopi.kopi.service.IUserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import com.kopi.kopi.entity.enums.UserStatus;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

@Service
public class UserServiceImpl implements IUserService {
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final ForceChangeStore forceChangeStore;
    private final UserAddressRepository userAddressRepository;
    private final RoleRepository roleRepository;
    private final PositionRepository positionRepository;

    // Mở rộng Constructor chính
    @Autowired
    public UserServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Qualifier("smtpEmailService") EmailService emailService, // match bean theo name
            ForceChangeStore forceChangeStore,
            UserAddressRepository userAddressRepository,
            RoleRepository roleRepository,
            PositionRepository positionRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.forceChangeStore = forceChangeStore;
        this.userAddressRepository = userAddressRepository;
        this.roleRepository = roleRepository;
        this.positionRepository = positionRepository;
    }

    // Constructor cũ của Võ Nhật Duy, lấy lại tất cả field cũ đã khai báo từ
    // contructor mở rộng
    public UserServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Qualifier("smtpEmailService") EmailService emailService, // match bean theo name
            ForceChangeStore forceChangeStore) {
        this(userRepository, passwordEncoder, emailService, forceChangeStore, null, null, null);
    }

    @Override
    @PreAuthorize("permitAll()")
    @Transactional
    public void resetPassword(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty())
            return;

        User user = userOpt.get();
        String tmp = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        user.setPasswordHash(passwordEncoder.encode(tmp));
        userRepository.save(user);

        // BẬT cờ trong file
        forceChangeStore.set(user.getEmail(), true);

        try {
            emailService.send(
                    user.getEmail(),
                    "[Kopi] Your temporary password",
                    "Hi " + user.getFullName() + ",\n\nYour temporary password is: " + tmp +
                            "\nPlease log in and change it immediately.");
        } catch (Exception ex) {
            LoggerFactory.getLogger(getClass())
                    .warn("Send mail failed for {}: {}", user.getEmail(), ex.getMessage());
        }
        System.out.println("Temporary password for " + user.getEmail() + ": " + tmp);
    }

    @Override
    @PreAuthorize("permitAll()")
    @Transactional
    public void changePassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // TẮT cờ trong file
        forceChangeStore.set(email, false);
    }

    @Override
    public boolean mustChangePassword(String email) {
        return forceChangeStore.get(email);
    }

    @Override
    public void clearForceChangePassword(String email) {
        forceChangeStore.set(email, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeSimpleDto> listEmployees() {
        // return users whose role_id == 2, excluding BANNED
        java.util.List<User> users = userRepository.findByRoleRoleIdAndStatusNot(2, UserStatus.BANNED);
        return users.stream().map(u -> {
            Integer userId = u.getUserId();
            String fullNameLocal = u.getFullName();
            String positionName = (u.getPosition() != null ? u.getPosition().getPositionName() : null);
            String status = (u.getStatus() != null ? u.getStatus().name() : null);
            return new EmployeeSimpleDto(userId, fullNameLocal, positionName, status);
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeSimpleDto> searchEmployees(String positionName, String phone,
            String email, String fullName) {
        // build dynamic specification to filter employees (role_id == 2)
        Specification<User> spec = (root, query, cb) -> {
            // ensure distinct root when joining
            query.distinct(true);
            List<Predicate> preds = new ArrayList<>();
            // only employees (role_id == 2)
            var roleJoin = root.join("role", JoinType.LEFT);
            preds.add(cb.equal(roleJoin.get("roleId"), 2));

            if (positionName != null && !positionName.isBlank()) {
                var posJoin = root.join("position", JoinType.LEFT);
                preds.add(cb.like(cb.lower(posJoin.get("positionName")), "%" + positionName.toLowerCase() + "%"));
            }
            if (phone != null && !phone.isBlank()) {
                preds.add(cb.like(cb.lower(root.get("phone")), "%" + phone.toLowerCase() + "%"));
            }
            if (email != null && !email.isBlank()) {
                preds.add(cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
            }
            if (fullName != null && !fullName.isBlank()) {
                preds.add(cb.like(cb.lower(root.get("fullName")), "%" + fullName.toLowerCase() + "%"));
            }
            // exclude BANNED users from search results
            preds.add(cb.notEqual(root.get("status"), UserStatus.BANNED));
            return cb.and(preds.toArray(new Predicate[0]));
        };

        List<User> users = userRepository.findAll(spec);

        return users.stream().map(u -> {
            Integer userId = u.getUserId();
            String fullNameLocal = u.getFullName();
            String positionNameLocal = (u.getPosition() != null ? u.getPosition().getPositionName() : null);
            String status = (u.getStatus() != null ? u.getStatus().name() : null);
            return new EmployeeSimpleDto(userId, fullNameLocal, positionNameLocal, status);
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDetailDto getEmployeeDetail(Integer userId) {
        User user = userRepository.findById(userId).orElseThrow();
        // Ưu tiên địa chỉ default, nếu không có thì lấy địa chỉ mới nhất
        var preferredList = userAddressRepository.findPreferredWithAddressByUserId(userId);
        Address addr = preferredList.isEmpty() ? null : preferredList.get(0).getAddress();
        String street = addr != null ? addr.getAddressLine() : null;
        String city = addr != null ? addr.getCity() : null;
        String district = addr != null ? addr.getDistrict() : null;

        return new EmployeeDetailDto(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhone(),
                street,
                city,
                district);
    }

    @Override
    @Transactional
    public void updateEmployee(Integer userId, UpdateEmployeeRequest req) {
        User user = userRepository.findById(userId).orElseThrow();

        // update role if provided
        if (req.roleId() != null) {
            var roleOpt = roleRepository.findById(req.roleId());
            if (roleOpt.isEmpty()) {
                throw new IllegalArgumentException("Requested role not found: " + req.roleId());
            }
            var newRole = roleOpt.get();
            var currentRole = user.getRole();
            // Allow role change from 3 -> 2 (customer -> employee) as requested by FE
            boolean allowed = false;
            if (currentRole == null) {
                allowed = true; // no current role, allow set
            } else if (currentRole.getRoleId() == 3 && newRole.getRoleId() == 2) {
                allowed = true; // allow promotion from customer to employee
            } else if (currentRole.getRoleId().equals(newRole.getRoleId())) {
                allowed = true; // no-op
            } else {
                // For other transitions, require explicit admin action - reject here
                allowed = false;
            }

            if (allowed) {
                user.setRole(newRole);
            } else {
                throw new IllegalArgumentException("Role change not allowed from " +
                        (currentRole != null ? currentRole.getRoleId() : "null") + " to " + newRole.getRoleId());
            }
        }

        // update position if provided
        if (req.positionId() != null) {
            var posOpt = positionRepository.findById(req.positionId());
            posOpt.ifPresent(user::setPosition);
        }

        // update status if provided
        if (req.status() != null && !req.status().isBlank()) {
            try {
                var st = UserStatus.valueOf(req.status().toUpperCase());
                user.setStatus(st);
            } catch (IllegalArgumentException ex) {
                // ignore invalid status values (could also throw a BadRequest)
                // for now, log and ignore
                log.warn("Invalid status provided for user {}: {}", userId, req.status());
            }
        }

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void banUser(Integer userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setStatus(UserStatus.BANNED);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void demoteEmployeeToCustomer(Integer userId) {
        User user = userRepository.findById(userId).orElseThrow();
        // fetch role with id 3 (customer)
        var roleOpt = roleRepository.findById(3);
        if (roleOpt.isEmpty()) {
            throw new IllegalArgumentException("Customer role (id=3) not found");
        }
        // set role to customer and clear position
        user.setRole(roleOpt.get());
        user.setPosition(null);
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerListDto> listCustomers(int page, int size,
            String fullName,
            String phone,
            String email,
            String roleName,
            String status) {
        var pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));

        Specification<User> spec = (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> preds = new ArrayList<>();

            // exclude admin (role_id == 1)
            var roleJoin = root.join("role", JoinType.LEFT);
            preds.add(cb.notEqual(roleJoin.get("roleId"), 1));

            // apply status filter if provided. If status == 'BANNED', return only banned
            // users.
            if (status != null && !status.isBlank()) {
                try {
                    var st = UserStatus.valueOf(status.toUpperCase());
                    preds.add(cb.equal(root.get("status"), st));
                } catch (IllegalArgumentException ex) {
                    // invalid status value - ignore and continue (no extra predicate)
                    log.warn("Invalid status filter provided: {}", status);
                }
            } else {
                // default behaviour: exclude BANNED users
                preds.add(cb.notEqual(root.get("status"), UserStatus.BANNED));
            }

            if (fullName != null && !fullName.isBlank()) {
                preds.add(cb.like(cb.lower(root.get("fullName")), "%" + fullName.toLowerCase() + "%"));
            }
            if (phone != null && !phone.isBlank()) {
                preds.add(cb.like(cb.lower(root.get("phone")), "%" + phone.toLowerCase() + "%"));
            }
            if (email != null && !email.isBlank()) {
                preds.add(cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
            }
            if (roleName != null && !roleName.isBlank()) {
                preds.add(cb.like(cb.lower(roleJoin.get("name")), "%" + roleName.toLowerCase() + "%"));
            }

            return cb.and(preds.toArray(new Predicate[0]));
        };

        var usersPage = userRepository.findAll(spec, pageable);
        return usersPage.map(u -> new CustomerListDto(
                u.getUserId(),
                u.getFullName(),
                (u.getStatus() != null ? u.getStatus().name() : null),
                (u.getRole() != null ? u.getRole().getName() : null)));
    }
}
