package com.example.crowdtransportfeedback.gamification;
import com.example.crowdtransportfeedback.common.ApiException; import com.example.crowdtransportfeedback.feedback.*; import com.example.crowdtransportfeedback.user.AppUser;
import java.text.Normalizer; import java.time.*; import java.util.*; import org.springframework.http.HttpStatus; import org.springframework.stereotype.Service;
@Service public class GamificationService {
 public static final int BASE_XP=10,FIRST_XP=40,TYPE_XP=40,LINE_XP=30; private final GamificationEventRepository events; private final UserAchievementRepository achievements; private final FeedbackRepository feedback;
 public GamificationService(GamificationEventRepository e,UserAchievementRepository a,FeedbackRepository f){events=e;achievements=a;feedback=f;}
 public static String normalizeLine(String line){return Normalizer.normalize(line.trim(),Normalizer.Form.NFKC).replaceAll("\\s+"," ").toUpperCase(Locale.ROOT);}
 public record Award(int xpAwarded,List<String> newAchievements){}
 public void enforceCooldown(UUID user,String identity,long millis){Instant at=Instant.ofEpochMilli(millis);if(events.cooldown(user,identity,at.minus(Duration.ofMinutes(30)),at.plus(Duration.ofMinutes(30))))throw new ApiException(HttpStatus.CONFLICT,"feedback_cooldown","Only one feedback per line is allowed every 30 minutes");}
 public Award award(AppUser user,Feedback item){String id=item.transportType+":"+item.normalizedLine;Instant at=Instant.ofEpochMilli(item.createdAt);int xp=0;
  xp+=add(user,"FEEDBACK_BASE_AWARDED",item.feedbackId.toString(),BASE_XP,id,at);
  xp+=add(user,"FIRST_CONTRIBUTION_BONUS","LIFETIME",FIRST_XP,null,at); xp+=add(user,"NEW_TRANSPORT_TYPE_BONUS",item.transportType.name(),TYPE_XP,null,at); xp+=add(user,"NEW_LINE_BONUS",id,LINE_XP,null,at);
  return new Award(xp,evaluate(user,at)); }
 public void revoke(AppUser user,UUID feedbackId){if(events.existsByUserIdAndTypeAndSourceKey(user.id,"FEEDBACK_BASE_AWARDED",feedbackId.toString()))add(user,"FEEDBACK_BASE_REVOKED",feedbackId.toString(),-BASE_XP,null,Instant.now());evaluate(user,Instant.now());}
 private int add(AppUser u,String type,String source,int xp,String identity,Instant at){if(events.existsByUserIdAndTypeAndSourceKey(u.id,type,source))return 0;events.saveAndFlush(new GamificationEvent(u,type,source,xp,identity,at));return xp;}
 public List<String> evaluate(AppUser user,Instant at){var rows=feedback.findByOwnerId(user.id);long count=rows.size(),lines=rows.stream().map(f->f.transportType+":"+f.normalizedLine).distinct().count(),types=rows.stream().map(f->f.transportType).distinct().count(),days=rows.stream().map(f->Instant.ofEpochMilli(f.createdAt).atZone(ZoneOffset.UTC).toLocalDate()).distinct().count();List<String> fresh=new ArrayList<>();
  for(var d:AchievementCatalog.ALL){long p=switch(d.kind()){case CONTRIBUTIONS->count;case LINES->lines;case TYPES->types;case ACTIVE_DAYS->days;case TYPE_COUNT->rows.stream().filter(f->f.transportType.name().equals(d.type())).count();};if(p>=d.target()&&!achievements.existsByUserIdAndCode(user.id,d.code())){achievements.save(new UserAchievement(user,d.code(),at));fresh.add(d.code());}} return fresh;}
 public void backfillAchievements(AppUser user){var rows=feedback.findByOwnerId(user.id).stream().sorted(Comparator.comparingLong(f->f.createdAt)).toList();Set<String> lines=new HashSet<>(),types=new HashSet<>();Set<java.time.LocalDate> days=new HashSet<>();Map<String,Long> typeCounts=new HashMap<>();long contributions=0;
  for(var row:rows){contributions++;lines.add(row.transportType+":"+row.normalizedLine);types.add(row.transportType.name());days.add(Instant.ofEpochMilli(row.createdAt).atZone(ZoneOffset.UTC).toLocalDate());typeCounts.merge(row.transportType.name(),1L,Long::sum);Instant reached=Instant.ofEpochMilli(row.createdAt);
   for(var d:AchievementCatalog.ALL){long p=switch(d.kind()){case CONTRIBUTIONS->contributions;case LINES->lines.size();case TYPES->types.size();case ACTIVE_DAYS->days.size();case TYPE_COUNT->typeCounts.getOrDefault(d.type(),0L);};if(p>=d.target()){var existing=achievements.findByUserIdAndCode(user.id,d.code());if(existing.isEmpty())achievements.save(new UserAchievement(user,d.code(),reached));else if(existing.get().unlockedAt.isAfter(reached))existing.get().unlockedAt=reached;}}
  }}
}
