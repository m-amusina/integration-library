package ru.evotor.framework.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle

abstract class BroadcastEventReceiver : BroadcastReceiver() {

    final override fun onReceive(context: Context, intent: Intent) {
        intent.action?.let { action ->
            intent.extras?.let { extras ->
                onEvent(context, action, extras)
            }
        }
    }

    protected abstract fun onEvent(context: Context, action: String, bundle: Bundle)

}