package com.kopi.kopi.service.impl;

import com.kopi.kopi.controller.ProfileController;
import com.kopi.kopi.dto.ProfileResponse;
import com.kopi.kopi.entity.Address;
import com.kopi.kopi.entity.User;
import com.kopi.kopi.entity.UserAddress;
import com.kopi.kopi.repository.AddressRepository;
import com.kopi.kopi.repository.UserAddressRepository;
import com.kopi.kopi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplExtraTest {

    @Mock
    UserRepository userRepository;
    @Mock
    UserAddressRepository userAddressRepository;
    @Mock
    AddressRepository addressRepository;
    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    ProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProfileServiceImpl(userRepository, userAddressRepository, addressRepository, passwordEncoder);
    }

    @Test
    void getProfile_noAddresses_returnsNullAddress() {
        User u = User.builder().userId(5).username("u5").fullName("User Five").email("e@x").phone("0123")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(userRepository.findById(5)).thenReturn(Optional.of(u));
        when(userAddressRepository.findAllWithAddressByUserId(5)).thenReturn(List.of());

        ResponseEntity<ProfileResponse> r = service.getProfile(5);
        assertThat(r.getStatusCode().value()).isEqualTo(200);
        ProfileResponse p = r.getBody();
        assertThat(p).isNotNull();
        assertThat(p.getAddress()).isNull();
    }

    @Test
    void getProfile_withAddress_buildsAddressString() {
        User u = User.builder().userId(6).username("u6").fullName("User Six").email("e6").phone("09")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        Address a = Address.builder().addressLine("123 A").ward("W1").district("D1").city("C1").build();
        UserAddress ua = mock(UserAddress.class);
        when(ua.getAddress()).thenReturn(a);
        when(userRepository.findById(6)).thenReturn(Optional.of(u));
        when(userAddressRepository.findAllWithAddressByUserId(6)).thenReturn(List.of(ua));

        ResponseEntity<ProfileResponse> r = service.getProfile(6);
        ProfileResponse p = r.getBody();
        assertThat(p.getAddress()).contains("123 A");
        assertThat(p.getAddress()).contains("W1");
        assertThat(p.getAddress()).contains("D1");
        assertThat(p.getAddress()).contains("C1");
    }

    @Test
    void patchProfile_updatesFields_andSaves() {
        User u = User.builder().userId(7).fullName("old").email("old@x").phone("000").createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now()).build();
        when(userRepository.findById(7)).thenReturn(Optional.of(u));

        ResponseEntity<?> r = service.patchProfile(7, "New Name", "new@x", "099", null, null, null);
        assertThat(r.getStatusCode().value()).isEqualTo(200);
        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(cap.capture());
        User saved = cap.getValue();
        assertThat(saved.getFullName()).isEqualTo("New Name");
        assertThat(saved.getEmail()).isEqualTo("new@x");
        assertThat(saved.getPhone()).isEqualTo("099");
    }

    @Test
    void changePassword_rejectsShortNewPassword() {
        ResponseEntity<?> r = service.changePassword(1, "old", "123");
        assertThat(r.getStatusCode().value()).isEqualTo(400);
        assertThat(((java.util.Map<?, ?>) r.getBody()).get("message"))
                .isEqualTo("New password must be at least 6 characters");
    }

    @Test
    void changePassword_incorrectCurrent_returnsBadRequest() {
        User u = User.builder().userId(8).passwordHash("h").build();
        when(userRepository.findById(8)).thenReturn(Optional.of(u));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        ResponseEntity<?> r = service.changePassword(8, "wrong", "newpassword");
        assertThat(r.getStatusCode().value()).isEqualTo(400);
        assertThat(((java.util.Map<?, ?>) r.getBody()).get("message")).isEqualTo("Current password is incorrect");
    }

    @Test
    void changePassword_success_encodesAndSaves() {
        User u = User.builder().userId(9).passwordHash("oldhash").build();
        when(userRepository.findById(9)).thenReturn(Optional.of(u));
        when(passwordEncoder.matches(eq("oldpass"), eq("oldhash"))).thenReturn(true);
        when(passwordEncoder.encode("newpass")).thenReturn("newhash");

        ResponseEntity<?> r = service.changePassword(9, "oldpass", "newpass");
        assertThat(r.getStatusCode().value()).isEqualTo(204);
        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(cap.capture());
        assertThat(cap.getValue().getPasswordHash()).isEqualTo("newhash");
    }

    @Test
    void saveDefaultAddress_infersCity_andClearsExistingDefault() {
        User u = User.builder().userId(10).build();
        when(userRepository.findById(10)).thenReturn(Optional.of(u));
        UserAddress existing = mock(UserAddress.class);
        when(existing.getDefaultAddress()).thenReturn(Boolean.TRUE);
        when(userAddressRepository.findAllWithAddressByUserId(10)).thenReturn(List.of(existing));

        ProfileController.AddressPayload payload = new ProfileController.AddressPayload("Street 1, Da Nang, Vietnam",
                "W", "D", null, null, null);

        // capture saved address
        when(addressRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> r = service.saveDefaultAddress(10, payload);
        assertThat(r.getStatusCode().value()).isEqualTo(200);
        // One save for clearing existing default, one save for the new UserAddress
        verify(userAddressRepository, times(2)).save(any(UserAddress.class));
        verify(userAddressRepository).save(existing);
        ArgumentCaptor<Address> ac = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(ac.capture());
        Address saved = ac.getValue();
        assertThat(saved.getCity()).isEqualTo("Da Nang");
    }

    @Test
    void setDefaultAddress_togglesDefaultFlags() {
        UserAddress ua1 = mock(UserAddress.class);
        when(ua1.getUserAddressId()).thenReturn(1);
        UserAddress ua2 = mock(UserAddress.class);
        when(ua2.getUserAddressId()).thenReturn(2);
        when(userAddressRepository.findAllWithAddressByUserId(11)).thenReturn(List.of(ua1, ua2));

        ResponseEntity<?> r = service.setDefaultAddress(11, 2);
        assertThat(r.getStatusCode().value()).isEqualTo(200);
        verify(userAddressRepository, times(2)).save(any());
    }
}
