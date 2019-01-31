package me.jackles.resultnavigation

import kotlin.coroutines.*
import kotlin.native.concurrent.freeze
import kotlinx.coroutines.*
import me.jackles.resultnavigation.*
import platform.darwin.*
import platform.Foundation.*
import platform.UIKit.*

interface ResultHandler<T> {
    var result: T?
}

data class ViewControllerParams(
    internal val uiViewController: UIViewController,
    internal val nextUIViewController: UIViewController,
    internal val pushToNavigationViewController: Boolean
)

private const val ON_COMPLETE_NOTIFICATION = "ON_COMPLETE_NOTIFICATION"
private const val RESULT_KEY = "result"
private var uniqueId: Long = 0

fun <T> endFlow(resultHandler: ResultHandler<T>) {
    NSNotificationCenter
                .defaultCenter
                .postNotificationName(
                    aName = ON_COMPLETE_NOTIFICATION,
                    `object` = null,
                    userInfo = mapOf(RESULT_KEY to resultHandler.result)
                )
}
class ViewControllerLauncher<T>: Launcher<ViewControllerParams, T> (
    { params, continuation ->
        params.apply {
            NSNotificationCenter
                .defaultCenter
                .addObserverForName(name = ON_COMPLETE_NOTIFICATION + uniqueId,
                                    queue = null,
                                    `object` = null,
                                    usingBlock = {
                                        it?.userInfo?.get(RESULT_KEY)?.let {
                                            continuation.resume(it as T)
                                        }?: run {
                                            continuation.cancel()
                                        }
                                    })
            
            if (pushToNavigationViewController && uiViewController is UINavigationController) {
                uiViewController.pushViewController(nextUIViewController,
                                                    animated = true)
            } else {
                uiViewController.presentViewController(nextUIViewController,
                                                        animated = true,
                                                        completion = null)
            }
        }
    }
)