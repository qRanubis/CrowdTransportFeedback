package com.example.crowdtransportfeedback.auth;
import com.example.crowdtransportfeedback.common.ApiException; import com.example.crowdtransportfeedback.security.JwtService; import com.example.crowdtransportfeedback.user.*;
import org.springframework.beans.factory.annotation.Value; import org.springframework.http.HttpStatus; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets; import java.security.*; import java.time.*; import java.util.*; import static com.example.crowdtransportfeedback.auth.AuthDtos.*;
@Service public class AuthService {
 private final UserRepository users; private final RefreshSessionRepository sessions; private final PasswordEncoder passwords; private final JwtService jwt; private final Duration inactivity; private final SecureRandom random=new SecureRandom();
 AuthService(UserRepository u,RefreshSessionRepository s,PasswordEncoder p,JwtService j,@Value("${app.refresh-inactivity:P90D}")Duration d){users=u;sessions=s;passwords=p;jwt=j;inactivity=d;}
 @Transactional public AuthResponse register(Credentials c){String email=normalize(c.email());if(users.existsByEmail(email))throw new ApiException(HttpStatus.CONFLICT,"email_exists","An account already exists for this email");AppUser u=users.save(new AppUser(UUID.randomUUID(),email,passwords.encode(c.password()),Role.USER,Instant.now()));return create(u);}
 @Transactional public AuthResponse login(Credentials c){AppUser u=users.findByEmail(normalize(c.email())).filter(x->passwords.matches(c.password(),x.passwordHash)).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"invalid_credentials","Invalid email or password"));return create(u);}
 @Transactional public AuthResponse refresh(String raw){RefreshSession old=sessions.findByTokenHash(hash(raw)).orElseThrow(AuthService::invalid);Instant now=Instant.now();if(old.revokedAt!=null||!old.expiresAt.isAfter(now))throw invalid();old.revokedAt=now;old.lastUsedAt=now;var pair=newToken(old.user,now);old.replacedBy=pair.session.id;sessions.save(old);sessions.save(pair.session);return response(old.user,pair.raw);}
 @Transactional public void logout(String raw){sessions.findByTokenHash(hash(raw)).ifPresent(s->{if(s.revokedAt==null)s.revokedAt=Instant.now();});}
 private AuthResponse create(AppUser u){var p=newToken(u,Instant.now());sessions.save(p.session);return response(u,p.raw);}
 private Pair newToken(AppUser u,Instant now){byte[] b=new byte[32];random.nextBytes(b);String raw=Base64.getUrlEncoder().withoutPadding().encodeToString(b);return new Pair(raw,new RefreshSession(UUID.randomUUID(),u,hash(raw),now,now.plus(inactivity)));}
 private AuthResponse response(AppUser u,String refresh){return new AuthResponse(jwt.issue(u),refresh,new UserSummary(u.id,u.email,u.role));}
 static String normalize(String e){return e.trim().toLowerCase(Locale.ROOT);} static ApiException invalid(){return new ApiException(HttpStatus.UNAUTHORIZED,"invalid_refresh","Refresh session is invalid or expired");}
 static String hash(String raw){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 private record Pair(String raw,RefreshSession session){}
}
