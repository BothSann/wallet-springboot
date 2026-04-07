package com.bothsann.wallet.auth.service;

import com.bothsann.wallet.auth.dto.AuthResponse;
import com.bothsann.wallet.auth.dto.LoginRequest;
import com.bothsann.wallet.auth.dto.RefreshTokenRequest;
import com.bothsann.wallet.auth.dto.RegisterRequest;
import com.bothsann.wallet.auth.entity.RefreshToken;
import com.bothsann.wallet.auth.repository.RefreshTokenRepository;
import com.bothsann.wallet.shared.config.JwtProperties;
import com.bothsann.wallet.shared.currency.CurrencyProperties;
import com.bothsann.wallet.shared.enums.Role;
import com.bothsann.wallet.shared.exception.AccountDeactivatedException;
import com.bothsann.wallet.shared.exception.EmailAlreadyExistsException;
import com.bothsann.wallet.shared.exception.InvalidTokenException;
import com.bothsann.wallet.shared.exception.UnsupportedCurrencyException;
import com.bothsann.wallet.shared.exception.UserNotFoundException;
import com.bothsann.wallet.user.entity.User;
import com.bothsann.wallet.user.repository.UserRepository;
import com.bothsann.wallet.auth.security.JwtService;
import com.bothsann.wallet.wallet.entity.Wallet;
import com.bothsann.wallet.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final WalletRepository walletRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CurrencyProperties currencyProperties;

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new EmailAlreadyExistsException(req.email());
        }
        User user = User.builder()
                .fullName(req.fullName())
                .email(req.email())
                .phone(req.phone())
                .password(passwordEncoder.encode(req.password()))
                .role(Role.USER)
                .isActive(true)
                .build();
        user = userRepository.save(user);
        String currency = (req.currency() != null && !req.currency().isBlank())
                ? req.currency().toUpperCase()
                : "USD";
        if (!currencyProperties.getWalletCurrencies().contains(currency)) {
            throw new UnsupportedCurrencyException(currency, currencyProperties.getWalletCurrencies());
        }
        walletRepository.save(Wallet.builder()
                .user(user)
                .balance(BigDecimal.ZERO)
                .currency(currency)
                .isDefault(true)
                .build());
        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new UserNotFoundException(req.email()));
        if (!user.isActive()) {
            throw new AccountDeactivatedException();
        }
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        return buildAuthResponse(user);
    }

    public AuthResponse refresh(RefreshTokenRequest req) {
        RefreshToken stored = refreshTokenRepository.findByToken(req.refreshToken())
                .orElseThrow(InvalidTokenException::new);
        if (stored.isRevoked() || stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException();
        }
        String newAccessToken = jwtService.generateAccessToken(stored.getUser());
        return new AuthResponse(newAccessToken, stored.getToken(), "Bearer",
                jwtProperties.getAccessExpiration() / 1000);
    }

    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(t -> {
            t.setRevoked(true);
            refreshTokenRepository.save(t);
        });
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenValue = jwtService.generateRefreshToken(user);
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(refreshTokenValue)
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtProperties.getRefreshExpiration() / 1000))
                .build();
        refreshTokenRepository.save(token);
        return new AuthResponse(accessToken, refreshTokenValue, "Bearer",
                jwtProperties.getAccessExpiration() / 1000);
    }
}
