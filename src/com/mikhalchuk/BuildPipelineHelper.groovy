package com.mikhalchuk

class BuildPipelineHelper {

    def pipeline

    public BuildPipelineHelper(pipeline) {
        this.pipeline = pipeline
    }

    def dockerBuild(ctx) {
        ctx.dockerImageTag = dockerImgTag(ctx)

        ctx.containerImages.each {
            def img = (it.name + ':' + ctx.dockerImageTag).toLowerCase()
            def imgLatest = (it.name + ':latest').toLowerCase()

            pipeline.sh "docker build -t ${img} ${it.source}"
            pipeline.sh "docker tag ${img} ${imgLatest}"

            ctx.dockerImages << img
            ctx.dockerImages << imgLatest
        }
    }

    def dockerImgTag(ctx) {
        "${currentTimestamp()}__${pipeline.BRANCH_NAME.replace('/', '_')}__${gitRev()}"
    }

    def currentTimestamp() {
        def clock = pipeline.env.IS_CLOCK_MOCKED == 'true'
                ? MOCKED_CLOCK
                : java.time.Clock.system(java.time.ZoneId.of("UTC+3"))

        java.time.ZonedDateTime
                .now(clock)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
    }

    def gitRev() {
        pipeline.sh(script: 'echo $(git rev-parse HEAD)', returnStdout: true).trim()
    }

    def dockerPush(ctx) {
        def push = { img, repo ->
            def dest = "${repo}/${img}"
            pipeline.sh "docker tag ${img} ${dest} && docker push ${dest}"
        }
        def to = Closure.IDENTITY

        ctx.dockerImages.each {
            push(it, to("blue.dockerhub.alutech.local"))
            push(it, to("green.dockerhub.alutech.local"))
        }
    }
}
