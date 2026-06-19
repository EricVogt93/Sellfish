package de.sellfish.sso;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.sellfish.common.error.ApiException;
import de.sellfish.sso.OidcProperties.ProviderConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class OidcService {

    private static final Logger log = LoggerFactory.getLogger(OidcService.class);

    private final OidcProperties properties;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper mapper = new ObjectMapper();

    public OidcService(OidcProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClientBuilder = restClientBuilder;
    }

    public String buildAuthUrl(String providerId) {
        ProviderConfig p = requireProvider(providerId);
        String state = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        String scopes = String.join(
                " ", Optional.ofNullable(p.scopes()).orElse(java.util.List.of("openid", "email", "profile")));
        String redirect = properties.redirectUri() + "?provider=" + providerId;
        return UriComponentsBuilder.fromUriString(p.authorizationUri())
                .queryParam("response_type", "code")
                .queryParam("client_id", p.clientId())
                .queryParam("redirect_uri", redirect)
                .queryParam("scope", scopes)
                .queryParam("state", state)
                .queryParam("nonce", UUID.randomUUID().toString())
                .build()
                .toUriString();
    }

    public OidcUser exchangeCode(String providerId, String code) {
        ProviderConfig p = requireProvider(providerId);
        RestClient client = restClientBuilder.build();
        String tokenResponse = client.post()
                .uri(p.tokenUri())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body("grant_type=authorization_code"
                        + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                        + "&redirect_uri=" + URLEncoder.encode(properties.redirectUri(), StandardCharsets.UTF_8)
                        + "&client_id=" + URLEncoder.encode(p.clientId(), StandardCharsets.UTF_8)
                        + "&client_secret=" + URLEncoder.encode(p.clientSecret(), StandardCharsets.UTF_8))
                .retrieve()
                .body(String.class);

        JsonNode tokenJson = parse(tokenResponse, "Token-Response");
        String idToken = tokenJson.path("id_token").asText();
        if (idToken == null || idToken.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "No ID token received");
        }

        Claims claims = verifyIdToken(idToken, p);
        String subject = claims.getSubject();
        String email = Optional.ofNullable(claims.get("email", String.class)).orElse(subject);

        return new OidcUser(
                subject,
                providerId,
                email.toLowerCase(),
                Optional.ofNullable(claims.get("name", String.class)).orElse(email),
                Optional.ofNullable(claims.get("email_verified", Boolean.class)).orElse(false));
    }

    private Claims verifyIdToken(String idToken, ProviderConfig p) {
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length < 2) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid ID token format");
            }
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            JsonNode header = mapper.readTree(headerJson);
            String kid = header.path("kid").asText(null);
            String alg = header.path("alg").asText("RS256");

            PublicKey publicKey = fetchPublicKey(p, kid);
            return (Claims) Jwts.parser()
                    .verifyWith(publicKey)
                    .requireIssuer(p.issuer())
                    .build()
                    .parse(idToken)
                    .getPayload();
        } catch (io.jsonwebtoken.JwtException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "ID token verification failed: " + e.getMessage());
        } catch (Exception e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "ID token parsing failed: " + e.getMessage());
        }
    }

    private PublicKey fetchPublicKey(ProviderConfig p, String kid) {
        RestClient client = restClientBuilder.build();
        try {
            String jwksJson = client.get().uri(p.jwksUri()).retrieve().body(String.class);
            JsonNode jwks = mapper.readTree(jwksJson);
            for (JsonNode key : jwks.path("keys")) {
                if (kid == null || kid.equals(key.path("kid").asText())) {
                    String n = key.path("n").asText();
                    String e = key.path("e").asText();
                    BigInteger modulus =
                            new BigInteger(1, Base64.getUrlDecoder().decode(n));
                    BigInteger exponent =
                            new BigInteger(1, Base64.getUrlDecoder().decode(e));
                    RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
                    return KeyFactory.getInstance("RSA").generatePublic(spec);
                }
            }
        } catch (Exception e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "JWKS-Abruf failed: " + e.getMessage());
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "No matching JWK key found");
    }

    public boolean hasProviders() {
        return properties.enabled()
                && properties.providers() != null
                && !properties.providers().isEmpty();
    }

    public java.util.List<ProviderInfo> providerList() {
        if (!hasProviders()) return java.util.List.of();
        return properties.providers().stream()
                .map(p -> new ProviderInfo(p.id(), p.name()))
                .toList();
    }

    private ProviderConfig requireProvider(String providerId) {
        return properties.providers().stream()
                .filter(p -> p.id().equalsIgnoreCase(providerId))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("SSO provider not configured: " + providerId));
    }

    private JsonNode parse(String json, String context) {
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, context + " could not be parsed");
        }
    }

    public record OidcUser(String subject, String provider, String email, String name, boolean emailVerified) {}

    public record ProviderInfo(String id, String name) {}
}
