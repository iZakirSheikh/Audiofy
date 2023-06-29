package com.prime.media.core.util

import android.os.Handler
import android.os.Looper
import android.os.Message

private typealias callback = () -> Unit

private const val UNTIL_INFINITY = -1


@Deprecated("Don't use it; use coroutines instead.")
/**
 * A singleton handler linked with [Looper.getMainLooper].
 */
object MainHandler : Handler(Looper.getMainLooper()) {

    /**
     * This property generates a unique token after each get call.
     */
    var token = 0
        private set
        get() = field++

    private val calls: HashMap<Int, callback> = HashMap()


    /**
     * Constructs a [Message] with [Message.what] as [token], [Message.arg1] as delayMills and
     * [Message.arg2] as [until]
     */
    private fun Timer(token: Int, mills: Long, until: Long) =
        Message.obtain().apply {
            what = token
            arg1 = mills.toInt()
            arg2 = until.toInt()
        }

    /**
     * Adds token to [calls].
     */
    @kotlin.jvm.Throws(IllegalArgumentException::class)
    private fun add(token: Int, call: () -> Unit) {
        require(!calls.contains(token)) {
            "Token already present in [calls]"
        }
        calls[token] = call
    }


    /**
     * Repeats and registers [token] [call]  after mills.
     *
     * Note: creates an infinite loop if [until] = -1 and cancels only if forced removed using [remove]
     */
    fun repeat(token: Int, mills: Long, until: Long = UNTIL_INFINITY.toLong(), call: () -> Unit) {
        calls[token] = call
        val msg = Timer(token, mills, until)
        sendMessage(msg)
    }

    /**
     * Removes the registered [token].
     */
    fun remove(token: Int): callback? {
        removeMessages(token)
        return calls.remove(token)
    }

    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        //invoke
        calls[msg.what]?.invoke() ?: return
        // check if this is repeatable
        val mills = msg.arg1.toLong()
        val until = if (msg.arg2 == UNTIL_INFINITY) -1 else (msg.arg2 - mills).coerceAtLeast(0)
        if (until != 0L)
            sendMessageDelayed(Timer(msg.what, mills, until), mills)
        else
        // automatically remove the call.
            calls.remove(msg.what)
    }
}