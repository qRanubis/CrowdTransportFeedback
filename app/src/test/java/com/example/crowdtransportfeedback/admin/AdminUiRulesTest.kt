package com.example.crowdtransportfeedback.admin
import com.example.crowdtransportfeedback.auth.UserRole; import org.junit.Assert.*; import org.junit.Test
class AdminUiRulesTest {
 @Test fun `admin entry is role gated`(){assertTrue(canAccessAdmin(UserRole.ADMIN));assertFalse(canAccessAdmin(UserRole.USER))}
 @Test fun `own feedback cannot be reported`(){assertFalse(canReportFeedback("me","me",true));assertTrue(canReportFeedback("me","other",true));assertFalse(canReportFeedback("me","other",false))}
 @Test fun `other requires details and details are bounded`(){assertNotNull(reportValidationError("OTHER"," "));assertNotNull(reportValidationError("SPAM","x".repeat(251)));assertNull(reportValidationError("OTHER","context"))}
 @Test fun `report statuses have safe user labels`(){assertEquals("Reported · Pending review",reportStatusLabel("PENDING"));assertEquals("Reported · Reviewed",reportStatusLabel("DISMISSED"));assertEquals("Reported · Closed",reportStatusLabel("CLOSED"));assertEquals("Reported",reportStatusLabel(null))}
 @Test fun `pagination respects boundaries`(){assertFalse(canGoPrevious(0));assertTrue(canGoPrevious(1));assertTrue(canGoNext(0,2));assertFalse(canGoNext(1,2));assertFalse(canGoNext(0,0))}
 @Test fun `filter changes reset page and transport clears line`(){val state=AdminFilterState(transportType="BUS",line="100",username="old",page=3);assertEquals(0,state.withWindow("D7").page);assertEquals(0,state.withUsername("new").page);assertNull(state.withTransport("METRO").line);assertEquals(0,state.withLine("101").page)}
}
