import com.mikhalchuk.*

def call(body) {
    def ctx = setUpContext(body)

    def helper = new DeploymentPipelineHelper(this)

    pipeline {
        agent { label 'docker-build' }
        options {
            buildDiscarder(logRotator(numToKeepStr: '5'))
            timestamps ()
        }
        stages {
            stage('docker') {
                steps {
                    container('docker') {
                        script {
                            // This step is very important!!!
                            // Please do not remove it unless you find a better way without introducing "Init" stage because it's ugly :)"
                            // Later stages depend on it.
                            defineMoreContextBasedOnUserInput(ctx)

                            helper.dockerBuild(ctx)
                            //dockerPush(ctx)
                        }
                    }
                }
            }
        }
    }
}

def setUpContext(body) {
    // client-defined parameters in the body block
    def ctx = JavaMicroservicePipelineContracts.resolve(ObjUtils.closureToMap(body))

    // defining more parameters for ourselves
    ctx.dockerImages = []
    return ctx
}

void defineMoreContextBasedOnUserInput(ctx) {
    ctx.currentBranchName = "${BRANCH_NAME}"
}