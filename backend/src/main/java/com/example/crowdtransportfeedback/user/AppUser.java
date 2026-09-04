package com.example.crowdtransportfeedback.user;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="app_user")
public class AppUser {
 @Id public UUID id; @Column(nullable=false,unique=true,length=320) public String email;
 @Column(name="password_hash",nullable=false,length=100) public String passwordHash;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) public Role role;
 @Column(name="created_at",nullable=false) public Instant createdAt;
 protected AppUser() {} public AppUser(UUID id,String email,String hash,Role role,Instant at){this.id=id;this.email=email;this.passwordHash=hash;this.role=role;this.createdAt=at;}
}
