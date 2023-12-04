package com.example.temporal.opentelemetry.test;

import com.alibaba.fastjson.JSON;
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
        System.out.println("initOpenTelemetry");
        OpenTelemetry openTelemetry = TracingConfig.initDynatraceUseOneAgent();
        log.info("openTelemetry" + JSON.toJSONString(openTelemetry));

//        OpenTelemetry openTelemetry = TracingConfig.initOpenTelemetry("http://localhost:4317");
        System.out.println("getTracer");
        Tracer tracer = openTelemetry.getTracer("io.opentelemetry.example.JaegerExample");

        log.info("tracer" + JSON.toJSONString(tracer));

        // it is important to initialize your SDK as early as possible in your application's lifecycle
        System.out.println("begin");
        // generate a few sample spans
        myWonderfulUseCase(tracer);

        System.out.println("end");

    }

//    public static void testDynatraceAPI(){
//        System.out.println("initOpenTelemetry");
//        OpenTelemetry openTelemetry = Api.initOpenTelemetryProperties();
////        OpenTelemetry openTelemetry = TracingConfig.initOpenTelemetry("http://localhost:4317");
//        System.out.println("getTracer");
//        Tracer tracer = openTelemetry.getTracer("io.opentelemetry.example.JaegerExample");
//
//        // it is important to initialize your SDK as early as possible in your application's lifecycle
//        System.out.println("begin");
//        // generate a few sample spans
//        myWonderfulUseCase(tracer);
//
//        System.out.println("end");
//    }
}
