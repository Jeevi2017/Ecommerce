package com.example.Ecomm.serviceImpl;

import com.example.Ecomm.entitiy.RefreshToken;
import com.example.Ecomm.entitiy.User;
import com.example.Ecomm.repository.RefreshTokenRepository;
import com.example.Ecomm.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    // ---------- HELPERS ----------

    private User mockUser() {
        User user = new User();
        user.setId(1L);
        return user;
    }

    private RefreshToken mockRefreshToken(User user) {
        RefreshToken token = new RefreshToken();
        token.setId(1L);
        token.setUser(user);
        token.setToken("token123");
        token.setExpiryDate(Instant.now().plusSeconds(600));
        token.setRevoked(false);
        return token;
    }

    // ---------- TESTS ----------

    @Test
    void createRefreshToken_newToken() {
        User user = mockUser();

        // set @Value field
        ReflectionTestUtils.setField(refreshTokenService,
                "refreshTokenDurationMs", 60000L);

        when(refreshTokenRepository.findByUser(user)).thenReturn(Optional.empty());
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(i -> i.getArgument(0));

        RefreshToken token = refreshTokenService.createRefreshToken(user);

        assertNotNull(token);
        assertNotNull(token.getToken());
        assertFalse(token.isRevoked());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void createRefreshToken_existingToken() {
        User user = mockUser();
        RefreshToken existing = mockRefreshToken(user);

        ReflectionTestUtils.setField(refreshTokenService,
                "refreshTokenDurationMs", 60000L);

        when(refreshTokenRepository.findByUser(user)).thenReturn(Optional.of(existing));
        when(refreshTokenRepository.save(any())).thenReturn(existing);

        RefreshToken token = refreshTokenService.createRefreshToken(user);

        assertNotNull(token.getToken());
        assertFalse(token.isRevoked());
    }

    @Test
    void findByToken() {
        RefreshToken token = mockRefreshToken(mockUser());

        when(refreshTokenRepository.findByToken("token123"))
                .thenReturn(Optional.of(token));

        Optional<RefreshToken> result =
                refreshTokenService.findByToken("token123");

        assertTrue(result.isPresent());
    }

    @Test
    void verifyExpiration_validToken() {
        RefreshToken token = mockRefreshToken(mockUser());

        RefreshToken result = refreshTokenService.verifyExpiration(token);

        assertNotNull(result);
    }

    @Test
    void verifyExpiration_expiredToken() {
        RefreshToken token = mockRefreshToken(mockUser());
        token.setExpiryDate(Instant.now().minusSeconds(10));

        assertThrows(RuntimeException.class,
                () -> refreshTokenService.verifyExpiration(token));

        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void deleteRefreshToken() {
        RefreshToken token = mockRefreshToken(mockUser());

        assertDoesNotThrow(() ->
                refreshTokenService.deleteRefreshToken(token));

        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void deleteByUserId() {
        User user = mockUser();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertDoesNotThrow(() ->
                refreshTokenService.deleteByUserId(1L));

        verify(refreshTokenRepository).deleteByUser(user);
    }
}
