package com.mikhalchuk.tests

import com.mikhalchuk.testSupport.PipelineSpockTestBase

class javaMicroservicePipelineTestSpec extends PipelineSpockTestBase {

//    def "test java ms pipeline 1.0"() {
//        given:
//        def pipeline = loadScript('vars/javaMicroservicePipeline.groovy')
//
//        and:
//        pipeline.getBinding().setVariable('JOB_NAME', "${P_SERVICE}-deploy-${P_ENV}")
//        pipeline.getBinding().setVariable('BUILD_NUMBER', '666')
//        pipeline.getBinding().setVariable('BRANCH_NAME', P_BRANCH)
//        pipeline.getBinding().setVariable('BUILD_USER', 'mikhalchuk')

//        when:
//        pipeline {
//            service = 'java-micro-service'
//            containerImages = [
//                    [ source: '.', name: 'java-micro-service' ]
//            ]
//        }
//
//        then:
//        testNonRegression()
//        assertJobStatusSuccess()
//    }
}