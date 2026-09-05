package com.example.crowdtransportfeedback.admin
import com.example.crowdtransportfeedback.auth.UserRole
fun canAccessAdmin(role:UserRole)=role==UserRole.ADMIN
fun canReportFeedback(currentUserId:String,ownerUserId:String?,synchronized:Boolean)=synchronized&&ownerUserId!=currentUserId
fun reportValidationError(reason:String,details:String):String?=when{details.length>250->"Details must be at most 250 characters";reason=="OTHER"&&details.isBlank()->"Details are required for OTHER";else->null}
