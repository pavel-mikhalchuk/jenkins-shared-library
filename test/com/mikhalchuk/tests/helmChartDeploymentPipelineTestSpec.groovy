package com.mikhalchuk.tests


import com.mikhalchuk.testSupport.PipelineSpockTestBase

import static com.mikhalchuk.tests.MockUtils.*

class helmChartDeploymentPipelineTestSpec extends PipelineSpockTestBase {

    def setup() {
        mockParameters(this)
        mockBuildUser(this)
        mockSlack(this)
        mockGit(this)
        mockContainer(this)
        mockInfraFolderName(this)
    }

    def "test helm chart deploy pipeline 1.0"() {
        given:
        def pipeline = loadScript('vars/helmChartDeploymentPipeline.groovy')

        and:
        pipeline.getBinding().setVariable('JOB_NAME', "${P_SERVICE}-deploy-${P_ENV}")
        pipeline.getBinding().setVariable('BUILD_NUMBER', '666')
        pipeline.getBinding().setVariable('BRANCH_NAME', P_BRANCH)
        pipeline.getBinding().setVariable('BUILD_USER', 'mikhalchuk')

        and:
        helper.addShMock('$(super-service-get-password-shell-command)', 'alex111', 0)

        when:
        pipeline.call {
            service = P_SERVICE
            env = P_ENV
            namespaces = [P_NAMESPACE]
            helmValues = P_HELM_VALUES
        }

        then:
        testNonRegression("${P_SERVICE}_${P_ENV}_${P_NAMESPACE}")
        assertJobStatusSuccess()

        where:
        P_SERVICE       | P_BRANCH  | P_ENV  | P_NAMESPACE | P_HELM_VALUES
        "super-service" | "develop" | "dev"  | "dev-dev"   | HelmValues.SUPER_SERVICE
        "super-service" | "master"  | "prod" | "prod"      | HelmValues.SUPER_SERVICE + [resources: null]
    }
}