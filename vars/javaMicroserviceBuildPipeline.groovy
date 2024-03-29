import com.mikhalchuk.*

def call(body) {
    def ctx = setUpContext(body)

    initUserDefinedParameters(ctx)

    def agentLabel = agentLabelForJava(ctx.javaVersion)

    pipeline {
        agent { label "${agentLabel}"}
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
                        dockerLoginNexus(ctx)
                        dockerPush(ctx)
                    }
                }
            }
        }
    }
}

def setUpContext(body) {
    // client-defined parameters in the body block
    def ctx = JavaMicroserviceBuildPipelineContracts.resolve(ObjUtils.closureToMap(body))

    // defining more parameters for ourselves
    ctx.dockerImages = []
    return ctx
}

def initUserDefinedParameters(ctx) {
    if (ctx.params) {
        def interceptor = new ParamsInterceptor(this)
        interceptor.delegate = this

        ctx.params.resolveStrategy = Closure.DELEGATE_ONLY
        ctx.params.delegate = interceptor
        ctx.params()

        properties([parameters(interceptor.params)])
    }
}

def agentLabelForJava(javaVersion){
    return javaVersion == null
            ? 'java-build'
            : 'java-build-16'
}

def defineMoreContextBasedOnUserInput(ctx) {
    ctx.currentBranchName = "${BRANCH_NAME}"
    ctx.gitRev = gitRev()
}

def mavenBuild(ctx) {
    def mvnParams = ObjUtils.closureToMap(ctx.maven, [params: params])

    sh "mvn clean package${mvnParams.skipTests ? ' -DskipTests=true' : ''}${mvnParams.args ? ' ' + mvnParams.args : ''}"
}

def dockerBuild(ctx) {
    def tag = dockerImgTag(ctx).toLowerCase()

    ctx.containerImages.each {
        def img = (it.name + ':' + tag).toLowerCase()
        // def imgLatest = (it.name + ':latest').toLowerCase()

        sh "docker build -t ${img} ${it.source}"
        // sh "docker tag ${img} ${imgLatest}"

        ctx.dockerImages << img
        // ctx.dockerImages << imgLatest
    }
}

// Login to Nexus. NEXUS_USER & NEXUS_PASSWORD runner container vars
def dockerLoginNexus(ctx) {
    sh 'docker login -u $NEXUS_USER -p $NEXUS_PASSWORD nexus-dockerhub.alutech.local'
}

def dockerPush(ctx) {
    def push = { img, repo -> 
        def dest = "${repo}/${img}"
        sh "docker tag ${img} ${dest} && docker push ${dest}"
    }
    def to = Closure.IDENTITY
    
    ctx.dockerImages.each {
        // push(it, to("blue.dockerhub.alutech.local"))
        // push(it, to("green.dockerhub.alutech.local"))
        push(it, to("nexus-dockerhub.alutech.local"))
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
            ? MOCKED_CLOCK.remove(0)
            : java.time.Clock.system(java.time.ZoneId.of("UTC+3"))

    java.time.ZonedDateTime
        .now(clock)
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
}