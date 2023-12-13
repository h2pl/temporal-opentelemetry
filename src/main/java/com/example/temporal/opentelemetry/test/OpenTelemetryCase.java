package com.example.temporal.opentelemetry.test;

import com.alibaba.fastjson.JSON;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class OpenTelemetryCase {

    private static void myWonderfulUseCase(Tracer tracer) {
        // Generate a span
        Span span = tracer.spanBuilder("Start my wonderful use case").startSpan();
        span.addEvent("Event 0");

        // execute my use case - here we simulate a wait
        childSpan(tracer);

        doWork();
        span.addEvent("Event 1");
        span.end();
    }

    private static void childSpan(Tracer tracer){
        Span childSpan = tracer.spanBuilder("child")
                // NOTE: setParent(...) is not required;
                // `Span.current()` is automatically added as the parent
                .startSpan();

        try(Scope scope = childSpan.makeCurrent()) {
            int rd = ThreadLocalRandom.current().nextInt(1, 100);
            childSpan.addEvent("res="  + rd);
            childSpan.setAttribute("key","value");
            if (rd < 50){
                childSpan.setStatus(StatusCode.ERROR);
            }else {
                childSpan.setStatus(StatusCode.OK);
            }

            System.out.println(rd);
        } finally {
            childSpan.end();
        }
    }

    private static void doWork() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // do the right thing here
        }
    }

    public static void main(String[] args) {
        demo();
    }

    public static void useCase() {
        System.out.println("initOpenTelemetry");
        String name = System.getenv("OTEL_SERVICE_NAME");
        System.out.println(name);

        OpenTelemetry openTelemetry = TracingConfig.initOpenTelemetryManually();
        log.info("openTelemetry" + JSON.toJSONString(openTelemetry));

        System.out.println("getTracer");
        Tracer tracer = openTelemetry.getTracer("io.opentelemetry.example.JaegerExample");

        log.info("tracer" + JSON.toJSONString(tracer));

        // it is important to initialize your SDK as early as possible in your application's lifecycle
        System.out.println("begin");
        // generate a few sample spans
        myWonderfulUseCase(tracer);

        System.out.println("end");

    }

    public static void demo() {
        TracingConfig.initOpenTelemetryManually();

        Tracer tracer = GlobalOpenTelemetry
                .getTracerProvider()
                .tracerBuilder("my-tracer") //TODO Replace with the name of your tracer
                .build();

        // Obtain and name new span from tracer
        Span span = tracer.spanBuilder("Call to /myendpoint").startSpan();

// Set demo span attributes using semantic naming
        span.setAttribute("http.method", "GET");
        span.setAttribute("net.protocol.version", "1.1");

// Set the span as current span and parent for future child spans
        try (Scope scope = span.makeCurrent())
        {
            int rd = ThreadLocalRandom.current().nextInt(1, 100);
            span.addEvent("res="  + rd);
            span.setAttribute("key","value");
            if (rd < 50){
                span.setStatus(StatusCode.ERROR);
            }else {
                span.setStatus(StatusCode.OK);
            }

            System.out.println(rd);
            // TODO your code goes here
        }
        finally
        {
            // Completing the span
            span.end();
        }
    }

}
