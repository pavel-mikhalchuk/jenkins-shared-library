package com.mikhalchuk.tests

import com.mikhalchuk.JavaMicroserviceBuildPipelineContracts
import spock.lang.Specification

import static org.assertj.core.api.Assertions.assertThat

class JavaMicroserviceBuildPipelineContractsTest extends Specification {

    def "test v 1.0 contract - conforms 1"() {
        given:
        def contract = [service: 'pricing', noUnitTests: false]

        when:
        def conformsToContract = JavaMicroserviceBuildPipelineContracts.is_v_1_0(contract)

        then:
        assertThat(conformsToContract).isEqualTo(true)
    }

    def "test v 1.0 contract - conforms 2"() {
        given:
        def contract = [service: 'pricing']

        when:
        def conformsToContract = JavaMicroserviceBuildPipelineContracts.is_v_1_0(contract)

        then:
        assertThat(conformsToContract).isEqualTo(true)
    }

    def "test v 1.0 contract - does not conform"() {
        given:
        def contract = [service: 'pricing', skipTests: true]

        when:
        def conformsToContract = JavaMicroserviceBuildPipelineContracts.is_v_1_0(contract)

        then:
        assertThat(conformsToContract).isEqualTo(false)
    }

    def "test v 1.1 contract - conforms 1"() {
        given:
        def contract = [noUnitTests: false, containerImages: []]

        when:
        def conformsToContract = JavaMicroserviceBuildPipelineContracts.is_v_1_1(contract)

        then:
        assertThat(conformsToContract).isEqualTo(true)
    }

    def "test v 1.1 contract - conforms 2"() {
        given:
        def contract = [containerImages: []]

        when:
        def conformsToContract = JavaMicroserviceBuildPipelineContracts.is_v_1_1(contract)

        then:
        assertThat(conformsToContract).isEqualTo(true)
    }

    def "test v 1.1 contract - does not conform"() {
        given:
        def contract = [skipTests: false, containerImages: []]

        when:
        def conformsToContract = JavaMicroserviceBuildPipelineContracts.is_v_1_1(contract)

        then:
        assertThat(conformsToContract).isEqualTo(false)
    }

    def "test v 1.2 contract - conforms 1"() {
        given:
        def contract = [params: {}, maven: {}, containerImages: []]

        when:
        def conformsToContract = JavaMicroserviceBuildPipelineContracts.is_v_1_2(contract)

        then:
        assertThat(conformsToContract).isEqualTo(true)
    }

    def "test v 1.2 contract - conforms 2"() {
        given:
        def contract = [maven: {}, containerImages: []]

        when:
        def conformsToContract = JavaMicroserviceBuildPipelineContracts.is_v_1_2(contract)

        then:
        assertThat(conformsToContract).isEqualTo(true)
    }

    def "test v 1.2 contract - conforms 3"() {
        given:
        def contract = [containerImages: []]

        when:
        def conformsToContract = JavaMicroserviceBuildPipelineContracts.is_v_1_2(contract)

        then:
        assertThat(conformsToContract).isEqualTo(true)
    }

    def "test v 1.2 contract - does not conform"() {
        given:
        def contract = [skipTests: {}, containerImages: []]

        when:
        def conformsToContract = JavaMicroserviceBuildPipelineContracts.is_v_1_2(contract)

        then:
        assertThat(conformsToContract).isEqualTo(false)
    }
}