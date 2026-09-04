package com.example.crowdtransportfeedback.feedback;

import com.example.crowdtransportfeedback.common.ApiException;
import com.example.crowdtransportfeedback.common.ApiExceptionHandler;
import com.example.crowdtransportfeedback.security.JwtAuthenticationFilter;
import com.example.crowdtransportfeedback.security.JwtService;
import com.example.crowdtransportfeedback.security.SecurityConfig;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeedbackController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ApiExceptionHandler.class})
class FeedbackControllerSecurityTest {
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ADMIN_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID FEEDBACK_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired MockMvc mvc;
    @MockBean FeedbackService service;
    @MockBean JwtService jwt;

    @BeforeEach
    void setUp() {
        when(jwt.parse("user-token")).thenReturn(new JwtService.AuthenticatedUser(USER_ID, "USER"));
        when(jwt.parse("admin-token")).thenReturn(new JwtService.AuthenticatedUser(ADMIN_ID, "ADMIN"));
        when(service.all()).thenReturn(List.of());
        when(service.create(any(), eq(USER_ID))).thenReturn(response(USER_ID));
    }

    @Test
    void unauthenticatedGetIsRejected() throws Exception {
        mvc.perform(get("/api/feedback")).andExpect(status().isUnauthorized());
    }

    @Test
    void userCanGetAndPostFeedback() throws Exception {
        mvc.perform(get("/api/feedback").header("Authorization", "Bearer user-token"))
            .andExpect(status().isOk());
        mvc.perform(post("/api/feedback")
                .header("Authorization", "Bearer user-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validJson()))
            .andExpect(status().isCreated());
    }

    @Test
    void userDeleteDelegatesOwnershipCheckToService() throws Exception {
        mvc.perform(delete("/api/feedback/{id}", FEEDBACK_ID)
                .header("Authorization", "Bearer user-token"))
            .andExpect(status().isNoContent());
        verify(service).delete(FEEDBACK_ID, USER_ID, "USER");
    }

    @Test
    void serviceCanRejectDeleteForUnrelatedUser() throws Exception {
        doThrow(new ApiException(
            HttpStatus.FORBIDDEN,
            "feedback_delete_forbidden",
            "Only the author or an administrator can delete this feedback"
        )).when(service).delete(FEEDBACK_ID, USER_ID, "USER");
        mvc.perform(delete("/api/feedback/{id}", FEEDBACK_ID)
                .header("Authorization", "Bearer user-token"))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminCanDeleteFeedback() throws Exception {
        mvc.perform(delete("/api/feedback/{id}", FEEDBACK_ID)
                .header("Authorization", "Bearer admin-token"))
            .andExpect(status().isNoContent());
        verify(service).delete(FEEDBACK_ID, ADMIN_ID, "ADMIN");
    }

    @Test
    void invalidStructuredFeedbackIsRejectedBeforePersistence() throws Exception {
        List<String> invalidRequests = List.of(
            validJson().replace("\"transportType\":\"BUS\",", ""),
            validJson().replace("\"BUS\"", "\"PLANE\""),
            validJson().replace("\"line\":\"41\"", "\"line\":\"   \""),
            validJson().replace("\"punctualityScore\":4,", ""),
            validJson().replace("\"cleanlinessScore\":4,", ""),
            validJson().replace("\"crowdingScore\":4,", ""),
            validJson().replace("\"punctualityScore\":4", "\"punctualityScore\":6"),
            validJson().replace("\"latitude\":44.4268", "\"latitude\":91.0"),
            validJson().replace("\"longitude\":26.1025", "\"longitude\":181.0")
        );
        for (String body : invalidRequests) {
            mvc.perform(post("/api/feedback")
                    .header("Authorization", "Bearer user-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest());
        }
        verify(service, never()).create(any(), eq(USER_ID));
    }

    private static FeedbackDtos.Response response(UUID ownerId) {
        return new FeedbackDtos.Response(
            FEEDBACK_ID, FEEDBACK_ID.toString(), ownerId, "user123", "COMMUTER",
            TransportType.BUS, "41", 4.0, 4.0, 4, 4, 4,
            "ok", 44.4268, 26.1025, 100L, 0, java.util.List.of()
        );
    }

    private static String validJson() {
        return """
            {
              "feedbackId":"33333333-3333-3333-3333-333333333333",
              "transportType":"BUS",
              "line":"41",
              "score":1.0,
              "punctualityScore":4,
              "cleanlinessScore":4,
              "crowdingScore":4,
              "comment":"ok",
              "latitude":44.4268,
              "longitude":26.1025,
              "createdAt":100
            }
            """;
    }
}
