package dsa_hub_backend.dto;

import java.time.Instant;

public record TokenResponse(String accessToken, String tokenType, Instant expiresAt) {
}
