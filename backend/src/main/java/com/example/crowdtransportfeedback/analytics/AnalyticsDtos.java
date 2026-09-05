package com.example.crowdtransportfeedback.analytics;
import java.util.List; import java.util.UUID;
public final class AnalyticsDtos {
 private AnalyticsDtos(){}
 public record Preview(UUID feedbackId,String createdByUsername,String transportType,String line,double overallRating,long createdAt){}
 public record Cell(String cellId,double centerLatitude,double centerLongitude,double score,ConfidenceLevel confidence,int feedbackCount,int uniqueContributorCount,long latestCreatedAt,double trustScore,double punctualityScore,double cleanlinessScore,double crowdingComfortScore){}
 public record Area(String cellId,double centerLatitude,double centerLongitude,double score,ConfidenceLevel confidence,int feedbackCount,int uniqueContributorCount,long latestCreatedAt,double trustScore,double punctualityScore,double cleanlinessScore,double crowdingComfortScore,List<Preview> latestFeedbacks){}
}
