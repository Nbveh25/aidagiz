package com.example.homework.core.data.source.api

import kotlinx.coroutines.TimeoutCancellationException
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.SocketException

fun Throwable.isGuideUnavailable(): Boolean =
    generateSequence(this) { it.cause }.any { error ->
        error is TimeoutCancellationException ||
            error is UnknownHostException ||
            error is ConnectException ||
            error is NoRouteToHostException ||
            error is SocketTimeoutException ||
            error is SocketException ||
            (error is IOException && error.isConnectionMessage())
    }

private fun IOException.isConnectionMessage(): Boolean {
    val message = message.orEmpty().lowercase()
    return listOf(
        "failed to connect",
        "connection refused",
        "connection reset",
        "network is unreachable",
        "software caused connection abort",
        "timed out",
        "timeout",
        "cleartext",
        "econnrefused",
        "no address associated",
        "unable to resolve host",
    ).any { it in message }
}
