def call(body) {
    def ctx = setUpContext(body)

    pipeline {
        agent { label 'java-build' }
        options { 
            buildDiscarder(logRotator(numToKeepStr: '5'))
            timestamps () 
        }
        stages {
            stage('maven') {
                steps {
                    container('maven') {
                        // This step is very important!!! 
                        // Please do not remove it unless you find a better way without introducing "Init" stage because it's ugly :)"
                        // Later stages depend on it.
                        defineMoreContextBasedOnUserInput(ctx)

                        mavenBuild(ctx)
                    }
                }
            }
            stage('docker') {
                steps {
                    container('docker') {
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
    
    // defining more parameters for ourselves
    ctx.dockerImages = []
    return ctx
}

def defineMoreContextBasedOnUserInput(ctx) {
    ctx.currentBranchName = "${BRANCH_NAME}"
    ctx.gitRev = gitRev()
}

def mavenBuild(ctx) {
    sh "mvn clean package${ctx.noUnitTests ? ' -DskipTests=true' : ''}"
}

def dockerBuild(ctx) {
    def img = ctx.service + ':' + dockerImgTag(ctx)
    def imgLatest = ctx.service + ':latest'
    
    sh "docker build -t ${img} ."
    sh "docker tag ${img} ${imgLatest}"

    ctx.dockerImages << img
    ctx.dockerImages << imgLatest
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
    "${currentTimestamp()}__${ctx.currentBranchName}__${ctx.gitRev}"
}

def currentTimestamp() {
    def clock = env.IS_CLOCK_MOCKED == 'true'
            ? MOCKED_CLOCK
            : java.time.Clock.system(java.time.ZoneId.of("UTC+3"))

    java.time.ZonedDateTime
        .now(clock)
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
}