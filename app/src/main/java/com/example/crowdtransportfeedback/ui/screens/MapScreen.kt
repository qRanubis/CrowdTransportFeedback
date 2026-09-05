package com.example.crowdtransportfeedback.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.crowdtransportfeedback.BuildConfig
import com.example.crowdtransportfeedback.analytics.*
import com.example.crowdtransportfeedback.domain.BucharestTransitCatalog
import com.example.crowdtransportfeedback.domain.TransportType
import com.example.crowdtransportfeedback.location.AndroidLocationProvider
import com.example.crowdtransportfeedback.location.Coordinates
import com.example.crowdtransportfeedback.ui.map.*
import com.example.crowdtransportfeedback.ui.viewmodel.FeedbackViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import java.text.DateFormat
import java.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

private val Bucharest = LatLng(44.4268, 26.1025)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(vm:FeedbackViewModel,analyticsRepository:AnalyticsRepository,onFeedbackClick:(Long)->Unit){
 if(!BuildConfig.MAPS_API_KEY_CONFIGURED){Box(Modifier.fillMaxSize().padding(24.dp),contentAlignment=Alignment.Center){Text("Google Maps is not configured. Add MAPS_API_KEY to local.properties and rebuild the app.")};return}
 val context=LocalContext.current;val feedback by vm.feedbackList.collectAsState();var mode by remember{mutableStateOf(MapMode.FEEDBACK)};var filter by remember{mutableStateOf(AnalyticsFilter())}
 val mapFilter=MapFilter.entries.firstOrNull{it.transportType?.name==filter.transportType}?:MapFilter.ALL
 val groups=remember(feedback,filter.transportType,filter.line){feedbackModeGroups(feedback,mapFilter.transportType,filter.line)};var cells by remember{mutableStateOf<List<AnalyticsCellDto>>(emptyList())};var loading by remember{mutableStateOf(false)};var error by remember{mutableStateOf<String?>(null)}
 var selectedGroup by remember{mutableStateOf<MapFeedbackGroup?>(null)};var area by remember{mutableStateOf<AreaDetailsDto?>(null)};var areaLocalGroup by remember{mutableStateOf<MapFeedbackGroup?>(null)};var showAll by remember{mutableStateOf(false)};var page by remember{mutableIntStateOf(1)}
 var pendingFeedbackId by remember { mutableStateOf<Long?>(null) }
 LaunchedEffect(pendingFeedbackId) { pendingFeedbackId?.let { pendingFeedbackId=null;onFeedbackClick(it) } }
 LaunchedEffect(mode,filter){if(mode==MapMode.HEATMAP){loading=true;error=null;cells=emptyList();try{cells=analyticsRepository.heatmap(filter);error=null}catch(cancelled:CancellationException){throw cancelled}catch(_:Exception){cells=emptyList();error="Heatmap unavailable. Feedback mode remains available."}finally{loading=false}}}
 val camera=rememberCameraPositionState{position=CameraPosition.fromLatLngZoom(Bucharest,12f)};val scope=rememberCoroutineScope();var permission by remember{mutableStateOf(ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED||ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED)};var location by remember{mutableStateOf<Coordinates?>(null)}
 fun locate(){if(permission)AndroidLocationProvider(context).getLocation{it.getOrNull()?.let{p->location=p;scope.launch{camera.animate(CameraUpdateFactory.newLatLngZoom(LatLng(p.latitude,p.longitude),15f))}}}}
 val launcher=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){permission=it.values.any{v->v};if(permission)locate()};LaunchedEffect(permission){if(permission&&location==null)locate()}
 Box(Modifier.fillMaxSize()){
  GoogleMap(Modifier.fillMaxSize(),cameraPositionState=camera,properties=MapProperties(isMyLocationEnabled=permission),uiSettings=MapUiSettings(myLocationButtonEnabled=false)){
   if(mode==MapMode.FEEDBACK)groups.forEach{g->key(g.cellId){val one=g.feedback.singleOrNull();Marker(state=rememberMarkerState(key=g.cellId,position=LatLng(if(one==null)g.latitude else one.latitude,if(one==null)g.longitude else one.longitude)),title=if(one==null)"${g.feedback.size} feedbacks · Area feedback" else "${one.transportType.displayName} ${one.line}",snippet=if(one==null)"Tap for latest feedbacks" else String.format(Locale.US,"Overall rating: %.1f/5 · @%s",one.overallRating,one.publicUsername),icon=BitmapDescriptorFactory.defaultMarker(if(one==null)BitmapDescriptorFactory.HUE_AZURE else markerHue(one.transportType)),onClick={if(one==null){selectedGroup=g;true}else false},onInfoWindowClick={one?.let{pendingFeedbackId=it.localId}})}}
   else cells.forEach{c->Circle(center=LatLng(c.centerLatitude,c.centerLongitude),radius=125.0,fillColor=semanticColor(c.score).copy(alpha=.55f),strokeColor=semanticColor(c.score),strokeWidth=2f,onClick={scope.launch{val requestedAt=System.currentTimeMillis();try{val details=analyticsRepository.area(c.cellId,filter);areaLocalGroup=analyticsAreaGroup(feedback,filter,c.cellId,requestedAt);area=details}catch(cancelled:CancellationException){throw cancelled}catch(_:Exception){area=null;areaLocalGroup=null;error="Area details unavailable."}}},clickable=true)}
  }
  Column(Modifier.fillMaxWidth().padding(8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){MapMode.entries.forEach{option->FilterChip(mode==option,{mode=option;if(option==MapMode.FEEDBACK)error=null},{Text(option.name.lowercase().replaceFirstChar(Char::uppercase))})}}
   if(mode==MapMode.HEATMAP){ChipRow(AnalyticsMetric.entries,filter.metric,{filter=filter.copy(metric=it)}){it.name.lowercase().replaceFirstChar(Char::uppercase)};ChipRow(AnalyticsWindow.entries,filter.window,{filter=filter.copy(window=it)}){it.label};Text(filter.metric.legend(),style=MaterialTheme.typography.labelMedium)}
   ChipRow(MapFilter.entries,mapFilter,{filter=filter.withTransport(it.transportType?.name)}){it.label}
   filter.transportType?.let{name->LineSelector(TransportType.valueOf(name),filter.line){filter=filter.copy(line=it)}}
   if(loading)LinearProgressIndicator(Modifier.fillMaxWidth());heatmapErrorForMode(mode,error)?.let{Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.labelMedium)}
   if(shouldShowHeatmapEmpty(mode,loading,error,cells.size))Surface(color=MaterialTheme.colorScheme.surface.copy(alpha=.94f),shape=MaterialTheme.shapes.medium,shadowElevation=4.dp){Text("No analytics match these filters",modifier=Modifier.padding(horizontal=12.dp,vertical=10.dp),style=MaterialTheme.typography.bodyMedium)}
  }
  Button({if(permission)locate()else launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))},Modifier.align(Alignment.BottomEnd).padding(16.dp)){Text(if(permission)"My location" else "Enable location")}
 }
 selectedGroup?.let{g->ModalBottomSheet(onDismissRequest={selectedGroup=null;showAll=false;page=1}){Text("Area feedback",style=MaterialTheme.typography.headlineSmall,modifier=Modifier.padding(horizontal=16.dp));Text("${g.feedback.size} feedbacks · ${g.feedback.map{it.publicUsername}.distinct().size} contributors",modifier=Modifier.padding(16.dp));val shown=if(showAll)newestPage(g.feedback,{it.createdAt},page)else g.feedback.take(5);LazyColumn(Modifier.heightIn(max=420.dp)){items(shown,key={it.localId}){Preview("${it.transportType.displayName} ${it.line}",it.overallRating,it.publicUsername,it.createdAt){onFeedbackClick(it.localId)}}};if(!showAll&&g.feedback.size>5)TextButton({showAll=true}){Text("View all ${g.feedback.size} feedbacks")};if(showAll&&shown.size<g.feedback.size)TextButton({page++}){Text("Load more")};Spacer(Modifier.height(24.dp))}}
 area?.let{a->ModalBottomSheet(onDismissRequest={area=null;areaLocalGroup=null}){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Text("Area details",style=MaterialTheme.typography.headlineSmall);Text("${filter.metric.name.lowercase().replaceFirstChar(Char::uppercase)}  ${"%.1f".format(a.score)} / 5");Text("Confidence ${a.confidence} · ${a.feedbackCount} feedbacks · ${a.uniqueContributorCount} contributors");Text("Last contribution: ${formatDateTime(a.latestCreatedAt)}");Text("Trust ${"%.1f".format(a.trustScore)} · Punctuality ${"%.1f".format(a.punctualityScore)}");Text("Cleanliness ${"%.1f".format(a.cleanlinessScore)} · Crowding comfort ${"%.1f".format(a.crowdingComfortScore)}");Text("Latest feedbacks",style=MaterialTheme.typography.titleMedium);a.latestFeedbacks.forEach{p->Preview("${p.transportType.lowercase().replaceFirstChar(Char::uppercase)} ${p.line}",p.overallRating,p.createdByUsername,p.createdAt){feedback.firstOrNull{it.feedbackId==p.feedbackId}?.let{onFeedbackClick(it.localId)}}};val local=areaLocalGroup;if(local!=null&&local.feedback.size==a.feedbackCount)TextButton({selectedGroup=local;showAll=true;area=null;areaLocalGroup=null}){Text("View all ${a.feedbackCount} feedbacks")}else Text("Full list unavailable locally (${local?.feedback?.size?:0} of ${a.feedbackCount} matching feedbacks synchronized).",style=MaterialTheme.typography.labelMedium);Spacer(Modifier.height(20.dp))}}}
}

