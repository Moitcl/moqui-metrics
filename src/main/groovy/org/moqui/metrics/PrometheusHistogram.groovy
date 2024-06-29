package org.moqui.metrics

import org.moqui.context.ExecutionContextFactory
import org.moqui.entity.EntityValue

import java.sql.Timestamp

class PrometheusHistogram {
    protected static List<Map<String,Object>> defaultBucketThresholds = [[upperBound:(0.005 as BigDecimal)], [upperBound:(0.010 as BigDecimal)], [upperBound:(0.025 as BigDecimal)],
                                                                         [upperBound:(0.050 as BigDecimal)], [upperBound:(0.100 as BigDecimal)], [upperBound:(0.250 as BigDecimal)],
                                                                         [upperBound:(0.500 as BigDecimal)], [upperBound:(1 as BigDecimal)], [upperBound:(3 as BigDecimal)],
                                                                         [upperBound:(5 as BigDecimal)], [upperBound:(10 as BigDecimal)]]

    protected ExecutionContextFactory ecf = null
    protected String counterName = null
    protected List<HistogramArtifact> artifactList = null
    protected Timestamp lastCalculation = null
    protected Timestamp fromDate = null
    protected Timestamp thruDate = null
    protected List<HistogramBucket> bucketList = null
    protected BigDecimal sum = null
    protected Long count = null
    protected Long errorCount = null
    protected Long slowHitCount = null

    protected Long bucketCounterInf = 0 as Long

    PrometheusHistogram(String counterName, ExecutionContextFactory ecf) throws IllegalArgumentException {
        this.ecf = ecf
        if (counterName == null || counterName.length() == 0)
            throw new IllegalArgumentException("Need to specify counterName")
        this.counterName = counterName
        this.artifactList = new LinkedList<HistogramArtifact>()
        lastCalculation = ecf.executionContext.user.nowTimestamp
        this.bucketList = new LinkedList<HistogramBucket>()
        sum = 0 as BigDecimal
        count = 0 as Long
        errorCount = 0 as Long
        slowHitCount = 0 as Long
    }

    void init() {
        List<Map> bucketEvList = ecf.entity.find("moqui.metrics.MetricsHistogramBucket").disableAuthz().condition("counterName", counterName).list()
        if (!bucketEvList)
            bucketEvList = defaultBucketThresholds
        List<BigDecimal> bucketBoundsToCheck = (List<BigDecimal>)bucketList.collect { it.upperBound }
        bucketEvList.each { Map bucketEv ->
            HistogramBucket bucket = bucketList.find { it.upperBound == bucketEv.upperBound }
            if (bucket)
                bucketBoundsToCheck.remove(bucket.upperBound)
            else {
                bucket = new HistogramBucket(bucketEv.upperBound as BigDecimal)
                bucketList.add(bucket)
            }
            if (bucketEv.additionalLabelsJson)
                bucket.setAdditionalLabels((Map<String, String>) new groovy.json.JsonSlurper().parseText(bucketEv.additionalLabelsJson as String))
        }
        bucketBoundsToCheck.each { BigDecimal upperBound ->
            HistogramBucket bucket = bucketList.find { it.upperBound == upperBound }
            bucketList.remove(bucket)
        }
        List<HistogramArtifact> artifactsToCheck = artifactList.clone()
        List<EntityValue> artifactEvList = ecf.entity.find("moqui.metrics.MetricsHistogramArtifact").disableAuthz().condition("counterName", counterName).list()
        artifactEvList.each { EntityValue ahMember ->
            HistogramArtifact artifact = artifactList.find { it.artifactName == ahMember.artifactName && it.artifactType == ahMember.artifactType }
            if (artifact)
                artifactsToCheck.remove(artifact)
            else
                artifactList.add(new HistogramArtifact(ahMember.artifactName as String, ahMember.artifactType as String))
        }
        artifactsToCheck.each { artifactList.remove(it) }
    }

