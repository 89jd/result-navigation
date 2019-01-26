package me.jackles.resultnavigation.common

actual sealed class ChoiceResult actual constructor() {
    actual data class OK<T> actual constructor(actual val data: T) : ChoiceResult()
    actual object Cancelled: ChoiceResult()
}

