package com.example.crowdtransportfeedback.profile;

import com.example.crowdtransportfeedback.common.ApiException;
import com.example.crowdtransportfeedback.gamification.*;
import com.example.crowdtransportfeedback.user.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProfileServiceTest {
    UserRepository users=mock(UserRepository.class); GamificationEventRepository events=mock(GamificationEventRepository.class); UserAchievementRepository achievements=mock(UserAchievementRepository.class); JdbcTemplate jdbc=mock(JdbcTemplate.class);
    ProfileService service=new ProfileService(users,events,achievements,jdbc); UUID id=UUID.randomUUID(); AppUser user=new AppUser(id,"private@example.com","publicuser","hash",Role.USER,Instant.EPOCH);
    @BeforeEach void user(){when(users.findById(id)).thenReturn(Optional.of(user));}
    @Test void acceptsOnlyThreeBuiltInAvatars(){service.avatar(id,"NAVIGATOR");assertEquals("NAVIGATOR",user.avatarKey);assertEquals("invalid_avatar",assertThrows(ApiException.class,()->service.avatar(id,"UPLOADED_URL")).code);}
    @Test void pinsRejectDuplicatesFourAndLockedBadges(){assertThrows(ApiException.class,()->service.pins(id,List.of("A","A")));assertThrows(ApiException.class,()->service.pins(id,List.of("A","B","C","D")));when(achievements.existsByUserIdAndCode(id,"LOCKED")).thenReturn(false);assertEquals("achievement_locked",assertThrows(ApiException.class,()->service.pins(id,List.of("LOCKED"))).code);}
    @Test void pinOrderIsWrittenExactlyAsRequested(){when(achievements.existsByUserIdAndCode(eq(id),anyString())).thenReturn(true);service.pins(id,List.of("SECOND","FIRST"));var ordered=inOrder(jdbc);ordered.verify(jdbc).update("delete from pinned_achievement where user_id=?",id);ordered.verify(jdbc).update("insert into pinned_achievement(user_id,achievement_code,display_order) values(?,?,?)",id,"SECOND",0);ordered.verify(jdbc).update("insert into pinned_achievement(user_id,achievement_code,display_order) values(?,?,?)",id,"FIRST",1);}
    @Test void fullCatalogOrderNeverDependsOnPins(){var ordered=ProfileService.orderedDefinitions(List.of("METRO_EXPLORER"),false);assertEquals("Contribution",ordered.getFirst().category());assertEquals(List.of("Contribution","Network Exploration","Transport","Consistency","Community Moderation"),ordered.stream().map(AchievementCatalog.Definition::category).distinct().toList());}
    @Test void publicPinsKeepExplicitOrder(){var ordered=ProfileService.orderedDefinitions(List.of("METRO_EXPLORER","FIRST_CONTRIBUTION"),true);assertEquals(List.of("METRO_EXPLORER","FIRST_CONTRIBUTION"),ordered.stream().map(AchievementCatalog.Definition::code).toList());}
    @Test void unlockedProgressAlwaysEqualsTargetWhileLockedProgressRemainsLive(){assertEquals(1,ProfileService.displayProgress(3,1,true));assertEquals(25,ProfileService.displayProgress(7,25,true));assertEquals(6,ProfileService.displayProgress(6,25,false));}
}
