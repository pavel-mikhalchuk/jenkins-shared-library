def call(body) {
    def ctx = setUpContext(body)

    pipeline {
        agent any
        options { 
            buildDiscarder(logRotator(numToKeepStr: '5'))
            timestamps () 
        }
        // tools { 
        //     maven 'Maven 3.5.0'
        // }
        stages {
            stage('build') {
                steps {
                    mavenBuild(ctx)
                    dockerBuild(ctx)
                }
            }
            stage('publish artifacts') {
                steps {
                    dockerPush(ctx)
                }
            }
        }
        post {
            always {
                deleteDir() /* clean up our workspace */
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
    // sh "mvn clean package -DskipTests=${ctx.noUnitTests}"
    sh "echo 'maven build'"
}

def dockerBuild(ctx) {
    def imgTag = ctx.service + ':' + gitRev()
    def imgTagLatest = ${ctx.service + ':latest'}
    
    // sh "docker build -t ${imgTag} ."
    sh "docker pull busybox"
    sh "docker tag busybox ${imgTag}"

    ctx.dockerImages << ctx.blueRepo + '/' + imgTag
    ctx.dockerImages << ctx.greenRepo + '/' + imgTag
    ctx.dockerImages << ctx.blueRepo + '/' + imgTagLatest
    ctx.dockerImages << ctx.greenRepo + '/' + imgTagLatest

    ctx.dockerImages.each {
        sh "docker tag ${imgTag} ${it}"
    }
}
def dockerPush(ctx) {
    ctx.dockerImages.each {
        sh "docker push ${it}"
    }
}

def gitRev() {
    sh(script: 'echo $(git rev-parse HEAD)', returnStdout: true).trim()
}