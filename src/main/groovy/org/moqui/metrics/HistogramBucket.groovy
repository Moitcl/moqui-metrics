package org.moqui.metrics

class HistogramBucket {
    protected BigDecimal upperBound = null
    protected Map<String,String> additionalLabels = null

    protected Long counter

    HistogramBucket(BigDecimal upperBound) {
        this.upperBound = upperBound
        counter = 0 as Long
    }

    Map<String,String> getAdditionalLabels() {
        return (Map<String,String>)additionalLabels
    }

    void setAdditionalLabels(Map<String,String> additionalLabels) {
        this.additionalLabels = additionalLabels
    }

    void count(BigDecimal value) {
        if (value <= upperBound)
            counter++
    }

    String getCountString() {
        return (counter as String)
    }

    String getUpperBoundString() {
        return (upperBound as String)
    }

    String toString() {
        return toString(null)
    }
    String toString(String prefix) {
        return "${prefix ?: ''}- Upper bound: ${upperBound}\n${prefix ?: ''}  additionalLabels: ${additionalLabels.toString()}\n"
    }
}
