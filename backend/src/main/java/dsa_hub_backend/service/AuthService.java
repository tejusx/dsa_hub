package dsa_hub_backend.service;

import dsa_hub_backend.dto.LoginRequest;
import dsa_hub_backend.dto.RegisterRequest;
import dsa_hub_backend.dto.TokenResponse;
import dsa_hub_backend.dto.UserResponse;
import dsa_hub_backend.entity.User;
import dsa_hub_backend.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at most 72 UTF-8 bytes");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().trim());

        try {
            User savedUser = userRepository.saveAndFlush(user);
            return toUserResponse(savedUser);
        } catch (DataIntegrityViolationException exception) {
            // The database's case-insensitive unique index also protects concurrent registrations.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(this::invalidCredentials);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        return jwtService.createToken(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserInformation(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User no longer exists"));
        return toUserResponse(user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(user.getUserId(), user.getEmail(), user.getDisplayName());
    }
}
