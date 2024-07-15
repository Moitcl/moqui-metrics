package org.moqui.metrics

import org.moqui.context.ExecutionContextFactory
import org.moqui.context.ToolFactory

class MetricsFactory implements ToolFactory<Metrics> {

    private PrometheusMetrics prometheusMetrics
    final static String TOOL_NAME = "Metrics"

    @Override
    void init(ExecutionContextFactory ecf) {
        if (prometheusMetrics == null)
            prometheusMetrics = new PrometheusMetrics()
        prometheusMetrics.init(ecf)
    }

    @Override
    PrometheusMetrics getInstance(Object... parameters) {
        return prometheusMetrics
    }

    @Override
    String getName() {
        return TOOL_NAME
    }

    @Override
    void preFacadeInit(ExecutionContextFactory ecf) { }

    @Override
    void destroy() { }

    @Override
    void postFacadeDestroy() { }
}