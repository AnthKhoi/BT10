package vn.iotstar.services;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service xu ly JSON Web Token su dung thu vien Nimbus JOSE + JWT
 * Thay the cho thu vien JJWT (io.jsonwebtoken).
 */
@Service
public class JwtService {

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.expiration-time}")
    private long jwtExpiration;

    public String extractUsername(String token) {
        return extractClaim(token, JWTClaimsSet::getSubject);
    }

    public <T> T extractClaim(String token, Function<JWTClaimsSet, T> claimsResolver) {
        final JWTClaimsSet claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    public long getExpirationTime() {
        return jwtExpiration;
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        try {
            // 1. Tao Header voi thuat toan HMAC-SHA256 (HS256)
            JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);

            // 2. Tao ClaimsSet Payload
            Date now = new Date(System.currentTimeMillis());
            Date expiryDate = new Date(System.currentTimeMillis() + expiration);

            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .subject(userDetails.getUsername())
                    .issueTime(now)
                    .expirationTime(expiryDate);

            if (extraClaims != null) {
                extraClaims.forEach(claimsBuilder::claim);
            }

            JWTClaimsSet claimsSet = claimsBuilder.build();

            // 3. Ky token bang MACSigner cua Nimbus
            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            JWSSigner signer = new MACSigner(getSigningKeyBytes());
            signedJWT.sign(signer);

            // 4. Tra ve chuoi JWT format: header.payload.signature
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Error signing JWT with Nimbus: " + e.getMessage(), e);
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username != null && username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        Date expiration = extractExpiration(token);
        return expiration != null && expiration.before(new Date());
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, JWTClaimsSet::getExpirationTime);
    }

    public JWTClaimsSet extractAllClaims(String token) {
        try {
            // 1. Parse chuoi JWT thanh SignedJWT
            SignedJWT signedJWT = SignedJWT.parse(token);

            // 2. Xac minh chu ky bang MACVerifier cua Nimbus
            JWSVerifier verifier = new MACVerifier(getSigningKeyBytes());
            if (!signedJWT.verify(verifier)) {
                throw new BadCredentialsException("The JWT signature is invalid");
            }

            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

            // 3. Kiem tra han su dung cua Token
            Date expirationTime = claimsSet.getExpirationTime();
            if (expirationTime != null && expirationTime.before(new Date())) {
                throw new BadCredentialsException("The JWT token has expired");
            }

            return claimsSet;
        } catch (BadCredentialsException e) {
            throw e;
        } catch (Exception e) {
            throw new BadCredentialsException("The JWT token is invalid: " + e.getMessage(), e);
        }
    }

    private byte[] getSigningKeyBytes() {
        byte[] keyBytes = null;
        try {
            keyBytes = Base64.getDecoder().decode(secretKey);
        } catch (Exception ignored) {
        }

        if (keyBytes == null || keyBytes.length < 32) {
            keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        }

        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            return padded;
        }
        return keyBytes;
    }
}
