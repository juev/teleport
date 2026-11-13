package org.evsyukov.teleport.shortcut

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutManager
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import org.evsyukov.teleport.MainActivity
import org.evsyukov.teleport.R
import org.evsyukov.teleport.data.Action
import org.evsyukov.teleport.data.TeleportConfig
import org.evsyukov.teleport.launch.LaunchKeys

class ShortcutSync(private val context: Context) {

    fun sync(config: TeleportConfig) {
        val staticCount = ShortcutManagerCompat
            .getShortcuts(context, ShortcutManager.FLAG_MATCH_MANIFEST).size
        val max = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context)
        val visible = visibleActions(config.actions, max, staticCount)
        ShortcutManagerCompat.setDynamicShortcuts(context, visible.map { toShortcut(it) })
    }

    private fun toShortcut(action: Action): ShortcutInfoCompat {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .putExtra(LaunchKeys.ACTION_ID, action.id)
        val label = action.label.ifEmpty { context.getString(R.string.app_name) }
        return ShortcutInfoCompat.Builder(context, action.id)
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(iconFor(action))
            .setIntent(intent)
            .build()
    }

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
