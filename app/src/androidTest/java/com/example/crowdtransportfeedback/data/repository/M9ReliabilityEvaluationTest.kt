package com.example.crowdtransportfeedback.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.util.Log
import com.example.crowdtransportfeedback.data.local.*
import com.example.crowdtransportfeedback.data.remote.*
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response

/** Controlled repository fault injection; this is not a cellular-network field study. */
@RunWith(AndroidJUnit4::class)
class M9ReliabilityEvaluationTest {
 @Test fun evaluateRecoveryAndIdempotency()=runBlocking {
  val db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),AppDatabase::class.java).allowMainThreadQueries().build()
  try {
   evaluate("R1_offline_create_then_reconnect") { n -> val api=FaultApi();val repo=repo(db,api);api.online=false;val id=repo.addFeedbackAndUpload(item("r1-$n"));assertEquals(SyncState.PENDING_CREATE,db.feedbackDao().getByLocalIdOnce(id)?.syncState);api.online=true;repo.synchronize();repo.synchronize();assertEquals(SyncState.SYNCED,db.feedbackDao().getByLocalIdOnce(id)?.syncState);assertEquals(1,api.remote.count{it.id=="r1-$n"});api.duplicates }
   evaluate("R2_transient_delete_then_reconnect") { n -> val api=FaultApi();val repo=repo(db,api);val dto=dto("r2-$n");api.remote+=dto;val id=db.feedbackDao().insert(dto.toEntity());api.online=false;repo.deleteFeedback(id);repo.synchronize();assertEquals(SyncState.PENDING_DELETE,db.feedbackDao().getByLocalIdOnce(id)?.syncState);api.online=true;repo.synchronize();repo.synchronize();assertNull(db.feedbackDao().getByLocalIdOnce(id));assertTrue(api.remote.none{it.id==dto.id});api.duplicates }
   evaluate("R3_repeated_synchronization_idempotency") { n -> val api=FaultApi();val repo=repo(db,api);api.online=false;repo.addFeedbackAndUpload(item("r3-$n"));api.online=true;repeat(5){repo.synchronize()};assertEquals(1,api.remote.count{it.id=="r3-$n"});assertEquals(1,repo.getAllFeedback().first().count{it.feedbackId=="r3-$n"});api.duplicates }
  } finally {db.close()}
 }
 private suspend fun evaluate(name:String,scenario:suspend(Int)->Int){var successes=0;var duplicates=0;repeat(30){n->try{duplicates+=scenario(n);assertEquals(0,duplicates);successes++}finally{/* each UUID is distinct and reconciliation removes prior rows */}};val result=if(successes==30&&duplicates==0)"PASS" else "FAIL";val message="M9_RESULT,$name,30,$successes,$duplicates,$result";println(message);Log.i("M9_EVAL",message);assertEquals(30,successes);assertEquals(0,duplicates)}
 private fun repo(db:AppDatabase,api:FaultApi)=FeedbackRepository(db.feedbackDao(),api,currentUserId={USER})
 private fun item(id:String)=FeedbackEntity(feedbackId=id,score=4,comment="M9",latitude=44.4268,longitude=26.1025,line=id,createdAt=1_700_000_000_000,createdByUserId=USER)
 private fun dto(id:String)=FeedbackDto(id,4.0,"M9",id,1_700_000_000_000,44.4268,26.1025,createdByUserId=USER)
 companion object {const val USER="11111111-1111-1111-1111-111111111111"}
}
private class FaultApi:FeedbackApi {var online=true;var remote:List<FeedbackDto> = emptyList();var duplicates=0
 private fun check(){if(!online)throw IOException("controlled offline state")}
 override suspend fun getAll():List<FeedbackDto>{check();return remote}
 override suspend fun getById(id:String):Response<FeedbackDto>{check();return remote.firstOrNull{it.id==id}?.let{Response.success(it)}?:Response.error(404,"missing".toResponseBody())}
 override suspend fun add(item:FeedbackDto):FeedbackDto{check();if(remote.any{it.id==item.id})duplicates++ else remote+=item;return item}
 override suspend fun delete(id:String):Response<Unit>{check();if(remote.none{it.id==id})return Response.error(404,"missing".toResponseBody());remote=remote.filterNot{it.id==id};return Response.success(Unit)}
 override suspend fun myReport(id:String)=FeedbackApi.MyReport(false,null)
 override suspend fun report(id:String,request:FeedbackApi.ReportRequest)=Response.success(Unit)
}
