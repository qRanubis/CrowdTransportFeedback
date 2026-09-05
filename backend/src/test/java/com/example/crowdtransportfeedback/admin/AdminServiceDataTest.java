package com.example.crowdtransportfeedback.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.example.crowdtransportfeedback.feedback.FeedbackService;
import com.example.crowdtransportfeedback.moderation.ReportLifecycle;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;

@JdbcTest(properties = "spring.flyway.enabled=false")
class AdminServiceDataTest {
    @Autowired JdbcTemplate jdbc;
    AdminService service;
    final UUID alice = UUID.fromString("11111111-1111-1111-1111-111111111111");
    final UUID bob = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setup() {
        service = new AdminService(jdbc, mock(FeedbackService.class), mock(ReportLifecycle.class));
        jdbc.execute("create table app_user(id uuid primary key, username varchar(20), role varchar(16), created_at timestamp)");
        jdbc.execute("create table feedback(feedback_id uuid primary key, created_by_user_id uuid, transport_type varchar(24), line varchar(32), normalized_line varchar(32), score double, punctuality_score int, cleanliness_score int, crowding_score int, latitude double, longitude double, comment varchar(2000), created_at bigint)");
        jdbc.execute("create table feedback_report(id uuid primary key, feedback_id uuid, reporter_user_id uuid, reason varchar(40), details varchar(250), status varchar(16), created_at timestamp)");
        jdbc.execute("create table gamification_event(id uuid primary key, user_id uuid, xp_delta int)");
        insertUser(alice, "alice"); insertUser(bob, "bob");
        insertFeedback(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"), alice, "BUS", "100", 4, 4, 5, 3, "bus comment");
        insertFeedback(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"), bob, "METRO", "M2", 2, 2, 2, 2, "metro comment");
        jdbc.update("insert into feedback_report values(?,?,?,?,?,?,?)", UUID.randomUUID(), UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"), alice, "SPAM", null, "CONFIRMED", Timestamp.from(Instant.now()));
        jdbc.update("insert into gamification_event values(?,?,?)", UUID.randomUUID(), alice, 225);
    }

    @Test
    void overviewFiltersPaginationUsersReportingAndCsvUseStoredData() {
        var overview = service.overview();
        assertThat(overview.totalUsers()).isEqualTo(2); assertThat(overview.totalFeedbacks()).isEqualTo(2);
        assertThat(overview.feedbackLast24h()).isEqualTo(2); assertThat(overview.feedbackByTransportType()).containsEntry("BUS", 1L).containsEntry("METRO", 1L);

        var feedback = service.feedbackList("BUS", "100", "ALL", "alice", 0, 1);
        assertThat(feedback.totalElements()).isEqualTo(1); assertThat(feedback.totalPages()).isEqualTo(1);
        assertThat(feedback.content()).singleElement().satisfies(row -> assertThat(row.username()).isEqualTo("alice"));

        var users = service.users("ali", 0, 1);
        assertThat(users.totalElements()).isEqualTo(1); assertThat(users.content()).singleElement().satisfies(user -> {
            assertThat(user.verifiedReportCount()).isEqualTo(1); assertThat(user.totalXp()).isEqualTo(225); assertThat(user.level()).isEqualTo(3);
        });

        var summary = service.summary("ALL", "BUS", "100");
        assertThat(summary.feedbackCount()).isEqualTo(1); assertThat(summary.uniqueContributors()).isEqualTo(1);
        assertThat(summary.averageOverall()).isEqualTo(4.0); assertThat(summary.averageCleanliness()).isEqualTo(5.0);

        String csv = new String(service.csv("ALL", "BUS", "100"), StandardCharsets.UTF_8);
        assertThat(csv).startsWith("feedback_id,created_at,username,transport_type,line,overall_score,punctuality,cleanliness,crowding,latitude,longitude,comment");
        assertThat(csv).contains("bus comment").doesNotContain("metro comment");
    }

    private void insertUser(UUID id, String username) {
        jdbc.update("insert into app_user values(?,?,?,?)", id, username, "USER", Timestamp.from(Instant.now()));
    }
    private void insertFeedback(UUID id, UUID owner, String type, String line, double overall, int punctuality, int cleanliness, int crowding, String comment) {
        jdbc.update("insert into feedback values(?,?,?,?,?,?,?,?,?,?,?,?,?)", id, owner, type, line, line, overall, punctuality, cleanliness, crowding, 44.4, 26.1, comment, System.currentTimeMillis());
    }
}
