package com.example.crowdtransportfeedback.moderation;
import com.example.crowdtransportfeedback.common.ApiException; import com.example.crowdtransportfeedback.feedback.FeedbackRepository; import com.example.crowdtransportfeedback.user.UserRepository;
import java.util.UUID; import org.springframework.http.HttpStatus; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service public class ReportService {
 private final FeedbackReportRepository reports; private final FeedbackRepository feedback; private final UserRepository users;
 public ReportService(FeedbackReportRepository r,FeedbackRepository f,UserRepository u){reports=r;feedback=f;users=u;}
 @Transactional public ReportDtos.Created create(UUID feedbackId,UUID reporterId,ReportDtos.CreateRequest request){
  var item=feedback.findById(feedbackId).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"feedback_not_found","Feedback was not found"));
  if(item.owner.getId().equals(reporterId))throw new ApiException(HttpStatus.FORBIDDEN,"own_feedback_report","You cannot report your own feedback");
  if(request.reason()==null)throw new ApiException(HttpStatus.BAD_REQUEST,"invalid_report_reason","A report reason is required");
  String details=request.details()==null?null:request.details().trim(); if(details!=null&&details.isEmpty())details=null;
  if(request.reason()==ReportReason.OTHER&&details==null)throw new ApiException(HttpStatus.BAD_REQUEST,"report_details_required","Details are required for OTHER");
  if(reports.findByFeedbackIdAndReporterId(feedbackId,reporterId).isPresent())throw new ApiException(HttpStatus.CONFLICT,"duplicate_report","You have already reported this feedback");
  var saved=reports.save(new FeedbackReport(feedbackId,users.getReferenceById(reporterId),request.reason(),details));
  return new ReportDtos.Created(saved.id,saved.status,saved.reason,saved.createdAt);
 }
 @Transactional(readOnly=true) public ReportDtos.Mine mine(UUID feedbackId,UUID userId){
  if(!feedback.existsById(feedbackId))throw new ApiException(HttpStatus.NOT_FOUND,"feedback_not_found","Feedback was not found");
  return reports.findByFeedbackIdAndReporterId(feedbackId,userId).map(r->new ReportDtos.Mine(true,r.status)).orElse(new ReportDtos.Mine(false,null));
 }
}
