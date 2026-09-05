package com.example.crowdtransportfeedback.analytics;

import com.example.crowdtransportfeedback.feedback.*;
import java.time.*; import java.util.*; import java.util.function.ToDoubleFunction; import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrustAggregationService {
 static final double PRIOR_MEAN=3, PRIOR_WEIGHT=2, HALF_LIFE_DAYS=30;
 private final FeedbackRepository repository; private final Clock clock;
 public TrustAggregationService(FeedbackRepository repository){this(repository,Clock.systemUTC());}
 TrustAggregationService(FeedbackRepository repository,Clock clock){this.repository=repository;this.clock=clock;}

 @Transactional(readOnly = true)
 public List<AnalyticsDtos.Cell> heatmap(AnalyticsMetric metric,TransportType type,String line,AnalyticsWindow window){
   return grouped(type,line,window).entrySet().stream().map(e->cell(e.getKey(),e.getValue(),metric)).sorted(Comparator.comparing(AnalyticsDtos.Cell::cellId)).toList();
 }
 @Transactional(readOnly = true)
 public AnalyticsDtos.Area area(String cellId,AnalyticsMetric metric,TransportType type,String line,AnalyticsWindow window){
   GeoGrid.Cell geo=GeoGrid.fromId(cellId); if(geo==null) return null;
   List<Feedback> rows=grouped(type,line,window).get(cellId); if(rows==null) return null;
   AnalyticsDtos.Cell c=cell(cellId,rows,metric);
   List<AnalyticsDtos.Preview> latest=rows.stream().sorted(Comparator.comparingLong((Feedback f)->f.createdAt).reversed()).limit(5).map(f->new AnalyticsDtos.Preview(f.feedbackId,f.owner.getUsername(),f.transportType.name(),f.line,f.score,f.createdAt)).toList();
   return new AnalyticsDtos.Area(c.cellId(),c.centerLatitude(),c.centerLongitude(),c.score(),c.confidence(),c.feedbackCount(),c.uniqueContributorCount(),c.latestCreatedAt(),c.trustScore(),c.punctualityScore(),c.cleanlinessScore(),c.crowdingComfortScore(),latest);
 }
 Map<String,List<Feedback>> grouped(TransportType type,String line,AnalyticsWindow window){
   long now=clock.millis(), cutoff=window.duration==null?Long.MIN_VALUE:now-window.duration.toMillis(); String normalized=line==null?null:line.trim().toUpperCase(Locale.ROOT);
   return repository.findAll().stream().filter(f->f.createdAt>=cutoff&&f.createdAt<=now).filter(f->type==null||f.transportType==type).filter(f->normalized==null||normalized.isBlank()||f.normalizedLine.equals(normalized)).filter(f->GeoGrid.cell(f.latitude,f.longitude)!=null).collect(Collectors.groupingBy(f->GeoGrid.cell(f.latitude,f.longitude).id()));
 }
 private AnalyticsDtos.Cell cell(String id,List<Feedback> rows,AnalyticsMetric metric){
   GeoGrid.Cell g=GeoGrid.fromId(id); int users=(int)rows.stream().map(f->f.owner.getId()).distinct().count();
   double trust=aggregate(rows,f->f.score), punctuality=aggregate(rows,f->f.punctualityScore), cleanliness=aggregate(rows,f->f.cleanlinessScore), crowding=aggregate(rows,f->f.crowdingScore);
   double selected=switch(metric){case TRUST->trust;case PUNCTUALITY->punctuality;case CLEANLINESS->cleanliness;case CROWDING->crowding;};
   long latest=rows.stream().mapToLong(f->f.createdAt).max().orElse(0);
   return new AnalyticsDtos.Cell(id,g.centerLatitude(),g.centerLongitude(),selected,ConfidenceLevel.of(users),rows.size(),users,latest,trust,punctuality,cleanliness,crowding);
 }
 double aggregate(List<Feedback> rows,ToDoubleFunction<Feedback> score){
   long now=clock.millis(); double weighted=0,total=0;
   for(List<Feedback> userRows:rows.stream().collect(Collectors.groupingBy(f->f.owner.getId())).values()){
     double uw=0,us=0; for(Feedback f:userRows){double age=Math.max(0,now-f.createdAt)/86400000d,w=Math.pow(.5,age/HALF_LIFE_DAYS);uw+=w;us+=w*Math.max(1,Math.min(5,score.applyAsDouble(f)));}
     double effective=Math.min(1,uw); weighted+=effective*(us/uw); total+=effective;
   }
   return Math.max(1,Math.min(5,(PRIOR_WEIGHT*PRIOR_MEAN+weighted)/(PRIOR_WEIGHT+total)));
 }
}
