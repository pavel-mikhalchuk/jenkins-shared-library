package com.mikhalchuk

import hudson.model.Result
import hudson.model.Run
import jenkins.model.CauseOfInterruption.UserInterruption

class PipelineHelper {

    def pipeline

    public PipelineHelper(pipeline) {
        this.pipeline = pipeline
    }

    def abortPreviousBuilds() {
        Run previousBuild = pipeline.currentBuild.rawBuild.getPreviousBuildInProgress()

        while (previousBuild != null) {
            if (previousBuild.isInProgress()) {
                def executor = previousBuild.getExecutor()
                if (executor != null) {
                    pipeline.echo ">> Aborting older build #${previousBuild.number}"
                    executor.interrupt(Result.ABORTED, new UserInterruption(
                            "Aborted by newer build #${currentBuild.number}"
                    ))
                }
            }

            previousBuild = previousBuild.getPreviousBuildInProgress()
        }
    }
}
