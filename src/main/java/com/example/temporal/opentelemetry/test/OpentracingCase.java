package com.example.temporal.opentelemetry.test;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.opentracingshim.OpenTracingShim;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;

public class OpentracingCase {

    public static void main(String[] args) {

        OpenTelemetry openTelemetry = TracingConfig.initOpenTelemetry("http://localhost:4317");
        System.out.println("begin");

        try (Tracer tracer = OpenTracingShim.createTracerShim(openTelemetry)) {
            // Make sure to use the correct Tracer.
            Tracer.SpanBuilder spanBuilder = tracer.buildSpan("hello");
            spanBuilder.withTag("foo", "bar");
            Span span = spanBuilder.start();
            // Make sure to close every created Scope.
            // It is recommended to use a try-with-resource statement for that.
            try (Scope scope = tracer.activateSpan(span)) {
                // Do actual operation.
                span.log("hello");
            } finally {
                // Make sure to finish every started Span.
                span.finish();
            }
        }
        System.out.println("end");
    }





}
