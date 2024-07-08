package org.moqui.metrics

import org.moqui.context.ExecutionContextFactory
import org.moqui.entity.EntityValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PrometheusMetrics extends Metrics {

    protected final static Logger logger = LoggerFactory.getLogger(PrometheusMetrics.class)

    protected static PrometheusMetrics prometheusMetricsInstance = null

    protected ExecutionContextFactory ecf
    protected List<PrometheusHistogram> histogramList = null

    static PrometheusMetrics getInstance() {
        if (prometheusMetricsInstance == null)
            prometheusMetricsInstance = new PrometheusMetrics()
        return prometheusMetricsInstance
    }

    PrometheusMetrics() {
        histogramList = new LinkedList<PrometheusHistogram>()
    }

    void reload() {
        init(ecf)
        logger.info("Reloaded PrometheusMetrics:\n${toString()}")
    }

    void init(ExecutionContextFactory ecf) {
        this.ecf = ecf
        List<EntityValue> masList = ecf.entity.find("moqui.metrics.MetricsHitGroup").disableAuthz().list()
        masList.each { EntityValue mas ->
            logger.info("Found histogram ${mas.metricName}")
            PrometheusHistogram hd = histogramList.find { al -> al.metricName == mas.metricName }
            if (!hd) {
                hd = new PrometheusHistogram(mas.metricName as String, ecf)
                histogramList.add(hd)
            }
            hd.init()
        }
    }

    void registerArtifactHit(String artifactType, String artifactName, BigDecimal runningTimeMillis, Character slowHit, String wasError) {
        histogramList.each { PrometheusHistogram histogram ->
            histogram.registerArtifactHit(artifactType, artifactName, runningTimeMillis, slowHit, wasError)
        }
    }

    String nextValues() {
        StringBuilder sb = new StringBuilder()
        histogramList.each { PrometheusHistogram histogram ->
            String histogramValues = histogram.nextValues()
            if (histogramValues)
                sb.append(histogramValues)
        }
        return sb
    }

    String toString() {
        StringBuilder sb = new StringBuilder("PrometheusMetric:\n${histogramList ? '' : '<No histograms registered>\n'}")
        histogramList.each { sb.append(it.toString("  ")) }
        return sb.toString()
    }

}