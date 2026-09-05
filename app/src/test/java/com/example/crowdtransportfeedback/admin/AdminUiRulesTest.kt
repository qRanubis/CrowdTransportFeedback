package com.example.crowdtransportfeedback.admin
import com.example.crowdtransportfeedback.auth.UserRole; import org.junit.Assert.*; import org.junit.Test
class AdminUiRulesTest {
 @Test fun `admin entry is role gated`(){assertTrue(canAccessAdmin(UserRole.ADMIN));assertFalse(canAccessAdmin(UserRole.USER))}
 @Test fun `own feedback cannot be reported`(){assertFalse(canReportFeedback("me","me",true));assertTrue(canReportFeedback("me","other",true));assertFalse(canReportFeedback("me","other",false))}
 @Test fun `other requires details and details are bounded`(){assertNotNull(reportValidationError("OTHER"," "));assertNotNull(reportValidationError("SPAM","x".repeat(251)));assertNull(reportValidationError("OTHER","context"))}
}
