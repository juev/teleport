package org.evsyukov.teleport.shortcut

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import org.evsyukov.teleport.MainActivity
import org.evsyukov.teleport.R
import org.evsyukov.teleport.data.Action
import org.evsyukov.teleport.data.TeleportConfig
import org.evsyukov.teleport.launch.LaunchKeys

class ShortcutSync(private val context: Context) {

    fun sync(config: TeleportConfig) {
        val staticCount = ShortcutManagerCompat
            .getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_MANIFEST).size
        val max = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context)
        val visible = visibleActions(config.actions, max, staticCount)
        ShortcutManagerCompat.setDynamicShortcuts(context, visible.map { toShortcut(it) })
    }

    private fun toShortcut(action: Action): ShortcutInfoCompat {
        val label = action.label.ifEmpty { context.getString(R.string.app_name) }
        return ShortcutInfoCompat.Builder(context, action.id)
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(iconFor(action))
            .setIntent(intentFor(action))
            .build()
    }

    /**
     * A direct intent lets the launcher start the target itself, exactly like
     * the target's own icon: one transition, no Teleport trampoline and no
     * blank card (spec R5). When the target cannot be resolved right now, the
     * shortcut keeps the trampoline intent, so a tap still leads to the
     * settings with the reason instead of the launcher's own error (spec R3).
     */
    private fun intentFor(action: Action): Intent = directIntent(action) ?: trampolineIntent(action)

    private fun directIntent(action: Action): Intent? = when (action) {
        is Action.AppLaunch -> context.packageManager.getLaunchIntentForPackage(action.packageName)
        // Package visibility can hide a handler; the fallback then only costs the
        // trampoline transition, the launch itself still works.
        is Action.OpenLink -> Intent(Intent.ACTION_VIEW, action.uri.toUri())
            .takeIf { context.packageManager.resolveActivity(it, 0) != null }
    }

    private fun trampolineIntent(action: Action): Intent =
        Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .putExtra(LaunchKeys.ACTION_ID, action.id)

    private fun iconFor(action: Action): IconCompat = when (action) {
        is Action.AppLaunch -> try {
            val drawable = context.packageManager.getApplicationIcon(action.packageName)
            val bitmap = drawable.toBitmap()
            if (drawable is AdaptiveIconDrawable) {
                IconCompat.createWithAdaptiveBitmap(bitmap)
            } else {
                IconCompat.createWithBitmap(bitmap)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            IconCompat.createWithResource(context, R.drawable.ic_shortcut_link)
        }
        is Action.OpenLink -> IconCompat.createWithResource(context, R.drawable.ic_shortcut_link)
    }

    companion object {
        fun visibleActions(actions: List<Action>, max: Int, staticCount: Int): List<Action> =
            actions.take((max - staticCount).coerceAtLeast(0))
    }
}
