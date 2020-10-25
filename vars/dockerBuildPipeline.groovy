def call(body) {
    def ctx = setUpContext(body)

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
                        // This step is very important!!!
                        // Please do not remove it unless you find a better way without introducing "Init" stage because it's ugly :)"
                        // Later stages depend on it.
                        defineMoreContextBasedOnUserInput(ctx)

                        dockerBuild(ctx)
                        dockerPush(ctx)
                    }
                }
            }
        }
    }
}

def setUpContext(body) {
    // client-defined parameters in the body block
    def ctx = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = ctx
    body()

    checkBodyBlockParameters(body)

    // defining more parameters for ourselves
    ctx.dockerImages = []
    return ctx
}

// This method makes sure that all required parameters are set
// the reason why those parameters may not be set is because of
// evolution of this pipeline.
//
// For example in v1.0 "containerImages" was not available.
// Later in v1.1 it was introduced
//
// See "javaMicroserviceBuildPipelineTestSpec" test
// for api contract evolution details. It has tests for all versions of the pipeline.
//
// Maybe this is not the best strategy to support all possible
// API contracts within the single version of the pipeline.
// However having all clients looking at the same master version
// makes it possible to deploy changes to everyone in one shot
// (since I'm responsible to support pipelines of all java services it is super handy :)).
// And we don't need to create tags/branches for specific versions of this pipeline.
// Try googling Jenkins Shared Library versioning.
def checkBodyBlockParameters(ctx) {
    if (!ctx.containerImages) {
        if (ctx.service) {
            ctx.containerImages = [
                [ source: '.', name: ctx.service ]
            ]
        }
    }
}

def defineMoreContextBasedOnUserInput(ctx) {
    ctx.currentBranchName = "${BRANCH_NAME}"
    ctx.gitRev = gitRev()
}

def mavenBuild(ctx) {
    sh "mvn clean package${ctx.noUnitTests ? ' -DskipTests=true' : ''}"
}

def dockerBuild(ctx) {
    ctx.containerImages.each {
        def img = (it.name + ':' + dockerImgTag(ctx)).toLowerCase()
        def imgLatest = (it.name + ':latest').toLowerCase()

        sh "docker build -t ${img} ${it.source}"
        sh "docker tag ${img} ${imgLatest}"

        ctx.dockerImages << img
        ctx.dockerImages << imgLatest
    }
}

def dockerPush(ctx) {
    def push = { img, repo -> 
        def dest = "${repo}/${img}"
        sh "docker tag ${img} ${dest} && docker push ${dest}"
    }
    def to = Closure.IDENTITY
    
    ctx.dockerImages.each {
        push(it, to("blue.dockerhub.alutech.local"))
        push(it, to("green.dockerhub.alutech.local"))
    }
}

def gitRev() {
    sh(script: 'echo $(git rev-parse HEAD)', returnStdout: true).trim()
}

def dockerImgTag(ctx) {
    "${currentTimestamp()}__${ctx.currentBranchName.replace('/', '_')}__${ctx.gitRev}"
}

def currentTimestamp() {
    def clock = env.IS_CLOCK_MOCKED == 'true'
            ? MOCKED_CLOCK
            : java.time.Clock.system(java.time.ZoneId.of("UTC+3"))

    java.time.ZonedDateTime
        .now(clock)
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
}