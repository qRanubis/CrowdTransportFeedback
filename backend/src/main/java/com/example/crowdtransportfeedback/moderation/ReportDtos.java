package com.example.crowdtransportfeedback.moderation;
import jakarta.validation.constraints.Size; import java.time.Instant; import java.util.UUID;
public final class ReportDtos {
 public record CreateRequest(ReportReason reason,@Size(max=250) String details){}
 public record Created(UUID id,ReportStatus status,ReportReason reason,Instant createdAt){}
 public record Mine(boolean reported,ReportStatus status){}
 private ReportDtos(){}
}
