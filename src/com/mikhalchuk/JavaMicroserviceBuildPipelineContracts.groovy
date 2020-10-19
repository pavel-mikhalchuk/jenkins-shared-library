package com.mikhalchuk

class JavaMicroserviceBuildPipelineContracts {

    // This method makes sure that all required parameters are set.
    // The reason why those parameters may not be set is because of
    // evolution of this pipeline.
    //
    // For example in v1.0 "containerImages" was not available.
    // Later in v1.1 it was introduced
    //
    // See "javaMicroserviceBuildPipelineTestSpec" test
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
        if (is_v_1_0(contract)) return upgrade_v_1_0(contract)
        if (is_v_1_1(contract)) return upgrade_v_1_1(contract)
        if (is_v_1_2(contract)) return contract

        throw new IllegalArgumentException("Unsupported contract: ${contract}")
    }

    static boolean is_v_1_0(contract) {
        def keys = contract.keySet()

        if (keys.size() == 1 && keys.contains('service')) return true;
        if (keys.size() == 2 && keys.contains('service') && keys.contains('noUnitTests')) return true;

        return false;
    }

    static def upgrade_v_1_0(oldContract) {
        return latestContract(
            params:  null,
            maven: {
                skipTests = oldContract.noUnitTests
            },
            containerImages: [
                [ source: '.', name: oldContract.service ]
            ]
        )
    }

    static boolean is_v_1_1(contract) {
        def keys = contract.keySet()

        if (keys.size() == 1 && keys.contains('containerImages')) return true;
        if (keys.size() == 2 && keys.contains('noUnitTests') && keys.contains('containerImages')) return true;

        return false;
    }

    static def upgrade_v_1_1(oldContract) {
        return latestContract(
            params: null,
            maven: {
                skipTests = oldContract.noUnitTests
            },
            containerImages: oldContract.containerImages
        )
    }

    static boolean is_v_1_2(contract) {
        def keys = contract.keySet()

        if (keys.size() == 1 && keys.contains('containerImages')) return true;
        if (keys.size() == 2 && keys.contains('maven') && keys.contains('containerImages')) return true;
        if (keys.size() == 3 && keys.contains('params') && keys.contains('maven')
                && keys.contains('containerImages')) return true;

        return false;
    }

    static def latestContract(params) {
        return [
            params: params.params,
            maven: params.maven,
            containerImages: params.containerImages
        ]
    }
}