/*
 *  Copyright (c) 2020 Temporal Technologies, Inc. All Rights Reserved
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.example.temporal.opentelemetry.tracing;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.extension.trace.propagation.JaegerPropagator;
import io.opentelemetry.opentracingshim.OpenTracingShim;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.semconv.ResourceAttributes;
import io.opentracing.Tracer;
import io.temporal.opentracing.OpenTracingOptions;
import io.temporal.opentracing.OpenTracingSpanContextCodec;
import java.util.concurrent.TimeUnit;

public class JaegerUtils {

  public static OpenTracingOptions getJaegerOptions(String type) {
    // default to Open Telemetry
    return getJaegerOpenTelemetryOptions();
  }

  private static OpenTracingOptions getJaegerOpenTelemetryOptions() {
    Resource serviceNameResource =
        Resource.create(
            Attributes.of(ResourceAttributes.SERVICE_NAME, "temporal-sample-opentelemetry"));

    //one agent
    OtlpHttpSpanExporter otlpHttpSpanExporter = OtlpHttpSpanExporter
            .builder()
            .setEndpoint("http://localhost:14499/otlp/v1/traces")
            .setTimeout(30, TimeUnit.SECONDS).build();

    //oltp
//    OtlpHttpSpanExporter otlpHttpSpanExporter = OtlpHttpSpanExporter
//            .builder()
//            .setEndpoint("https://jne77204.live.dynatrace.com/api/v2/otlp/v1/traces")
//            .addHeader("Authorization", "Api-Token dt0c01.4GXBDHPXHUGYJ4VMUKLRPR7W")
//            .setTimeout(30, TimeUnit.SECONDS).build();

    SdkTracerProvider tracerProvider =
        SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(otlpHttpSpanExporter))
            .setResource(Resource.getDefault().merge(serviceNameResource))
            .build();

    OpenTelemetrySdk openTelemetry =
        OpenTelemetrySdk.builder()
            .setPropagators(
                ContextPropagators.create(
                    TextMapPropagator.composite(
                        W3CTraceContextPropagator.getInstance(), JaegerPropagator.getInstance())))
            .setTracerProvider(tracerProvider)
            .build();

    // create OpenTracing shim and return OpenTracing Tracer from it
    return getOpenTracingOptionsForTracer(OpenTracingShim.createTracerShim(openTelemetry));
  }



  private static OpenTracingOptions getOpenTracingOptionsForTracer(Tracer tracer) {
    OpenTracingOptions options =
        OpenTracingOptions.newBuilder()
            .setSpanContextCodec(OpenTracingSpanContextCodec.TEXT_MAP_CODEC)
            .setTracer(tracer)
            .build();
    return options;
  }
}
