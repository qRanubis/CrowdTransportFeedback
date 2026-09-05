package com.example.crowdtransportfeedback.analytics;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.example.crowdtransportfeedback.feedback.*;
import com.example.crowdtransportfeedback.user.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Deterministic experiments which call the authoritative production aggregation primitive. */
class M9TrustEvaluationTest {
    private static final Instant NOW = Instant.parse("2026-01-15T12:00:00Z");
    private final TrustAggregationService service =
        new TrustAggregationService(mock(FeedbackRepository.class), Clock.fixed(NOW, ZoneOffset.UTC));

    @Test void m9TrustExperiments() {
        var e1 = new ArrayList<Feedback>();
        for (int i=0;i<20;i++) e1.add(row(i, i, i%2==0?3:4, 0));
        double b=mean(e1), p=proposed(e1);
        emit("E1","normal_consensus","feedback_count",20); emit("E1","normal_consensus","unique_contributors",20);
        emit("E1","normal_consensus","baseline_score",b); emit("E1","normal_consensus","proposed_score",p);
        emit("E1","normal_consensus","absolute_difference",Math.abs(b-p)); emit("E1","normal_consensus","confidence",ConfidenceLevel.of(20));
        assertTrue(Math.abs(b-p)<0.1, "independent consensus should only be slightly shrunk");

        var e2=new ArrayList<Feedback>(); for(int i=0;i<20;i++)e2.add(row(100+i,i,4,0));
        for(int i=0;i<20;i++)e2.add(row(120+i,100,1,0));
        assertUniqueFeedbackIds(e2);
        errors("E2","repeated_contributor",4,e2);
        assertTrue(Math.abs(proposed(e2)-4)<Math.abs(mean(e2)-4));

        for(int age:new int[]{30,60,90}) {
            var rows=new ArrayList<Feedback>();
            for(int i=0;i<10;i++) rows.add(row(age*100+i,i,2,age));
            for(int i=10;i<20;i++) rows.add(row(age*100+i,i,4,0));
            emit("E3","age_"+age,"age_days",age); errors("E3","age_"+age,4,rows);
            assertTrue(Math.abs(proposed(rows)-4)<Math.abs(mean(rows)-4));
        }

        var e4=List.of(row(400,0,5,0)); b=mean(e4);p=proposed(e4);
        emit("E4","sparse_extreme","baseline_score",b);emit("E4","sparse_extreme","proposed_score",p);
        emit("E4","sparse_extreme","distance_from_neutral_baseline",Math.abs(b-3));
        emit("E4","sparse_extreme","distance_from_neutral_proposed",Math.abs(p-3));
        emit("E4","sparse_extreme","extremeness_attenuation_pct",100*(Math.abs(b-3)-Math.abs(p-3))/Math.abs(b-3));
        emit("E4","sparse_extreme","confidence",ConfidenceLevel.of(1)); assertTrue(Math.abs(p-3)<Math.abs(b-3));

        var repeated=new ArrayList<Feedback>();var diverse=new ArrayList<Feedback>();
        for(int i=0;i<20;i++){repeated.add(row(500+i,0,5,0));diverse.add(row(520+i,i,5,0));}
        assertUniqueFeedbackIds(repeated); assertUniqueFeedbackIds(diverse);
        diversity("one_contributor",repeated,1); diversity("twenty_contributors",diverse,20);
        assertEquals(mean(repeated),mean(diverse));assertTrue(proposed(diverse)>proposed(repeated));

        int[] counts={1,2,3,5,6}; ConfidenceLevel[] expected={ConfidenceLevel.LOW,ConfidenceLevel.LOW,ConfidenceLevel.MEDIUM,ConfidenceLevel.MEDIUM,ConfidenceLevel.HIGH};
        for(int j=0;j<counts.length;j++){var rows=new ArrayList<Feedback>();for(int i=0;i<counts[j];i++)rows.add(row(600+j*10+i,i,4,0));
            var confidence=ConfidenceLevel.of(counts[j]);emit("E6","contributors_"+counts[j],"unique_contributors",counts[j]);emit("E6","contributors_"+counts[j],"proposed_score",proposed(rows));emit("E6","contributors_"+counts[j],"confidence",confidence);assertEquals(expected[j],confidence);}
    }

    private void errors(String id,String variant,double reference,List<Feedback> rows){double b=mean(rows),p=proposed(rows),be=Math.abs(b-reference),pe=Math.abs(p-reference);
        emit(id,variant,"reference_score",reference);emit(id,variant,"baseline_score",b);emit(id,variant,"proposed_score",p);emit(id,variant,"baseline_absolute_error",be);emit(id,variant,"proposed_absolute_error",pe);if(be>0)emit(id,variant,"distortion_reduction_pct",100*(be-pe)/be);}
    private void diversity(String variant,List<Feedback> rows,int users){emit("E5",variant,"feedback_count",rows.size());emit("E5",variant,"unique_contributors",users);emit("E5",variant,"baseline_score",mean(rows));emit("E5",variant,"proposed_score",proposed(rows));emit("E5",variant,"confidence",ConfidenceLevel.of(users));}
    private double proposed(List<Feedback> rows){return service.aggregate(rows,f->f.score);}
    private static double mean(List<Feedback> rows){return rows.stream().mapToDouble(f->f.score).average().orElse(Double.NaN);}
    private static void assertUniqueFeedbackIds(List<Feedback> rows){assertEquals(rows.size(),rows.stream().map(f->f.feedbackId).distinct().count());}
    private static void emit(String id,String variant,String metric,Object value){System.out.printf(Locale.ROOT,"M9_RESULT,%s,%s,%s,%s%n",id,variant,metric,value);}
    private static Feedback row(int observation,int contributor,int score,int ageDays){UUID uid=new UUID(0,contributor+1L);AppUser u=new AppUser(uid,"m9"+contributor+"@example.test","m9u"+contributor,"x",Role.USER,NOW.minusSeconds(86400L*ageDays));return new Feedback(new UUID(1,observation+1L),u,TransportType.METRO,"M9EVAL",score,score,score,"",44.4268,26.1025,NOW.minusSeconds(86400L*ageDays).toEpochMilli());}
}
