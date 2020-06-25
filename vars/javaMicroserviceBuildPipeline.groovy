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

def mavenBuild(ctx) {
    sh "mvn clean package -DskipTests=${ctx.noUnitTests}"
}

def dockerBuild(ctx) {
    def imgTag = ctx.service + ':' + gitRev()
    def imgTagLatest = ctx.service + ':latest'
    
    sh "docker build -t ${imgTag} ."
    sh "docker tag ${imgTag} ${imgTagLatest}"

    ctx.dockerImages << imgTag
    ctx.dockerImages << imgTagLatest
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