package com.mikhalchuk

class HelmChartDeployPipelineContracts {

    // This method makes sure that all required parameters are set.
    // The reason why those parameters may not be set is because of
    // evolution of this pipeline.
    //
    // See "helmChartDeployPipelineTestSpec" test
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
        if (is_v_1_0(contract)) return contract

        throw new IllegalArgumentException("Unsupported contract: ${contract}")
    }

    static boolean is_v_1_0(contract) {
        def keys = contract.keySet()

        if (keys.size() == 3 &&
                keys.contains('service') &&
                keys.contains('namespaces') &&
                keys.contains('env')) return true;

        if (keys.size() == 4 &&
                keys.contains('service') &&
                keys.contains('env') &&
                keys.contains('namespaces') &&
                keys.contains('helmValues')) return true;

        return false;
    }
}