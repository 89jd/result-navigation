package me.jackles.resultnavigation

import kotlin.coroutines.*
import kotlin.native.concurrent.freeze
import kotlinx.coroutines.*
import me.jackles.resultnavigation.*
import platform.darwin.*
import platform.Foundation.*
import platform.UIKit.*

data class ViewControllerParams(
    internal val uiViewController: UIViewController,
    internal val nextUIViewController: UIViewController,
    internal val pushToNavigationViewController: Boolean
)

private const val ON_COMPLETE_NOTIFICATION = "ON_COMPLETE_NOTIFICATION"
private const val RESULT_KEY = "result"

private val isNavigationController = mutableMapOf<String, Boolean>()

fun <T> endFlow(uiViewController: UIViewController,
                result: T?) {
    val sendNotification = {
        NSNotificationCenter
            .defaultCenter
            .postNotificationName(
                aName = ON_COMPLETE_NOTIFICATION + uiViewController.hash,
                `object` = null,
                userInfo = mapOf(RESULT_KEY to result)
            )
    }
    if (isNavigationController[uiViewController.hash.toString()] == true) {
        uiViewController.navigationController?.popViewControllerAnimated(true)
        sendNotification()
    } else {
        uiViewController.dismissViewControllerAnimated(true,
            completion = sendNotification)
    }
}
class ViewControllerLauncher<T>: Launcher<ViewControllerParams, T> (
    { params, continuation ->
        params.apply {
            val uniqueId = params.nextUIViewController.hash
            isNavigationController[uniqueId.toString()] = uiViewController is UINavigationController
            NSNotificationCenter
                .defaultCenter
                .addObserverForName(name = ON_COMPLETE_NOTIFICATION + uniqueId,
                                    queue = null,
                                    `object` = null,
                                    usingBlock = {
                                        NSNotificationCenter
                                            .defaultCenter
                                            .removeObserver(this)
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
