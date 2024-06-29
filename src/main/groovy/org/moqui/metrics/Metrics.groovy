package org.moqui.metrics

import org.moqui.context.ExecutionContextFactory

abstract class Metrics {

    abstract void reload()

    abstract void init(ExecutionContextFactory ecf)

    abstract void registerArtifactHit(String artifactType, String artifactName, BigDecimal runningTimeMillis, String slowHit, String wasError)

    abstract String nextValues()

}
