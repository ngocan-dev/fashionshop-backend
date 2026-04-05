package com.example.fashionshop.security.jwt;

import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    private final Map<String, Date> blacklistedTokens = new ConcurrentHashMap<>();

    public void blacklistToken(String token, Date expiry) {
        if (token == null || token.isBlank() || expiry == null) {
            return;
        }
        cleanupExpiredTokens();
        blacklistedTokens.put(token, expiry);
    }

    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        cleanupExpiredTokens();
        Date expiry = blacklistedTokens.get(token);
        return expiry != null && expiry.after(new Date());
    }

    private void cleanupExpiredTokens() {
        Date now = new Date();
        blacklistedTokens.entrySet().removeIf(entry -> !entry.getValue().after(now));
    }
}
