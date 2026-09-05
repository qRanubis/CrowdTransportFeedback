package com.example.crowdtransportfeedback.security;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.crowdtransportfeedback.common.ApiExceptionHandler;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SecurityConfigHealthTest.ProbeController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ApiExceptionHandler.class})
class SecurityConfigHealthTest {
    @Autowired MockMvc mvc;
    @MockBean JwtService jwt;

    @BeforeEach void setup() {
        when(jwt.parse("user-token")).thenReturn(new JwtService.AuthenticatedUser(UUID.randomUUID(), "USER"));
        when(jwt.parse("admin-token")).thenReturn(new JwtService.AuthenticatedUser(UUID.randomUUID(), "ADMIN"));
    }

    @Test void healthAndProbeSubpathsArePublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
    }

    @Test void unrelatedActuatorAndApplicationEndpointsRemainProtected() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/protected")).andExpect(status().isUnauthorized());
    }

    @Test void adminNamespaceRemainsAdminOnly() throws Exception {
        mvc.perform(get("/api/admin/probe").header("Authorization", "Bearer user-token")).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/probe").header("Authorization", "Bearer admin-token")).andExpect(status().isOk());
    }

    @RestController
    static class ProbeController {
        @GetMapping({"/actuator/health", "/actuator/health/readiness", "/actuator/health/liveness", "/actuator/env", "/api/protected", "/api/admin/probe"})
        String probe() { return "ok"; }
    }
}
