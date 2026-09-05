package com.example.crowdtransportfeedback.gamification;

import com.example.crowdtransportfeedback.common.ApiException;
import com.example.crowdtransportfeedback.feedback.*;
import com.example.crowdtransportfeedback.user.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GamificationServiceTest {
    GamificationEventRepository events = mock(GamificationEventRepository.class);
    UserAchievementRepository achievements = mock(UserAchievementRepository.class);
    FeedbackRepository feedback = mock(FeedbackRepository.class);
    GamificationService service = new GamificationService(events, achievements, feedback);
    AppUser user = new AppUser(UUID.randomUUID(), "u@example.com", "userone", "hash", Role.USER, Instant.EPOCH);
    Set<String> ledger = new HashSet<>();

    @BeforeEach void idempotentLedger() {
        when(events.existsByUserIdAndTypeAndSourceKey(any(), anyString(), anyString())).thenAnswer(i -> ledger.contains(i.getArgument(1)+":"+i.getArgument(2)));
        when(events.saveAndFlush(any())).thenAnswer(i -> { GamificationEvent e=i.getArgument(0); ledger.add(e.type+":"+e.sourceKey); return e; });
        when(feedback.findByOwnerId(user.id)).thenReturn(new ArrayList<>());
    }

    @Test void firstFeedbackAwards120AndRetryAwardsZero() {
        Feedback item=item(UUID.randomUUID(), TransportType.METRO, "M5", 0);
        assertEquals(120, service.award(user,item).xpAwarded());
        assertEquals(0, service.award(user,item).xpAwarded());
    }

    @Test void repeatKnownLineIs10KnownTransportNewLineIs40AndNewTypeLineIs80() {
        service.award(user,item(UUID.randomUUID(),TransportType.METRO,"M5",0));
        assertEquals(10,service.award(user,item(UUID.randomUUID(),TransportType.METRO,"M5",1)).xpAwarded());
        assertEquals(40,service.award(user,item(UUID.randomUUID(),TransportType.METRO,"M4",2)).xpAwarded());
        assertEquals(80,service.award(user,item(UUID.randomUUID(),TransportType.BUS,"100",3)).xpAwarded());
    }

    @Test void deleteOnlyRevokesBaseAndRepeatedDeleteIsIdempotent() {
        Feedback item=item(UUID.randomUUID(),TransportType.BUS,"1",0); service.award(user,item);
        service.revoke(user,item.feedbackId); service.revoke(user,item.feedbackId);
        verify(events, times(5)).saveAndFlush(any()); // four awards plus exactly one reversal
        assertTrue(ledger.contains("FEEDBACK_BASE_REVOKED:"+item.feedbackId));
    }

    @Test void cooldownUsesAbsoluteOpenIntervalAndKeepsExactThirtyMinutesValid() {
        UUID id=user.id; String line="METRO:M5"; long at=Instant.parse("2026-01-01T12:00:00Z").toEpochMilli();
        when(events.cooldown(eq(id),eq(line),any(),any())).thenReturn(true);
        assertEquals("feedback_cooldown",assertThrows(ApiException.class,()->service.enforceCooldown(id,line,at)).code);
        verify(events).cooldown(id,line,Instant.ofEpochMilli(at).minus(Duration.ofMinutes(30)),Instant.ofEpochMilli(at).plus(Duration.ofMinutes(30)));
        reset(events); when(events.cooldown(any(),any(),any(),any())).thenReturn(false);
        assertDoesNotThrow(()->service.enforceCooldown(id,line,at));
    }

    @Test void historicalReplayUsesTimestampWhenEachThresholdWasReachedAndIsIdempotent() {
        List<Feedback> rows=new ArrayList<>(); for(int day=0;day<5;day++) rows.add(item(UUID.randomUUID(),TransportType.BUS,"L"+day,day*86_400_000L));
        when(feedback.findByOwnerId(user.id)).thenReturn(rows);
        Map<String,UserAchievement> unlocked=new HashMap<>(); when(achievements.findByUserIdAndCode(eq(user.id),anyString())).thenAnswer(i->Optional.ofNullable(unlocked.get(i.getArgument(1))));
        List<UserAchievement> saved=new ArrayList<>(); when(achievements.save(any())).thenAnswer(i->{UserAchievement a=i.getArgument(0);unlocked.put(a.code,a);saved.add(a);return a;});
        service.backfillAchievements(user); service.backfillAchievements(user);
        UserAchievement gettingStarted=saved.stream().filter(a->a.code.equals("GETTING_STARTED")).findFirst().orElseThrow();
        assertEquals(Instant.ofEpochMilli(4*86_400_000L),gettingStarted.unlockedAt);
        assertEquals(saved.size(),unlocked.size());
    }

    @Test void hibernateProxyUsesGetterForAwardsAchievementsLifetimeBonusesAndRevocation() {
        UUID realId=UUID.randomUUID();
        AppUser proxy=new AppUser(realId,"proxy@example.com","proxyuser","hash",Role.USER,Instant.EPOCH){@Override public UUID getId(){return realId;}};
        proxy.id=null;
        Feedback first=itemFor(proxy,UUID.randomUUID(),TransportType.METRO,"M5",0);
        when(feedback.findByOwnerId(realId)).thenReturn(List.of(first));
        assertEquals(120,service.award(proxy,first).xpAwarded());
        verify(feedback,atLeastOnce()).findByOwnerId(realId);
        verify(achievements,atLeastOnce()).existsByUserIdAndCode(eq(realId),anyString());

        Feedback second=itemFor(proxy,UUID.randomUUID(),TransportType.METRO,"M5",1);
        assertEquals(10,service.award(proxy,second).xpAwarded());
        service.revoke(proxy,first.feedbackId);
        verify(events).existsByUserIdAndTypeAndSourceKey(realId,"FEEDBACK_BASE_REVOKED",first.feedbackId.toString());
    }

    private Feedback item(UUID id,TransportType type,String line,long at){Feedback f=new TestFeedback();f.feedbackId=id;f.owner=user;f.transportType=type;f.line=line;f.normalizedLine=GamificationService.normalizeLine(line);f.createdAt=at;return f;}
    private Feedback itemFor(AppUser owner,UUID id,TransportType type,String line,long at){Feedback f=item(id,type,line,at);f.owner=owner;return f;}
    static class TestFeedback extends Feedback { TestFeedback(){super();} }
}
