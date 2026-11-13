package org.evsyukov.teleport.launch

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import org.evsyukov.teleport.data.Action

class ActionLauncher(private val context: Context) {
    fun launch(action: Action): Boolean {
        val intent = when (action) {
            is Action.AppLaunch -> context.packageManager.getLaunchIntentForPackage(action.packageName)
            is Action.OpenLink -> Intent(Intent.ACTION_VIEW, Uri.parse(action.uri))
        } ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}
