package com.mikhalchuk.tests

import com.mikhalchuk.testSupport.PipelineSpockTestBase

class javaMicroserviceDeploymentPipelineTestSpec extends PipelineSpockTestBase {

    def "test java ms deploy pipeline"() {
        given:
        def script = loadScript('vars/javaMicroserviceDeploymentPipeline.groovy')

        and:
        script.getBinding().setVariable("JOB_NAME", "pricing")
        script.getBinding().setVariable("BUILD_NUMBER", "666")
        script.getBinding().setVariable("BRANCH_NAME", "develpo")
        script.getBinding().setVariable("BUILD_USER", "mikhalchuk")

        and:
        script.initDockerImageChoiceParameter() >> {
            addParam("IMAGE_TAG", "latest")
        }

        when:
        script.call({
            service = 'pricing'
            namespaces = ['dev-dev']
            podResources = "DUMMY_RESOURCES"
        })

        then:
        println script.getBinding()
        printCallStack()
        assertJobStatusSuccess()
    }
}