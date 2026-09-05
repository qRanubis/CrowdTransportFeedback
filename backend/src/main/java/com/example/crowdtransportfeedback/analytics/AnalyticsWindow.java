package com.example.crowdtransportfeedback.analytics;
import java.time.Duration;
public enum AnalyticsWindow { H24(Duration.ofHours(24)), D7(Duration.ofDays(7)), D30(Duration.ofDays(30)), ALL(null); final Duration duration; AnalyticsWindow(Duration d){duration=d;} public static AnalyticsWindow parse(String value){return switch(value.toUpperCase()){case "24H","H24"->H24;case "7D","D7"->D7;case "30D","D30"->D30;case "ALL"->ALL;default->throw new IllegalArgumentException("Unsupported analytics window: "+value);};} }
