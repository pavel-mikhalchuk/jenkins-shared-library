package com.mikhalchuk.tests


import com.mikhalchuk.testSupport.PipelineSpockTestBase

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class MockUtils {
    static void mockParameters(PipelineSpockTestBase test) {
        def isDockerImgTagChoiceParam = { params ->
            params.get(0) instanceof Map && params.get(0)?.name == "IMAGE_TAG"
        }
        test.helper.registerAllowedMethod('parameters', [ArrayList.class], { params ->
            if (isDockerImgTagChoiceParam(params)) {
                test.addParam("IMAGE_TAG", "latest")
            }
        })
    }

    static void mockBuildUser(PipelineSpockTestBase test) {
        test.helper.registerAllowedMethod("wrap", [LinkedHashMap.class, Closure.class], { settings, body ->
            body()
        })
    }

    static void mockSlack(PipelineSpockTestBase test) {
        test.helper.registerAllowedMethod("slackSend", [LinkedHashMap.class], null)
    }

    static void mockGit(PipelineSpockTestBase test) {
        test.helper.registerAllowedMethod("git", [LinkedHashMap.class], null)
    }

    static void mockContainer(PipelineSpockTestBase test) {
        test.helper.registerAllowedMethod("container", [String.class, Closure.class], { name, body ->
            body()
        })
    }

    static void mockInput(PipelineSpockTestBase test) {
        test.helper.registerAllowedMethod("input", [Closure.class], { body ->
            body()
        })
    }

    static void mockMessage(PipelineSpockTestBase test) {
        test.helper.registerAllowedMethod("message", [String.class], { msg ->
            println msg
        })
    }

    static void mockDateShellCommand(PipelineSpockTestBase test) {
        test.helper.addShMock('echo $(date +"%d-%m-%Y_%H-%M-%S")',
                '06-06-2020_06-06-06', 0)
    }

    static void mockGitRevParse(PipelineSpockTestBase test) {
        test.helper.addShMock('echo $(git rev-parse HEAD)',
                'bbfcd9f9632d4d8d7a9b7b4f0f155f16c78660eb', 0)
    }

    static void mockClock(Script pipeline, PipelineSpockTestBase test) {
        test.addEnvVar('IS_CLOCK_MOCKED', 'true')
        pipeline.getBinding().setVariable('MOCKED_CLOCK',
                [Clock.fixed(Instant.parse('2020-08-30T00:59:45.00Z'), ZoneId.of("UTC")),
                 Clock.fixed(Instant.parse('2020-08-30T00:59:46.00Z'), ZoneId.of("UTC")),
                 Clock.fixed(Instant.parse('2020-08-30T00:59:47.00Z'), ZoneId.of("UTC")),
                 Clock.fixed(Instant.parse('2020-08-30T00:59:48.00Z'), ZoneId.of("UTC")),
                 Clock.fixed(Instant.parse('2020-08-30T00:59:49.00Z'), ZoneId.of("UTC"))])
    }
}
