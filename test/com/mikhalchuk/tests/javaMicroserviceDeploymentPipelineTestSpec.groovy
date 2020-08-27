package com.mikhalchuk.tests

import com.mikhalchuk.testSupport.PipelineSpockTestBase

import static com.mikhalchuk.tests.MockUtils.*

class javaMicroserviceDeploymentPipelineTestSpec extends PipelineSpockTestBase {

    def setup() {
        mockDockerImgTagParam(this)
        mockBuildUser(this)
        mockSlack(this)
        mockGit(this)
        mockContainer(this)
        mockInfraFolderName(this)
    }

    def "test pricing ms deploy dev pipeline"() {
        given:
        def script = loadScript('vars/javaMicroserviceDeploymentPipeline.groovy')

        and:
        script.getBinding().setVariable('JOB_NAME', 'pricing-deploy-dev')
        script.getBinding().setVariable('BUILD_NUMBER', '666')
        script.getBinding().setVariable('BRANCH_NAME', 'develop')
        script.getBinding().setVariable('BUILD_USER', 'mikhalchuk')

        when:
        script.call({
            service = 'pricing'
            namespaces = ['dev-dev']
            podResources = PodResources.PRICING_DEV
        })

        then:
        testNonRegression("pricing")
        assertJobStatusSuccess()
    }

    def "test calculation ms deploy dev pipeline"() {
        given:
        def script = loadScript('vars/javaMicroserviceDeploymentPipeline.groovy')

        and:
        script.getBinding().setVariable('JOB_NAME', 'calculation-deploy-dev')
        script.getBinding().setVariable('BUILD_NUMBER', '666')
        script.getBinding().setVariable('BRANCH_NAME', 'develop')
        script.getBinding().setVariable('BUILD_USER', 'mikhalchuk')

        when:
        script.call({
            service = 'calculation'
            namespaces = ['dev-dev']
            podResources = PodResources.CALCULATION_DEV
        })

        then:
        testNonRegression("calculation")
        assertJobStatusSuccess()
    }
}