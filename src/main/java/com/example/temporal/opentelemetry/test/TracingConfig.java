package com.example.temporal.opentelemetry.test;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.ResourceAttributes;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class TracingConfig {
    public static OpenTelemetry initOpenTelemetry(String jaegerEndpoint) {
        // Export traces to Jaeger over OTLP
        OtlpGrpcSpanExporter jaegerOtlpExporter = OtlpGrpcSpanExporter.builder().setEndpoint(jaegerEndpoint).setTimeout(30, TimeUnit.SECONDS).build();

        Resource serviceNameResource = Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "otel-jaeger-example"));

        // Set to process the spans by the Jaeger Exporter
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder().addSpanProcessor(BatchSpanProcessor.builder(jaegerOtlpExporter).build()).setResource(Resource.getDefault().merge(serviceNameResource)).build();
        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build();

        // it's always a good idea to shut down the SDK cleanly at JVM exit.
        Runtime.getRuntime().addShutdownHook(new Thread(tracerProvider::close));

        return openTelemetry;
    }

    public static OpenTelemetry initDynatraceUseOneAgent(){
        OtlpHttpSpanExporter otlpHttpSpanExporter = OtlpHttpSpanExporter
                .builder()
                .setEndpoint("http://localhost:14499/otlp/v1/traces")
                .setTimeout(30, TimeUnit.SECONDS).build();

        Resource serviceNameResource = Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "otel-dynatrace-example"));
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(otlpHttpSpanExporter).build())
                .setResource(Resource.getDefault().merge(serviceNameResource))
                .build();
        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build();

        // it's always a good idea to shut down the SDK cleanly at JVM exit.
        Runtime.getRuntime().addShutdownHook(new Thread(tracerProvider::close));
        return openTelemetry;
    }

    public static OpenTelemetry initDynatraceUseActiveGate(){
        OtlpHttpSpanExporter otlpHttpSpanExporter = OtlpHttpSpanExporter
                .builder()
                .setEndpoint("https://jne77204.live.dynatrace.com/api/v2/otlp/v1/traces")
                .addHeader("Authorization", "Api-Token dt0c01.4GXBDHPXHUGYJ4VMUKLRPR7W")
                .setTimeout(30, TimeUnit.SECONDS).build();

        Resource serviceNameResource = Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "otel-dynatrace-example"));
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(otlpHttpSpanExporter).build())
                .setResource(Resource.getDefault().merge(serviceNameResource))
                .build();
        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build();

        // it's always a good idea to shut down the SDK cleanly at JVM exit.
        Runtime.getRuntime().addShutdownHook(new Thread(tracerProvider::close));
        return openTelemetry;
    }

    public static OpenTelemetry initOpenTelemetryProperties() {
        OpenTelemetrySdk sdk = AutoConfiguredOpenTelemetrySdk.builder().addResourceCustomizer((resource, properties) -> {
            Resource dtMetadata = Resource.empty();

            for (String name : new String[]{"src/main/resources/environment.properties"}) {
                try {
                    Properties props = new Properties();
                    props.load(new FileInputStream(name)) ;
                    dtMetadata = dtMetadata.merge(Resource.create(props.entrySet().stream()
                            .collect(Attributes::builder, (b, e) -> b.put(e.getKey().toString(), e.getValue().toString()), (b1, b2) -> b1.putAll(b2.build()))
                            .build())
                    );
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            return resource.merge(dtMetadata);
        }).build().getOpenTelemetrySdk();

        return sdk;
    }
}
