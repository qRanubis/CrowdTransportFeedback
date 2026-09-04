package com.example.crowdtransportfeedback.leaderboard;

import com.example.crowdtransportfeedback.gamification.LevelCatalog;
import java.util.*;

final class LeaderboardRanking {
    record Candidate(UUID id,String username,String avatarKey,long metricValue,long totalXp) {}
    private LeaderboardRanking() {}

    static LeaderboardController.Response rank(List<Candidate> candidates, LeaderboardController.Metric metric,
                                                LeaderboardController.Period period, int limit, UUID currentUserId) {
        Comparator<Candidate> comparator=Comparator.comparingLong(Candidate::metricValue).reversed();
        if(metric!=LeaderboardController.Metric.XP) comparator=comparator.thenComparing(Comparator.comparingLong(Candidate::totalXp).reversed());
        comparator=comparator.thenComparing(Candidate::username);
        List<Candidate> sorted=candidates.stream().sorted(comparator).toList();
        List<LeaderboardController.Entry> all=new ArrayList<>();
        for(int i=0;i<sorted.size();i++){
            Candidate c=sorted.get(i);Long rank=period==LeaderboardController.Period.THIS_MONTH&&c.metricValue()==0?null:(long)i+1;
            all.add(new LeaderboardController.Entry(rank,c.username(),c.avatarKey(),LevelCatalog.forXp(c.totalXp()).level(),c.metricValue(),c.totalXp(),c.id().equals(currentUserId)));
        }
        var mine=all.stream().filter(LeaderboardController.Entry::currentUser).findFirst().orElseThrow();
        return new LeaderboardController.Response(all.stream().filter(e->e.rank()!=null&&e.rank()<=limit).toList(),mine);
    }
}
