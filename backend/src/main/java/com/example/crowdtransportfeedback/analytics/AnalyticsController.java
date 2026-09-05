package com.example.crowdtransportfeedback.analytics;
import com.example.crowdtransportfeedback.feedback.TransportType; import java.util.List; import org.springframework.http.ResponseEntity; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/analytics") public class AnalyticsController {
 private final TrustAggregationService service; public AnalyticsController(TrustAggregationService s){service=s;}
 @GetMapping("/heatmap") List<AnalyticsDtos.Cell> heatmap(@RequestParam(defaultValue="TRUST") AnalyticsMetric metric,@RequestParam(required=false) TransportType transportType,@RequestParam(required=false) String line,@RequestParam(defaultValue="30D") String window){return service.heatmap(metric,transportType,line,AnalyticsWindow.parse(window));}
 @GetMapping("/area") ResponseEntity<AnalyticsDtos.Area> area(@RequestParam String cellId,@RequestParam(defaultValue="TRUST") AnalyticsMetric metric,@RequestParam(required=false) TransportType transportType,@RequestParam(required=false) String line,@RequestParam(defaultValue="30D") String window){return ResponseEntity.ofNullable(service.area(cellId,metric,transportType,line,AnalyticsWindow.parse(window)));}
}
