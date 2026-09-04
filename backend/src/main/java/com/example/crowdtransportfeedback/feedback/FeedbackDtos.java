package com.example.crowdtransportfeedback.feedback;
import jakarta.validation.constraints.*; import java.util.UUID; import com.fasterxml.jackson.annotation.JsonAlias;
public final class FeedbackDtos { private FeedbackDtos(){}
 public record Request(@NotNull @JsonAlias("id") UUID feedbackId,String transportType,@Size(max=32)String line,@Min(1)@Max(5)int score,@Min(1)@Max(5)Integer punctualityScore,@Min(1)@Max(5)Integer cleanlinessScore,@Min(1)@Max(5)Integer crowdingScore,@Size(max=2000)String comment,Double latitude,Double longitude,@PositiveOrZero long createdAt){}
 public record Response(UUID feedbackId,String id,UUID createdByUserId,String transportType,String line,int score,Integer punctualityScore,Integer cleanlinessScore,Integer crowdingScore,String comment,Double latitude,Double longitude,long createdAt){}
}
