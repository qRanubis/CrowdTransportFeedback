package com.example.crowdtransportfeedback.demo;

import com.example.crowdtransportfeedback.feedback.*; import com.example.crowdtransportfeedback.user.*;
import java.nio.charset.StandardCharsets; import java.time.*; import java.util.*;
import org.springframework.boot.*; import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty; import org.springframework.context.annotation.Profile; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.stereotype.Component;

/** LOCAL DEVELOPMENT ONLY. Requires both the demo profile and an explicit opt-in property. */
@Component @Profile("demo") @ConditionalOnProperty(name="app.demo-seed-enabled",havingValue="true")
public class DemoDataSeed implements ApplicationRunner {
 private final UserRepository users; private final FeedbackRepository feedback; private final PasswordEncoder encoder;
 public DemoDataSeed(UserRepository u,FeedbackRepository f,PasswordEncoder e){users=u;feedback=f;encoder=e;}
 public void run(ApplicationArguments ignored){
   Instant base=Instant.now(); List<AppUser> demoUsers=new ArrayList<>();
   for(int i=1;i<=8;i++){final int n=i;UUID id=id("user-"+n); AppUser u=users.findById(id).orElseGet(()->users.save(new AppUser(id,"demo"+n+"@local.invalid","demo_user_"+n,encoder.encode("demo-only-password"),Role.USER,base)));demoUsers.add(u);}
   String[][] places={{"Unirii","44.4270","26.1030"},{"Universitate","44.4356","26.1025"},{"Victoriei","44.4525","26.0865"},{"Romana","44.4460","26.0970"},{"Crangasi","44.4520","26.0460"}};
   TransportType[] types={TransportType.BUS,TransportType.BUS,TransportType.METRO,TransportType.METRO,TransportType.TRAM,TransportType.TRAM,TransportType.TROLLEYBUS,TransportType.NIGHT_BUS}; String[] lines={"100","205","M1","M2","1","41","69","N1"}; int[] ages={0,1,3,6,10,18,34,60,85,4};
   for(int i=0;i<50;i++){UUID fid=id("feedback-"+i);if(feedback.existsById(fid))continue;int p=i%5, quality=switch(p){case 0->2;case 1->3;case 2->5;case 3->3;default->2;};double lat=Double.parseDouble(places[p][1])+(i%4)*.00028,lon=Double.parseDouble(places[p][2])+((i/4)%4)*.00032;int punctual=Math.max(1,Math.min(5,quality+(i%3)-1)),clean=Math.max(1,Math.min(5,quality+(p==0?1:0))),crowd=Math.max(1,Math.min(5,quality+(p==3?2:0)-(p==2?1:0)));feedback.save(new Feedback(fid,demoUsers.get(i%8),types[i%8],lines[i%8],punctual,clean,crowd,"Deterministic demo report near "+places[p][0],lat,lon,base.minus(Duration.ofDays(ages[i%ages.length])).toEpochMilli()));}
 }
 private static UUID id(String value){return UUID.nameUUIDFromBytes(("m7-demo:"+value).getBytes(StandardCharsets.UTF_8));}
}
