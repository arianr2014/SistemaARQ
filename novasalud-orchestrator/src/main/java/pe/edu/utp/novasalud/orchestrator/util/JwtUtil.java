package pe.edu.utp.novasalud.orchestrator.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Properties;

public final class JwtUtil {

    private static final String DEFAULT_SECRET = "NovaSaludSystemSecretKeyHMAC256BitsLongForSecureJwtTokenSigning2026UTP";
    private static final Key SIGNING_KEY;

    static {
        String secret = loadSecret();
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 32));
            keyBytes = padded;
        }
        SIGNING_KEY = Keys.hmacShaKeyFor(keyBytes);
    }

    private JwtUtil() {}

    public static Claims validarToken(String token) {
        if (token == null || token.isBlank()) return null;
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(SIGNING_KEY)
                    .build()
                    .parseClaimsJws(token.trim())
                    .getBody();
        } catch (Exception ex) {
            AppLogger.error(JwtUtil.class, "Token JWT inválido o expirado en el Orquestador", ex);
            return null;
        }
    }

    private static String loadSecret() {
        String envSecret = System.getenv("NOVASALUD_JWT_SECRET");
        if (envSecret != null && !envSecret.isBlank()) return envSecret.trim();

        try (InputStream input = JwtUtil.class.getClassLoader().getResourceAsStream("orchestrator.properties")) {
            if (input != null) {
                Properties props = new Properties();
                props.load(input);
                String val = props.getProperty("jwt.secret");
                if (val != null && !val.isBlank()) return val.trim();
            }
        } catch (Exception ignored) {}

        return DEFAULT_SECRET;
    }

    public static String generarTokenSistema() {
        return Jwts.builder()
                .setSubject("sistema-orquestador")
                .claim("rol", "ADMINISTRADOR")
                .claim("origen", "orchestrator-resilience-scheduler")
                .setIssuedAt(new java.util.Date())
                .setExpiration(new java.util.Date(System.currentTimeMillis() + 86400000L))
                .signWith(SIGNING_KEY)
                .compact();
    }
}