    void registerArtifactHit(String artifactType, String artifactName, BigDecimal runningTimeMillis, String slowHit, String wasError) {
        boolean matches = false
        for (HistogramArtifact ha in artifactList) {
            if (ha.artifactType == artifactType && ha.artifactName == artifactName) {
                matches = true
                break
            }
        }
        if (!matches)
            return
        count++
        BigDecimal hitExecutionTime = runningTimeMillis / 1000
        sum += hitExecutionTime
        if (slowHit == 'Y')
            slowHitCount++
        if (wasError == 'Y')
            errorCount++
        bucketList.each { HistogramBucket bucket ->
            bucket.count(hitExecutionTime)
        }
        bucketCounterInf++
    }


    String getCounterName() {
        return counterName
    }

    void setCounterName(String counterName) {
        this.counterName = counterName
    }

    String getArtifactName() {
        return artifactName
    }

    void setArtifactName(String artifactName) {
        this.artifactName = artifactName
    }

    String getArtifactType() {
        return artifactType
    }

    void setArtifactType(String artifactType) {
        this.artifactType = artifactType
    }

    Timestamp getFromDate() {
        return fromDate
    }

    void setFromDate(Timestamp fromDate) {
        this.fromDate = fromDate
    }

    Timestamp getThruDate() {
        return thruDate
    }

    void setThruDate(Timestamp thruDate) {
        this.thruDate = thruDate
    }

    String nextValues() {
        Timestamp now = ecf.executionContext.user.nowTimestamp
        Long nowEpoch = now.time
        if ((fromDate != null && fromDate.after(now)) || (thruDate != null && thruDate.before(now)))
            return null

        // Build String
        StringBuilder sb = new StringBuilder("#HELP moqui_hit_${counterName}_slowhits_total Total number of slow hits seen\n" +
                "#TYPE moqui_hit_${counterName}_slowhits_total counter\n" +
                "moqui_hit_${counterName}_slowhits_total  ${slowHitCount} ${nowEpoch}\n" +
                "#HELP moqui_hit_${counterName}_errors_total Total number of slow hits seen\n" +
                "#TYPE moqui_hit_${counterName}_errors_total counter\n" +
                "moqui_hit_${counterName}_errors_total  ${errorCount} ${nowEpoch}\n" +
                "#HELP moqui_hit_${counterName}_histogram_seconds Histogram with durations of running times in buckets\n" +
                "#TYPE moqui_hit_${counterName}_histogram_seconds histogram\n" +
                "moqui_hit_${counterName}_histogram_seconds_sum  ${sum} ${nowEpoch}\n" +
                "moqui_hit_${counterName}_histogram_seconds_count ${count} ${nowEpoch}\n")
        bucketList.each { HistogramBucket bucket ->
            List additionalLabelsList = ["le=\"${bucket.getUpperBoundString()}\""]
            bucket.additionalLabels.each { fieldName, fieldValue ->
                additionalLabelsList.add(fieldName + "=\"" + (fieldValue ? (fieldValue.toString().replace('\\', '\\\\').replace('"', '\\"') + "\"") : "\""))
            }
            String additionalLabelString = additionalLabelsList.join(', ')
            sb.append("moqui_hit_${counterName}_histogram_seconds_bucket {${additionalLabelString}} ${bucket.getCountString()} ${nowEpoch}\n")
        }
        sb.append("moqui_hit_${counterName}_histogram_seconds_bucket {le=\"+Inf\"} ${count} ${nowEpoch}\n")

        return sb.toString()
    }

    String toString() {
        return toString(null)
    }

    String toString(String prefix) {
        StringBuilder sb = new StringBuilder("${prefix ?: ''}- Histogram '${counterName}':\n${prefix ?: ''}Buckets:\n${bucketList ? '' : '<no buckets defined>\n'}")
        prefix = (prefix ?: '') + '  '
        String subprefix = (prefix ?: '') + '  '
        bucketList.each { sb.append(it.toString(subprefix)) }
        sb.append("${prefix ?: ''}Artifacts:\n${artifactList ? '' : '<no artifacts registered>\n'}")
        artifactList.each { sb.append(it.toString(subprefix)) }
        return sb.toString()
    }

}