@Composable private fun <T> ChipRow(values:Iterable<T>,selected:T,onSelect:(T)->Unit,label:(T)->String){Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){values.forEach{FilterChip(selected==it,{onSelect(it)},{Text(label(it))})}}}
@Composable private fun LineSelector(type:TransportType,selected:String?,onSelect:(String?)->Unit){var open by remember{mutableStateOf(false)};Box{AssistChip({open=true},{Text(selected?:"All lines")});DropdownMenu(open,{open=false}){DropdownMenuItem({Text("All lines")},{onSelect(null);open=false});BucharestTransitCatalog.linesFor(type).forEach{line->DropdownMenuItem({Text(line)},{onSelect(line);open=false})}}}}
@Composable private fun Preview(title:String,rating:Double,user:String,created:Long,onClick:()->Unit){Column(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(horizontal=16.dp,vertical=8.dp)){Text(title,style=MaterialTheme.typography.titleSmall);Text(String.format(Locale.US,"Overall rating %.1f/5 · @%s",rating,user));Text(DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(Date(created)),style=MaterialTheme.typography.labelSmall)}}
private fun formatDateTime(timestamp:Long):String=DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(Date(timestamp))
internal fun semanticColor(score:Double)=when{score<1.8->Color(0xffd32f2f);score<2.6->Color(0xfff57c00);score<3.4->Color(0xffffd600);score<4.2->Color(0xff8bc34a);else->Color(0xff2e7d32)}
internal fun markerHue(type:TransportType):Float=when(type){TransportType.BUS->BitmapDescriptorFactory.HUE_BLUE;TransportType.METRO->BitmapDescriptorFactory.HUE_RED;TransportType.TRAM->BitmapDescriptorFactory.HUE_ORANGE;TransportType.TROLLEYBUS->BitmapDescriptorFactory.HUE_GREEN;TransportType.NIGHT_BUS->BitmapDescriptorFactory.HUE_VIOLET}
