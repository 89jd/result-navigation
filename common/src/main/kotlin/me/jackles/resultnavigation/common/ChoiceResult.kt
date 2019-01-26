package me.jackles.resultnavigation.common

expect sealed class ChoiceResult(){
    class OK<T>(data: T): ChoiceResult {
        val data: T
    }
    object Cancelled: ChoiceResult
}

inline fun <reified T>ChoiceResult.OK<*>.getValue(): T {
    if (data is T) {
        return data
    } else {
        throw Exception("Cannot cast data")
    }
}

