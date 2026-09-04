package com.example.crowdtransportfeedback.security;
import com.example.crowdtransportfeedback.user.AppUser; import io.jsonwebtoken.*; import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service; import javax.crypto.SecretKey; import java.nio.charset.StandardCharsets; import java.time.Instant; import java.util.Date; import java.util.UUID;
@Service public class JwtService {
 private final JwtProperties props; private final SecretKey key;
 public JwtService(JwtProperties props){this.props=props;if(props.secret()==null||props.secret().getBytes(StandardCharsets.UTF_8).length<32)throw new IllegalStateException("JWT_SECRET must be at least 32 bytes");key=Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));}
 public String issue(AppUser u){Instant now=Instant.now();return Jwts.builder().subject(u.id.toString()).claim("role",u.role.name()).issuer("crowd-transport-feedback").issuedAt(Date.from(now)).expiration(Date.from(now.plus(props.accessLifetime()))).signWith(key).compact();}
 public AuthenticatedUser parse(String token){Claims c=Jwts.parser().verifyWith(key).requireIssuer("crowd-transport-feedback").build().parseSignedClaims(token).getPayload();return new AuthenticatedUser(UUID.fromString(c.getSubject()),String.valueOf(c.get("role")));}
 public record AuthenticatedUser(UUID id,String role) {}
}
