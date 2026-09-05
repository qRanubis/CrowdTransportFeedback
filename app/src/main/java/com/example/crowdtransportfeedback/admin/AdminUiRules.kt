package com.example.crowdtransportfeedback.admin
import com.example.crowdtransportfeedback.auth.UserRole
import com.example.crowdtransportfeedback.domain.BucharestTransitCatalog
import com.example.crowdtransportfeedback.domain.TransportType
fun canAccessAdmin(role:UserRole)=role==UserRole.ADMIN
fun canReportFeedback(role:UserRole,currentUserId:String,ownerUserId:String?,synchronized:Boolean)=role==UserRole.USER&&synchronized&&ownerUserId!=currentUserId
fun shouldShowAdminEntry(role:UserRole,currentRoute:String?)=role==UserRole.ADMIN&&currentRoute!="admin"
fun reportValidationError(reason:String,details:String):String?=when{details.length>250->"Details must be at most 250 characters";reason=="OTHER"&&details.isBlank()->"Details are required for OTHER";else->null}
fun reportStatusLabel(status:String?):String=when(status){"PENDING"->"Reported · Pending review";"DISMISSED"->"Reported · Reviewed";"CONFIRMED"->"Reported · Confirmed";"CLOSED"->"Reported · Closed";else->"Reported"}
fun canGoPrevious(page:Int)=page>0
fun canGoNext(page:Int,totalPages:Int)=page+1<totalPages
data class AdminFilterState(val window:String="ALL",val transportType:String?=null,val line:String?=null,val username:String="",val page:Int=0){
 fun withWindow(value:String)=copy(window=value,page=0)
 fun withTransport(value:String?)=copy(transportType=value,line=null,page=0)
 fun withLine(value:String?)=copy(line=value,page=0)
 fun withUsername(value:String)=copy(username=value,page=0)
}
fun adminLineOptions(type:TransportType?):List<String?> = type?.let { listOf<String?>(null) + BucharestTransitCatalog.linesFor(it) } ?: emptyList()
