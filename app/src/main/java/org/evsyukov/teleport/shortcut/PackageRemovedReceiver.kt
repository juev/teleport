package org.evsyukov.teleport.shortcut

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.evsyukov.teleport.TeleportApplication

/**
 * A shortcut with a direct intent stops resolving once its target is gone, and
 * the launcher would show its own error instead of the settings (spec R3).
 * Republishing the shortcuts as soon as a package is removed replaces the
 * intent with the trampoline fallback, so the item leads to the settings again.
 */
class PackageRemovedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_PACKAGE_FULLY_REMOVED) return
        (context.applicationContext as TeleportApplication).resyncShortcuts()
    }
}
