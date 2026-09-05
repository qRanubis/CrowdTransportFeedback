package com.example.crowdtransportfeedback.analytics;
public enum ConfidenceLevel { NONE, LOW, MEDIUM, HIGH; public static ConfidenceLevel of(int n){return n==0?NONE:n<=2?LOW:n<=5?MEDIUM:HIGH;} }
