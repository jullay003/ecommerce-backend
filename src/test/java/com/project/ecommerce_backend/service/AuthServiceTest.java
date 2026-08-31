package com.project.ecommerce_backend.service;

import com.project.ecommerce_backend.dto.JwtResponse;
import com.project.ecommerce_backend.dto.LoginRequest;
import com.project.ecommerce_backend.dto.SignupRequest;
import com.project.ecommerce_backend.entity.Role;
import com.project.ecommerce_backend.entity.User;
import com.project.ecommerce_backend.repository.RoleRepository;
import com.project.ecommerce_backend.repository.UserRepository;
import com.project.ecommerce_backend.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void registerUser_ShouldSaveUserWithUserRole() {
        //Arrange:
        SignupRequest request = new SignupRequest();
        request.setUsername("john");
        request.setEmail("john@Example.com");
        request.setPassword("password123");
        request.setFirstName("John");
        request.setLastName("Doe");

        Role userRole = new Role("ROLE_USER");
        userRole.setId(1L);

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("john");
        savedUser.setEmail("john@Example.com");
        savedUser.setPassword("encodedPassword");
        savedUser.setEnabled(true);
        savedUser.addRole(userRole);

        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@Example.com")).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        //Act
        User result = authService.registerUser(request);

        //Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("john");
        assertThat(result.getPassword()).isEqualTo("encodedPassword");
        assertThat(result.getRoles()).extracting(Role::getName).contains("ROLE_USER");
        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode("password123");
    }

    @Test
    void registerUser_WhenUsernameExists_ShouldThrowException() {
        SignupRequest request = new SignupRequest();
        request.setUsername("john");
        request.setEmail("john@Example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("john")).thenReturn(true);

        //Act & Assert
        assertThatThrownBy(() -> authService.registerUser(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Username already taken");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_WhenEmailExists_ShouldThrowException() {
        //Arrange
        SignupRequest request = new SignupRequest();
        request.setUsername("john");
        request.setEmail("john@Example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@Example.com")).thenReturn(true);

        //Act & Assert
        assertThatThrownBy(() -> authService.registerUser(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already in use");
        verify(userRepository, never()).save(any(User.class));

    }

    @Test
    void loginUser_WhenValidCredentials_ShouldReturnJwtResponse() {
        //Arrange:
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("john");
        request.setPassword("password123");

        User user = new User();
        user.setId(1L);
        user.setUsername("john");
        user.setEmail("john@Example.com");
        user.setPassword("encodedPassword");
        user.addRole(new Role("ROLE_USER"));

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getRoles().stream()
                        .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(role.getName()))
                        .toList()
        );

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtil.generateToken(userDetails)).thenReturn("mocked-jwt-token");
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        //Act
        JwtResponse response = authService.loginUser(request);

        //Assert
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mocked-jwt-token");
        assertThat(response.getUsername()).isEqualTo("john");
        assertThat(response.getEmail()).isEqualTo("john@Example.com");
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil, times(1)).generateToken(userDetails);
    }

    @Test
    void loginUser_WhenInvalidCredentials_ShouldThrowException() {
        //Arrange
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("john");
        request.setPassword("wrongPassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Bad Credentials"));

        //Act & Assert
        assertThatThrownBy(() -> authService.loginUser(request))
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
        verify(jwtUtil, never()).generateToken(any());


    }

}
