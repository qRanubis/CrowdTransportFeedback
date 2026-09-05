package com.example.crowdtransportfeedback.analytics
class AnalyticsRepository(private val api:AnalyticsApi){suspend fun heatmap(f:AnalyticsFilter)=api.heatmap(f.metric.name,f.transportType,f.line,f.window.query);suspend fun area(id:String,f:AnalyticsFilter)=api.area(id,f.metric.name,f.transportType,f.line,f.window.query)}
