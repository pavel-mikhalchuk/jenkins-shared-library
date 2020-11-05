package com.mikhalchuk

class JavaMicroserviceDeployPipelineContracts {

    // This method makes sure that all required parameters are set.
    // The reason why those parameters may not be set is because of
    // evolution of this pipeline.
    //
    // For example in v1.0 "ingress" was not available.
    // Later in v1.1 it was introduced
    //
    // See "javaMicroserviceDeployPipelineTestSpec" test
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

        if (keys.size() == 3 &&
                keys.contains('service') &&
                keys.contains('namespaces') &&
                keys.contains('podResources')) return true;

        return false;
    }

    static def upgrade_v_1_0(oldContract) {
        def env = oldContract.namespaces == ['prod'] ? 'prod' : 'dev'
        return latestContract(
            service:  oldContract.service,
            env: env,
            namespaces: oldContract.namespaces,
            helmValues: defaultHost(env) + Yaml.parse(oldContract.podResources)
        )
    }

    static boolean is_v_1_1(contract) {
        def keys = contract.keySet()

        if (keys.size() == 5 &&
                keys.contains('service') &&
                keys.contains('env') &&
                keys.contains('namespaces') &&
                keys.contains('ingress') &&
                keys.contains('podResources')) return true;

        return false;
    }

    static def upgrade_v_1_1(oldContract) {
        return latestContract(
            service:  oldContract.service,
            env: oldContract.env,
            namespaces: oldContract.namespaces,
            helmValues: (oldContract.ingress.host ? [host: oldContract.ingress.host] : [:])
                    + Yaml.parse(oldContract.podResources)
        )
    }

    static boolean is_v_1_2(contract) {
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

        if (keys.size() == 5 &&
                keys.contains('service') &&
                keys.contains('env') &&
                keys.contains('namespaces') &&
                keys.contains('preDeploy') &&
                keys.contains('helmValues')) return true;

        return false;
    }

    static def defaultHost(env) {
        return env == 'dev' ? [ host: Ingresses.SVC_NS_IN_IN.host ] : [:]
    }

    static def latestContract(params) {
        return [
            service: params.service,
            env: params.env,
            namespaces: params.namespaces,
            helmValues: params.helmValues
        ]
    }
}