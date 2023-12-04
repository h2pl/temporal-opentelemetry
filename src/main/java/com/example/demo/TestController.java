package com.example.demo;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@RestController
public class TestController {

    @GetMapping("/hello")
    public String hello() {
        return "hello";
    }

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    @GetMapping("/rolldice")
    public String index(@RequestParam("player") Optional<String> player) {
        int result = this.getRandomNumber(1, 6);
        Tracer tracer = GlobalOpenTelemetry.getTracer("instrumentation-scope-name", "instrumentation-scope-version");
        Span span = tracer.spanBuilder("rollTheDice").startSpan();
        try (Scope scope = span.makeCurrent()) {
            if (player.isPresent()) {
                logger.info("{} is rolling the dice: {}", player.get(), result);
            } else {
                logger.info("Anonymous player is rolling the dice: {}", result);
            }
            span.addEvent("hello");
        } catch (Throwable t) {
            span.recordException(t);
            throw t;
        } finally {
            span.end();
        }

        return Integer.toString(result);
    }

    @GetMapping("/trace")
    public String trace() {
        Tracer tracer = GlobalOpenTelemetry.getTracer("instrumentation-scope-name", "instrumentation-scope-version");
        Span span = tracer.spanBuilder("trace").startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.addEvent("hello");
        } catch (Throwable t) {
            span.recordException(t);
            throw t;
        } finally {
            span.end();
        }

        return "trace";
    }

    @GetMapping("/metric")
    public String metric() {
        Meter meter = GlobalOpenTelemetry.meterBuilder("instrumentation-library-name")
                .setInstrumentationVersion("1.0.0")
                .build();

        LongCounter counter = meter
                .counterBuilder("processed_jobs")
                .setDescription("Processed jobs")
                .setUnit("1")
                .build();

        Attributes attributes = Attributes.of(AttributeKey.stringKey("Key"), "SomeWork");
        counter.add(123, attributes);

        return "metric";
    }

    public int getRandomNumber(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

}
