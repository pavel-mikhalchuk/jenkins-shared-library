import com.mikhalchuk.*

import hudson.model.Result
import hudson.model.Run
import jenkins.model.CauseOfInterruption.UserInterruption

def call(body) {
    def ctx = setUpContext(body)

    def builder = new BuildPipelineHelper(this)
    def deployer = new DeploymentPipelineHelper(this)

    pipeline {
        agent none
        options {
            buildDiscarder(logRotator(numToKeepStr: '5'))
            timestamps ()
        }
        stages {
            stage('abort-previous-builds') {
                steps {
                    abortPreviousBuilds()
                }
            }
            stage('docker-build') {
                agent { label 'docker-build' }
                steps {
                    container('docker') {
                        script {
                            builder.dockerBuild(ctx)
                            builder.dockerPush(ctx)
                        }
                    }
                }
            }
            stage('deploy-to-dev-dev') {
                input {
                    message "Deploy to 'dev-dev'?"
                }
                agent { label 'helm-deploy' }
                steps {
                    deploy('dev-dev', deployer, ctx)
                }
            }
        }
    }
}

def abortPreviousBuilds() {
    Run previousBuild = currentBuild.rawBuild.getPreviousBuildInProgress()

    while (previousBuild != null) {
        if (previousBuild.isInProgress()) {
            def executor = previousBuild.getExecutor()
            if (executor != null) {
                echo ">> Aborting older build #${previousBuild.number}"

                def cause = { "interrupted by build #${build.getId()}" as String } as CauseOfInterruption

                executor.interrupt(Result.ABORTED, cause)
            }
        }

        previousBuild = previousBuild.getPreviousBuildInProgress()
    }
}

def setUpContext(body) {
    // client-defined parameters in the body block
    def ctx = JavaMicroservicePipelineContracts.resolve(ObjUtils.closureToMap(body))

    // defining more parameters for ourselves
    ctx.dockerImages = []
    return ctx
}

def deploy(namespace, deployer, ctx) {
    script {
        deployer.defineJavaMsDeploymentContext(namespace, ctx.dockerImageTag, ctx)
        deployer.checkoutInfraRepo(ctx)

        deployer.copyConfigToHelmChart(ctx)
        deployer.writeHelmValuesYaml(ctx)
    }
    container('helm') {
        script {
            deployer.generateK8SManifests(ctx)
        }
    }
    script {
        deployer.pushK8SManifests(ctx)
    }
}