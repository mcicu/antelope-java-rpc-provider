package io.antelope.javarpcprovider.error;

import io.antelope.javasdk.error.rpcProvider.RpcProviderError;
import org.jetbrains.annotations.NotNull;

/**
 * Error thrown when there is an issue initializing the RPC Provider.
 */
public class EosioJavaRpcProviderInitializerError extends RpcProviderError {

    public EosioJavaRpcProviderInitializerError() {
    }

    public EosioJavaRpcProviderInitializerError(
            @NotNull String message) {
        super(message);
    }

    public EosioJavaRpcProviderInitializerError(
            @NotNull String message,
            @NotNull Exception exception) {
        super(message, exception);
    }

    public EosioJavaRpcProviderInitializerError(
            @NotNull Exception exception) {
        super(exception);
    }

}
