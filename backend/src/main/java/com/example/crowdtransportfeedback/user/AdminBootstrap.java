package com.example.crowdtransportfeedback.user;
import com.example.crowdtransportfeedback.auth.AuthService; import org.springframework.beans.factory.annotation.Value; import org.springframework.boot.ApplicationArguments; import org.springframework.boot.ApplicationRunner; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.stereotype.Component; import org.springframework.transaction.annotation.Transactional; import java.time.Instant; import java.util.UUID;
@Component public class AdminBootstrap implements ApplicationRunner {
 private final UserRepository users; private final PasswordEncoder encoder; private final String email,password;
 AdminBootstrap(UserRepository u,PasswordEncoder e,@Value("${app.admin.email:}")String mail,@Value("${app.admin.password:}")String pass){users=u;encoder=e;email=mail;password=pass;}
 @Override @Transactional public void run(ApplicationArguments args){if(email.isBlank()||password.isBlank())return;if(password.length()<8)throw new IllegalStateException("APP_ADMIN_PASSWORD must have at least 8 characters");String normalized=email.trim().toLowerCase();if(!users.existsByEmail(normalized))users.save(new AppUser(UUID.randomUUID(),normalized,encoder.encode(password),Role.ADMIN,Instant.now()));}
}
