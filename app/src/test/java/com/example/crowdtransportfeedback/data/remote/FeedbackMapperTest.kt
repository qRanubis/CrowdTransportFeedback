package com.example.crowdtransportfeedback.data.remote
import com.example.crowdtransportfeedback.data.local.FeedbackEntity
import com.example.crowdtransportfeedback.domain.TransportType
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test
class FeedbackMapperTest {
 @Test fun structuredFieldsRoundTrip() { val source=FeedbackEntity(feedbackId="uuid",score=4,comment="ok",latitude=1.0,longitude=2.0,line="41",createdAt=3,transportType=TransportType.TRAM,crowdingScore=2,cleanlinessScore=3,punctualityScore=5); val result=source.toDto().toEntity(); assertEquals(source.transportType,result.transportType); assertEquals(source.crowdingScore,result.crowdingScore); assertEquals(source.cleanlinessScore,result.cleanlinessScore); assertEquals(source.punctualityScore,result.punctualityScore) }
 @Test fun legacyJsonDefaultsStructuredPropertiesToNull() { val dto=Gson().fromJson("{\"id\":\"old\",\"score\":3,\"comment\":\"legacy\",\"createdAt\":1}",FeedbackDto::class.java); assertNull(dto.transportType); assertNull(dto.crowdingScore); assertNull(dto.toEntity().cleanlinessScore) }
}
