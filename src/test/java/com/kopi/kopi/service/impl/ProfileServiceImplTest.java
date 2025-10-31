package com.kopi.kopi.service.impl;

import com.kopi.kopi.controller.ProfileController;
import com.kopi.kopi.dto.ProfileResponse;
import com.kopi.kopi.entity.Address;
import com.kopi.kopi.entity.Role;
import com.kopi.kopi.entity.Position;
import com.kopi.kopi.entity.User;
import com.kopi.kopi.entity.UserAddress;
import com.kopi.kopi.entity.enums.UserStatus;
import com.kopi.kopi.repository.AddressRepository;
import com.kopi.kopi.repository.UserAddressRepository;
import com.kopi.kopi.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserAddressRepository userAddressRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private ProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProfileServiceImpl(userRepository, userAddressRepository, addressRepository, passwordEncoder);
    }

    @AfterEach
    void tearDown() {
        // no-op
    }

    // ------------------ Helpers ------------------

    private User user(int id) {
        User u = new User();
        u.setUserId(id);
        u.setUsername("u"+id);
        u.setFullName("Full Name "+id);
        u.setEmail("u"+id+"@kopi.com");
        u.setPhone("090"+id);
        u.setCreatedAt(LocalDateTime.now().minusDays(7));
        u.setUpdatedAt(LocalDateTime.now().minusDays(1));
        Role r = new Role(); r.setName("CUSTOMER");
        u.setRole(r);
        Position p = new Position(); p.setPositionId(99); p.setPositionName("Dev");
        u.setPosition(p);
        u.setStatus(UserStatus.ACTIVE);
        u.setPasswordHash("$2a$hash");
        return u;
    }

    private Address addr(String line, String ward, String district, String city) {
        Address a = new Address();
        a.setAddressLine(line);
        a.setWard(ward);
        a.setDistrict(district);
        a.setCity(city);
        return a;
    }

    private UserAddress ua(User u, Address a, boolean isDefault) {
        UserAddress x = new UserAddress();
        x.setUser(u);
        x.setAddress(a);
        x.setDefaultAddress(isDefault);
        x.setCreatedAt(LocalDateTime.now().minusDays(2));
        return x;
    }

    // ================== getProfile ==================

    @Test
    void should_ReturnProfile_WithFormattedAddress() {
        // Given
        User u = user(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(u));

        Address a = addr("123 Street", "Ward 1", "District 3", "HCMC");
        UserAddress ua = ua(u, a, true);
        when(userAddressRepository.findAllWithAddressByUserId(1)).thenReturn(List.of(ua));

        // When
        ResponseEntity<ProfileResponse> resp = service.getProfile(1);

        // Then
        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        ProfileResponse dto = resp.getBody();
        assertThat(dto.getAddress()).isEqualTo("123 Street, Ward 1, District 3, HCMC");
        assertThat(dto.getRole()).isEqualTo("CUSTOMER");
        assertThat(dto.getPositionId()).isEqualTo(99);
        assertThat(dto.getPositionName()).isEqualTo("Dev");
        assertThat(dto.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void should_ReturnProfile_WithNullAddress_When_NoAddresses() {
        // Given
        User u = user(2);
        when(userRepository.findById(2)).thenReturn(Optional.of(u));
        when(userAddressRepository.findAllWithAddressByUserId(2)).thenReturn(List.of());

        // When
        var resp = service.getProfile(2);

        // Then
        assertThat(resp.getBody().getAddress()).isNull();
    }

    @Test
    void should_ReturnNullAddress_When_AllPartsBlank() {
        // Given
        User u = user(3);
        when(userRepository.findById(3)).thenReturn(Optional.of(u));

        Address a = addr("   ", null, " ", "");
        UserAddress ua = ua(u, a, true);
        when(userAddressRepository.findAllWithAddressByUserId(3)).thenReturn(List.of(ua));

        // When
        var resp = service.getProfile(3);

        // Then
        assertThat(resp.getBody().getAddress()).isNull();
    }

    @Test
    void should_IncludeTimestamps_AndBasics_InProfile() {
        // Given
        User u = user(4);
        when(userRepository.findById(4)).thenReturn(Optional.of(u));
        when(userAddressRepository.findAllWithAddressByUserId(4)).thenReturn(List.of());

        // When
        var resp = service.getProfile(4);

        // Then
        ProfileResponse dto = resp.getBody();
        assertThat(dto.getUserId()).isEqualTo(4);
        assertThat(dto.getUsername()).isEqualTo("u4");
        assertThat(dto.getDisplayName()).isEqualTo("Full Name 4");
        assertThat(dto.getCreatedAt()).isNotNull();
        assertThat(dto.getUpdatedAt()).isNotNull();
    }

    // ================== patchProfile ==================

    @Test
    void should_Update_DisplayEmailPhone_AndUpdatedAt() {
        // Given
        User u = user(10);
        when(userRepository.findById(10)).thenReturn(Optional.of(u));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        var resp = service.patchProfile(10, "New Name", "new@kopi.com", "0123", null, null, null);

        // Then
        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        assertThat(u.getFullName()).isEqualTo("New Name");
        assertThat(u.getEmail()).isEqualTo("new@kopi.com");
        assertThat(u.getPhone()).isEqualTo("0123");
        assertThat(u.getUpdatedAt()).isNotNull();
        verify(userRepository).save(u);
    }

    @Test
    void should_PartiallyUpdate_When_SomeFieldsNull() {
        // Given
        User u = user(11);
        String oldEmail = u.getEmail();
        String oldPhone = u.getPhone();
        when(userRepository.findById(11)).thenReturn(Optional.of(u));

        // When
        service.patchProfile(11, "Only Name", null, null, null, null, null);

        // Then
        assertThat(u.getFullName()).isEqualTo("Only Name");
        assertThat(u.getEmail()).isEqualTo(oldEmail);
        assertThat(u.getPhone()).isEqualTo(oldPhone);
        verify(userRepository).save(u);
    }

    @Test
    void should_ReturnOk_OnPatch() {
        // Given
        User u = user(12);
        when(userRepository.findById(12)).thenReturn(Optional.of(u));

        // When
        var resp = service.patchProfile(12, null, null, null, null, null, null);

        // Then
        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
    }

    @Test
    void should_Throw_When_UserNotFound_OnPatch() {
        // Given
        when(userRepository.findById(404)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.patchProfile(404, null, null, null, null, null, null))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    // ================== changePassword ==================

    @Test
    void should_ReturnBadRequest_When_NewPasswordTooShort() {
        // Given
        // When
        var resp = service.changePassword(1, "current", "123");

        // Then
        assertThat(resp.getStatusCodeValue()).isEqualTo(400);
        assertThat(((java.util.Map<?,?>)resp.getBody()).get("message"))
                .isEqualTo("New password must be at least 6 characters");
        verify(userRepository, never()).save(any());
    }

    @Test
    void should_ReturnBadRequest_When_CurrentPasswordIncorrect() {
        // Given
        User u = user(20);
        when(userRepository.findById(20)).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("wrong", u.getPasswordHash())).thenReturn(false);

        // When
        var resp = service.changePassword(20, "wrong", "correct123");

        // Then
        assertThat(resp.getStatusCodeValue()).isEqualTo(400);
        assertThat(((java.util.Map<?,?>)resp.getBody()).get("message"))
                .isEqualTo("Current password is incorrect");
        verify(userRepository, never()).save(any());
    }

    @Test
    void should_ChangePassword_When_Valid() {
        // Given
        User u = user(21);
        when(userRepository.findById(21)).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("oldpass", u.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("newpass")).thenReturn("HASHED");

        // When
        var resp = service.changePassword(21, "oldpass", "newpass");

        // Then
        assertThat(resp.getStatusCodeValue()).isEqualTo(204);
        assertThat(u.getPasswordHash()).isEqualTo("HASHED");
        assertThat(u.getUpdatedAt()).isNotNull();
        verify(userRepository).save(u);
    }

    @Test
    void should_Throw_When_UserNotFound_OnChangePassword() {
        // Given
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.changePassword(999, "x", "123456"))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    // ================== saveDefaultAddress ==================

    @Test
    void should_TurnOffExistingDefault_AndCreateNewDefault() {
        // Given
        User u = user(30);
        when(userRepository.findById(30)).thenReturn(Optional.of(u));

        UserAddress oldDefault = ua(u, addr("old", "w", "d", "c"), true);
        UserAddress notDefault = ua(u, addr("other", "w", "d", "c"), false);
        when(userAddressRepository.findAllWithAddressByUserId(30)).thenReturn(List.of(oldDefault, notDefault));

        Address savedAddr = new Address();
        savedAddr.setAddressId(777);
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

        when(userAddressRepository.save(any(UserAddress.class))).thenAnswer(inv -> inv.getArgument(0));

        ProfileController.AddressPayload payload = mock(ProfileController.AddressPayload.class);
        when(payload.address_line()).thenReturn("123 New");
        when(payload.ward()).thenReturn("W1");
        when(payload.district()).thenReturn("D1");
        when(payload.city()).thenReturn("C1");
        when(payload.latitude()).thenReturn(10.1);
        when(payload.longitude()).thenReturn(20.2);

        // When
        var resp = service.saveDefaultAddress(30, payload);

        // Then
        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        assertThat(oldDefault.getDefaultAddress()).isFalse();
        // verify new UserAddress default = true
        ArgumentCaptor<UserAddress> uaCaptor = ArgumentCaptor.forClass(UserAddress.class);
        verify(userAddressRepository, times(2)).save(uaCaptor.capture()); // 1 lần tắt default cũ, 1 lần tạo mới
        UserAddress created = uaCaptor.getAllValues().get(1);
        assertThat(created.getDefaultAddress()).isTrue();
        assertThat(created.getUser()).isEqualTo(u);
        assertThat(created.getAddress()).isNotNull();
    }

    @Test
    void should_CreateDefault_When_NoExisting() {
        // Given
        User u = user(31);
        when(userRepository.findById(31)).thenReturn(Optional.of(u));
        when(userAddressRepository.findAllWithAddressByUserId(31)).thenReturn(List.of());
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userAddressRepository.save(any(UserAddress.class))).thenAnswer(inv -> inv.getArgument(0));

        ProfileController.AddressPayload payload = mock(ProfileController.AddressPayload.class);
        when(payload.address_line()).thenReturn("A");
        when(payload.ward()).thenReturn("B");
        when(payload.district()).thenReturn("C");
        when(payload.city()).thenReturn("D");
        when(payload.latitude()).thenReturn(1.0);
        when(payload.longitude()).thenReturn(2.0);

        // When
        var resp = service.saveDefaultAddress(31, payload);

        // Then
        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        ArgumentCaptor<UserAddress> captor = ArgumentCaptor.forClass(UserAddress.class);
        verify(userAddressRepository).save(captor.capture());
        assertThat(captor.getValue().getDefaultAddress()).isTrue();
    }

    @Test
    void should_SaveAddress_WithCorrectFields() {
        // Given
        User u = user(32);
        when(userRepository.findById(32)).thenReturn(Optional.of(u));
        when(userAddressRepository.findAllWithAddressByUserId(32)).thenReturn(null);
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userAddressRepository.save(any(UserAddress.class))).thenAnswer(inv -> inv.getArgument(0));

        ProfileController.AddressPayload payload = mock(ProfileController.AddressPayload.class);
        when(payload.address_line()).thenReturn("Line");
        when(payload.ward()).thenReturn("Ward");
        when(payload.district()).thenReturn("Dist");
        when(payload.city()).thenReturn("City");
        when(payload.latitude()).thenReturn(9.9);
        when(payload.longitude()).thenReturn(8.8);

        // When
        service.saveDefaultAddress(32, payload);

        // Then
        ArgumentCaptor<Address> aCap = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(aCap.capture());
        Address saved = aCap.getValue();
        assertThat(saved.getAddressLine()).isEqualTo("Line");
        assertThat(saved.getWard()).isEqualTo("Ward");
        assertThat(saved.getDistrict()).isEqualTo("Dist");
        assertThat(saved.getCity()).isEqualTo("City");
        assertThat(saved.getLatitude()).isEqualTo(9.9);
        assertThat(saved.getLongitude()).isEqualTo(8.8);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void should_ReturnOk_AndInteractInOrder_When_SaveDefaultAddress() {
        // Given
        User u = user(33);
        when(userRepository.findById(33)).thenReturn(Optional.of(u));
        when(userAddressRepository.findAllWithAddressByUserId(33)).thenReturn(List.of());
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userAddressRepository.save(any(UserAddress.class))).thenAnswer(inv -> inv.getArgument(0));

        ProfileController.AddressPayload payload = mock(ProfileController.AddressPayload.class);
        when(payload.address_line()).thenReturn("Z");
        when(payload.ward()).thenReturn("W");
        when(payload.district()).thenReturn("Q");
        when(payload.city()).thenReturn("E");
        when(payload.latitude()).thenReturn(1.23);
        when(payload.longitude()).thenReturn(4.56);

        // When
        var resp = service.saveDefaultAddress(33, payload);

        // Then
        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        InOrder inOrder = inOrder(userAddressRepository, addressRepository);
        inOrder.verify(userAddressRepository).findAllWithAddressByUserId(33);
        inOrder.verify(addressRepository).save(any(Address.class));
        inOrder.verify(userAddressRepository).save(any(UserAddress.class));
    }
}
