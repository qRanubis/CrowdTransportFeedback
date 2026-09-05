package com.example.crowdtransportfeedback.analytics
import com.example.crowdtransportfeedback.data.local.*; import com.example.crowdtransportfeedback.domain.TransportType; import com.example.crowdtransportfeedback.ui.map.*; import org.junit.Assert.*; import org.junit.Test
class AnalyticsLogicTest {
 @Test fun transportChangeResetsLine(){assertNull(AnalyticsFilter(transportType="METRO",line="M2").withTransport("BUS").line)}
 @Test fun normalizationUsesFixedScale(){assertEquals(0.0,normalizedHeatWeight(1.0),0.0);assertEquals(.5,normalizedHeatWeight(3.0),0.0);assertEquals(1.0,normalizedHeatWeight(5.0),0.0);assertTrue(AnalyticsMetric.CROWDING.legend().contains("Comfortable"))}
 @Test fun groupingExcludesUnsynchronizedAndOrdersLatest(){fun f(id:Long,state:SyncState,time:Long)=FeedbackEntity(id,"id$id",3,"",44.4268,26.1025,"100",time,state,TransportType.BUS,3,3,3,createdByUsername="u$id");val groups=feedbackGroups(visibleMapMarkers(listOf(f(1,SyncState.SYNCED,1),f(2,SyncState.PENDING_CREATE,3),f(3,SyncState.SYNCED,2)),MapFilter.ALL));assertEquals(1,groups.size);assertEquals(listOf(3L,1L),groups[0].feedback.map{it.localId})}
 @Test fun incrementalPageUsesTwentyNewest(){val page=newestPage((1..50).toList(),{it.toLong()},2);assertEquals(40,page.size);assertEquals(50,page.first())}
}
