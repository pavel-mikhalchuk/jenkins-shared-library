package com.mikhalchuk.tests

import com.mikhalchuk.Ingresses
import com.mikhalchuk.testSupport.PipelineSpockTestBase

import static com.mikhalchuk.Scripts.ASERVICE_PRE_DEPLOY
import static com.mikhalchuk.Scripts.ASERVICE_PRE_DEPLOY_MULTIPLE_DOMAINS
import static com.mikhalchuk.tests.HelmValues.*
import static com.mikhalchuk.tests.MockUtils.*

class javaMicroserviceDeploymentPipelineTestSpec extends PipelineSpockTestBase {

    def setup() {
        mockParameters(this)
        mockBuildUser(this)
        mockSlack(this)
        mockGit(this)
        mockContainer(this)
        mockDateShellCommand(this)
    }

    def "test java ms deploy pipeline 1.0"() {
        given:
        def pipeline = loadScript('vars/javaMicroserviceDeploymentPipeline.groovy')

        and:
        pipeline.getBinding().setVariable('JOB_NAME', "${P_SERVICE}-deploy-${P_ENV}")
        pipeline.getBinding().setVariable('BUILD_NUMBER', '666')
        pipeline.getBinding().setVariable('BRANCH_NAME', P_BRANCH)
        pipeline.getBinding().setVariable('BUILD_USER', 'mikhalchuk')

        when:
        pipeline {
            service = P_SERVICE
            namespaces = [P_NAMESPACE]
            podResources = P_POD_RESOURCES
        }

        then:
        testNonRegression("${P_SERVICE}_${P_ENV}_${P_NAMESPACE}")
        assertJobStatusSuccess()

        where:
        P_SERVICE | P_BRANCH  | P_ENV  | P_NAMESPACE | P_POD_RESOURCES
        "pricing" | "develop" | "dev"  | "dev-dev"   | PodResources.PRICING_DEV
        "pricing" | "develop" | "dev"  | "tst-test"  | PodResources.PRICING_DEV
        "pricing" | "master"  | "prod" | "prod"      | PodResources.PRICING_DEV
    }

    def "test java ms deploy pipeline 1.1"() {
        given:
        def pipeline = loadScript('vars/javaMicroserviceDeploymentPipeline.groovy')

        and:
        pipeline.getBinding().setVariable('JOB_NAME', "${P_SERVICE}-deploy-${P_ENV}")
        pipeline.getBinding().setVariable('BUILD_NUMBER', '666')
        pipeline.getBinding().setVariable('BRANCH_NAME', P_BRANCH)
        pipeline.getBinding().setVariable('BUILD_USER', 'mikhalchuk')

        when:
        pipeline.call({
            service = P_SERVICE
            env = P_ENV
            namespaces = [P_NAMESPACE]
            ingress = P_INGRESS
            podResources = P_POD_RESOURCES
        })

        then:
        testNonRegression("${P_SERVICE}_${P_ENV}_${P_NAMESPACE}")
        assertJobStatusSuccess()

        where:
        P_SERVICE     | P_BRANCH  | P_ENV  | P_NAMESPACE | P_INGRESS              | P_POD_RESOURCES
        "pricing"     | "develop" | "dev"  | "dev-dev"   | Ingresses.SVC_NS_IN_IN | PodResources.PRICING_DEV
        "pricing"     | "develop" | "dev"  | "tst-test"  | Ingresses.SVC_NS_IN_IN | PodResources.PRICING_DEV
        "pricing"     | "master"  | "prod" | "prod"      | Ingresses.DISABLED     | PodResources.PRICING_PROD
        "calculation" | "develop" | "dev"  | "dev-dev"   | Ingresses.SVC_NS_IN_IN | PodResources.CALCULATION_DEV
        "calculation" | "develop" | "dev"  | "tst-test"  | Ingresses.SVC_NS_IN_IN | PodResources.CALCULATION_DEV
        "calculation" | "master"  | "prod" | "prod"      | Ingresses.SVC_PROD     | PodResources.CALCULATION_PROD
    }

    def "test java ms deploy pipeline 1.2"() {
        given:
        def pipeline = loadScript('vars/javaMicroserviceDeploymentPipeline.groovy')

        and:
        pipeline.getBinding().setVariable('JOB_NAME', "${P_SERVICE}-deploy-${P_ENV}")
        pipeline.getBinding().setVariable('BUILD_NUMBER', '666')
        pipeline.getBinding().setVariable('BRANCH_NAME', P_BRANCH)
        pipeline.getBinding().setVariable('BUILD_USER', 'mikhalchuk')

        when:
        addParam("RESOURCES", P_MANUAL_RESOURCES)
        pipeline.call {
            service = P_SERVICE
            env = P_ENV
            namespaces = [P_NAMESPACE]
            preDeploy = P_PRE_DEPLOY
            helmValues = P_HELM_VALUES
        }

        then:
        testNonRegression("${P_SERVICE}_${P_ENV}_${P_NAMESPACE}")
        assertJobStatusSuccess()

        where:
        P_SERVICE     | P_BRANCH        | P_ENV  | P_NAMESPACE     | P_PRE_DEPLOY                         | P_HELM_VALUES                   | P_MANUAL_RESOURCES
        "pricing"     | "develop"       | "dev"  | "dev-dev"       | null                                 | PRICING_DEV                     | PodResources.PRICING_DEV
        "pricing"     | "develop"       | "dev"  | "tst-test"      | null                                 | PRICING_DEV                     | PodResources.PRICING_DEV
        "pricing"     | "master"        | "prod" | "prod"          | null                                 | PRICING_PROD                    | PodResources.PRICING_PROD
        "calculation" | "develop"       | "dev"  | "dev-dev"       | null                                 | CALCULATION_DEV                 | PodResources.CALCULATION_DEV
        "calculation" | "develop"       | "dev"  | "tst-test"      | null                                 | CALCULATION_DEV                 | PodResources.CALCULATION_DEV
        "calculation" | "master"        | "prod" | "prod"          | null                                 | CALCULATION_PROD                | PodResources.CALCULATION_PROD
        "calculation" | "master"        | "prod" | "prod"          | null                                 | CALCULATION_PROD                | PodResources.CALCULATION_PROD
        "calculation" | "master"        | "dev"  | "prod"          | null                                 | CALCULATION_DEV                 | PodResources.CALCULATION_PROD
        "aservice"    | "master"        | "dev"  | "dev-dev"       | ASERVICE_PRE_DEPLOY                  | ASERVICE_DEV                    | PodResources.ASERVICE_DEV
        "aservice"    | "no-javaOpts"   | "dev"  | "no-javaOpts"   | ASERVICE_PRE_DEPLOY                  | ASERVICE_DEV + [javaOpts: null] | PodResources.ASERVICE_DEV_JAVA_OPTS_NULL
        "aservice"    | "develop"       | "dev"  | "multi-domains" | ASERVICE_PRE_DEPLOY_MULTIPLE_DOMAINS | ASERVICE_DEV                    | PodResources.ASERVICE_DEV
        "aservice"    | "master"        | "prod" | "multi-domains" | ASERVICE_PRE_DEPLOY_MULTIPLE_DOMAINS | ASERVICE_PROD                   | PodResources.ASERVICE_DEV
    }
}