package com.mikhalchuk.tests


import com.mikhalchuk.testSupport.PipelineSpockTestBase

class MockUtils {
    static void mockDockerImgTagParam(PipelineSpockTestBase test) {
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

    static void mockInfraFolderName(PipelineSpockTestBase test) {
        test.helper.addShMock('echo infra-$(date +"%d-%m-%Y_%H-%M-%S")',
                'infra-06-06-2020_06-06-06', 0)
    }
}
