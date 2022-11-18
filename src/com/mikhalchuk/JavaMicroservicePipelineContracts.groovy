package com.mikhalchuk;

public class JavaMicroservicePipelineContracts {

    // This method makes sure that all required parameters are set.
    // The reason why those parameters may not be set is because of
    // evolution of this pipeline.
    //
    // See "javaMicroservicePipelineTestSpec" test
    // for api contract evolution details. It has tests for all versions of the pipeline.
    //
    // Maybe this is not the best strategy to support all possible
    // API contracts within the single version of the pipeline.
    // However having all clients looking at the same master version
    // makes it possible to deploy changes to everyone in one shot
    // (since I'm responsible to support pipelines of all java services it is super handy :)).
    // And we don't need to create tags/branches for specific versions of this pipeline.
    // Try googling Jenkins Shared Library versioning.
    static def resolve(contract) {
        if (is_v_1_1(contract)) return upgrade_v_1_1(contract)
        if (is_v_1_2(contract)) return upgrade_v_1_2(contract)
        if (is_v_1_3(contract)) return upgrade_v_1_3(contract)

        throw new IllegalArgumentException("Unsupported contract: ${contract}")
    }

    static boolean is_v_1_1(contract) {
        def keys = contract.keySet()
        if (keys.size() == 2 &&
                keys.contains('service') &&
                keys.contains('containerImages')) return true;
        return false;
    }

    static boolean is_v_1_2(contract) {
        def keys = contract.keySet()
        if (keys.size() == 3 &&
                keys.contains('service') &&
                keys.contains('containerImages') &&
                keys.contains('storage')) return true;
        return false;
    }

    static boolean is_v_1_3(contract) {
        def keys = contract.keySet()
        if (keys.size() == 3 &&
                keys.contains('service') &&
                keys.contains('containerImages') &&
                keys.contains('helmValues')) return true;
        return false;
    }

    static def upgrade_v_1_1(contract) {
        return latestContract(
                service: contract.service,
                containerImages: contract.containerImages,
                storage: null,
                helmValues: null
        )
    }

    static def upgrade_v_1_2(contract) {
        return latestContract(
                service: contract.service,
                containerImages: contract.containerImages,
                storage: contract.storage,
                helmValues: null
        )
    }

    static def upgrade_v_1_3(contract) {
        return latestContract(
                service: contract.service,
                containerImages: contract.containerImages,
                storage: null,
                helmValues: contract.helmValues
        )
    }

    static def latestContract(params) {
        return [
                service: params.service,
                containerImages: params.containerImages,
                storage: params.storage,
                helmValues: params.helmValues
        ]
    }
}