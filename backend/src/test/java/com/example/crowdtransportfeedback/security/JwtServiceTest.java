package com.example.crowdtransportfeedback.security;
import com.example.crowdtransportfeedback.user.*; import org.junit.jupiter.api.Test; import java.time.*; import java.util.UUID; import static org.junit.jupiter.api.Assertions.*;
class JwtServiceTest {
 private final AppUser user=new AppUser(UUID.randomUUID(),"user@example.com","hash",Role.ADMIN,Instant.now());
 @Test void validTokenCarriesIdentityAndRole(){var jwt=new JwtService(new JwtProperties("a-secret-with-at-least-thirty-two-bytes-long",Duration.ofMinutes(5)));var parsed=jwt.parse(jwt.issue(user));assertEquals(user.id,parsed.id());assertEquals("ADMIN",parsed.role());}
 @Test void invalidTokenIsRejected(){var jwt=new JwtService(new JwtProperties("a-secret-with-at-least-thirty-two-bytes-long",Duration.ofMinutes(5)));assertThrows(Exception.class,()->jwt.parse("invalid"));}
 @Test void weakSecretIsRejected(){assertThrows(IllegalStateException.class,()->new JwtService(new JwtProperties("short",Duration.ofMinutes(5))));}
}
