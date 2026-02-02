package com.example.Ecomm.service;

import com.example.Ecomm.entitiy.RefreshToken;
import com.example.Ecomm.entitiy.User;
import java.util.Optional;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(User user);
    Optional<RefreshToken> findByToken(String token);
    RefreshToken verifyExpiration(RefreshToken token);
    void deleteRefreshToken(RefreshToken token);
    void deleteByUserId(Long userId); 
}
