package com.example.temporal.opentelemetry.test;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.instrumentation.log4j.appender.v2_17.OpenTelemetryAppender;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.AggregationTemporalitySelector;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.ResourceAttributes;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class TracingConfigTrial {
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


            for (String name : new String[]{"dt_metadata_e617c525669e072eebe3d0f08212e8f2.properties", "/var/lib/dynatrace/enrichment/dt_metadata.properties"}) {
                try {
                    Properties props = new Properties();
                    props.load(name.startsWith("/var") ? new FileInputStream(name) : new FileInputStream(Files.readAllLines(Paths.get(name)).get(0)));
                    dtMetadata = dtMetadata.merge(Resource.create(props.entrySet().stream()
                            .collect(Attributes::builder, (b, e) -> b.put(e.getKey().toString(), e.getValue().toString()), (b1, b2) -> b1.putAll(b2.build()))
                            .build())
                    );
                } catch (IOException e) {
                }
            }


            return resource.merge(dtMetadata);
        }).build().getOpenTelemetrySdk();
        OpenTelemetryAppender.install(sdk);

        return sdk;
    }

    private static final String DT_API_URL = "https://aki10000.live.dynatrace.com/api/v2/otlp"; // TODO: Provide your SaaS/Managed URL here
    private static final String DT_API_TOKEN = "dt0c01.BZCWAQJ6RQ6BYCNCIRD7WIKV.BCAYABR367UJDE3TJ6H7GB7QYGITGLVFQUCD4STGL6QJVDE4BVWOVYCMSMPDO4YI"; // TODO: Provide the OpenTelemetry-scoped access token here
    public static OpenTelemetry initOpenTelemetryManually(){
        // ===== GENERAL SETUP =====

        // Read service name from the environment variable OTEL_SERVICE_NAME, if present
        Resource serviceName = Optional.ofNullable(System.getenv("OTEL_SERVICE_NAME"))
                .map(n -> Attributes.of(AttributeKey.stringKey("service.name"), n))
                .map(Resource::create)
                .orElseGet(Resource::empty);

        // Parse the environment variable OTEL_RESOURCE_ATTRIBUTES into key-value pairs
        Resource envResourceAttributes = Resource.create(Stream.of(Optional.ofNullable(System.getenv("OTEL_RESOURCE_ATTRIBUTES")).orElse("").split(","))
                .filter(pair -> pair != null && pair.length() > 0 && pair.contains("="))
                .map(pair -> pair.split("="))
                .filter(pair -> pair.length == 2)
                .collect(Attributes::builder, (b, p) -> b.put(p[0], p[1]), (b1, b2) -> b1.putAll(b2.build()))
                .build()
        );

        // Read host information from OneAgent files to enrich telemetry
        Resource dtMetadata = Resource.empty();
        for (String name : new String[] {"dt_metadata_e617c525669e072eebe3d0f08212e8f2.properties", "/var/lib/dynatrace/enrichment/dt_metadata.properties", "/var/lib/dynatrace/enrichment/dt_host_metadata.properties"}) {
            try {
                Properties props = new Properties();
                props.load(name.startsWith("/var") ? new FileInputStream(name) : new FileInputStream(Files.readAllLines(Paths.get(name)).get(0)));


                dtMetadata = dtMetadata.merge(Resource.create(
                        props.entrySet().stream()
                                .collect(Attributes::builder, (b, e) -> b.put(e.getKey().toString(), e.getValue().toString()), (b1, b2) -> b1.putAll(b2.build()))
                                .build()
                ));
            } catch (IOException e) {}
        }

        // ===== TRACING SETUP =====

        // Configure span exporter with the Dynatrace URL and the API token
        SpanExporter exporter = OtlpHttpSpanExporter.builder()
                .setEndpoint(DT_API_URL + "/v1/traces")
                .addHeader("Authorization", "Api-Token " + DT_API_TOKEN)
                .build();


        // Set up tracer provider with a batch processor and the span exporter
        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                .setResource(Resource.getDefault().merge(envResourceAttributes).merge(serviceName).merge(dtMetadata))
                .setSampler(Sampler.alwaysOn())
                .addSpanProcessor(BatchSpanProcessor.builder(exporter).build())
                .build();


        // ===== METRIC SETUP =====


        // Configure metric exporter with the Dynatrace URL and the API token
        OtlpHttpMetricExporter metricExporter = OtlpHttpMetricExporter.builder()
                .setEndpoint(DT_API_URL + "/v1/metrics")
                .addHeader("Authorization", "Api-Token " + DT_API_TOKEN)
                .setAggregationTemporalitySelector(AggregationTemporalitySelector.deltaPreferred())
                .build();


        // Set up meter provider with a periodic reader and the metric exporter
        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                .setResource(Resource.getDefault().merge(envResourceAttributes).merge(serviceName).merge(dtMetadata))
                .registerMetricReader(PeriodicMetricReader.builder(metricExporter).build())
                .build();

        // ===== LOG SETUP =====

        // Configure log exporter with the Dynatrace URL and the API token
        OtlpHttpLogRecordExporter logExporter = OtlpHttpLogRecordExporter.builder()
                .setEndpoint(DT_API_URL + "/v1/logs")
                .addHeader("Authorization", "Api-Token " + DT_API_TOKEN)
                .build();


        // Set up log provider with the log exporter
        SdkLoggerProvider sdkLoggerProvider = SdkLoggerProvider.builder()
                .setResource(Resource.getDefault().merge(envResourceAttributes).merge(serviceName).merge(dtMetadata))
                .addLogRecordProcessor(BatchLogRecordProcessor.builder(logExporter).build())
                .build();

        // ===== INITIALIZATION =====

        // Initialize OpenTelemetry with the tracer and meter providers
        OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .setMeterProvider(meterProvider)
                .setLoggerProvider(sdkLoggerProvider)
                .buildAndRegisterGlobal();

        //
        Runtime.getRuntime().addShutdownHook(new Thread(sdkTracerProvider::close));
        OpenTelemetryAppender.install(sdk);

        return sdk;
    }
}
