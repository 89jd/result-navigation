package me.jackles.resultnavigation

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

open class Launcher<InputParams, Result> (private val block : (InputParams,
                                                               CancellableContinuation<Result>) -> Unit) {
    suspend fun launchForResult(inputParams: InputParams,
                                onCancelled: (() -> Unit)? = null): Result = suspendCancellableCoroutine {
        it.invokeOnCancellation { throwable ->
            if (throwable == null) {
                onCancelled?.invoke()
            }
        }
        block(
            inputParams,
            it
        )
    }
}