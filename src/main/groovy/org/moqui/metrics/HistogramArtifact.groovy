package org.moqui.metrics

import org.moqui.context.ExecutionContextFactory
import org.moqui.entity.EntityCondition

class HistogramArtifact {
    protected String artifactName
    protected String artifactType

    HistogramArtifact(String artifactName, String artifactType) {
        this.artifactName = artifactName
        this.artifactType = artifactType
    }

    EntityCondition getCondition(ExecutionContextFactory ecf) {
        return ecf.entity.conditionFactory.makeCondition([artifactName:artifactName, artifactType:artifactType])
    }

    String toString() {
        return toString(null)
    }
    String toString(String prefix) {
        return "${prefix?:''}- Artifact type: ${artifactType}\n${prefix?:''}  Artifact name: ${artifactName}\n"
    }

}
