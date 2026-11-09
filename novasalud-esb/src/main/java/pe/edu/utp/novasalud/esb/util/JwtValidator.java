package pe.edu.utp.novasalud.esb.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;

/**
 * Valida los tokens JWT con la clave compartida por todos los servicios de
 * NovaSalud (NOVASALUD_JWT_SECRET, definida en la Fase 1).
 */
public final class JwtValidator {

    private static final String DEFAULT_SECRET = "NovaSaludSystemSecretKeyHMAC256BitsLongForSecureJwtTokenSigning2026UTP";
    private static final Key SIGNING_KEY;

    static {
        String secret = System.getenv("NOVASALUD_JWT_SECRET");
        if (secret == null || secret.isBlank()) {
            secret = DEFAULT_SECRET;
        }
        byte[] keyBytes = secret.trim().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        SIGNING_KEY = Keys.hmacShaKeyFor(keyBytes);
    }

    private JwtValidator() {
    }

    /** Devuelve los datos del token si su firma y vigencia son válidas; si no, null. */
    public static Claims validar(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return Jwts.parserBuilder().setSigningKey(SIGNING_KEY).build()
                    .parseClaimsJws(token.trim()).getBody();
        } catch (Exception ex) {
            return null;
        }
    }
}
