package com.example.crowdtransportfeedback.moderation;
import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog,UUID>{}
