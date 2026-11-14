package com.thevault.alerts

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.thevault.MainActivity
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.repository.SettingsRepository
import com.thevault.domain.usecase.RefreshQuotesUseCase
import com.thevault.ui.component.formatEuro
import com.thevault.ui.screen.collection.VaultEntriesSource
import com.thevault.ui.screen.wishlist.underMaxPrice
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

/**
 * Refreshes the eBay quotes in the background and notifies when a wishlist item's PAL quote
 * falls to or under its max price. Each item is notified once per price.
 */
class WishAlertWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params),
    KoinComponent {
    private val source: VaultEntriesSource by inject()
    private val refreshQuotes: RefreshQuotesUseCase by inject()
    private val settings: SettingsRepository by inject()

    override suspend fun doWork(): Result {
        val ids = source.entries().first().map { it.id }
        refreshQuotes(ids)
        val under =
            source
                .entries()
                .first()
                .filter { it.status == ItemStatus.WISHLIST }
                .mapNotNull { e -> e.underMaxPrice?.let { e to it } }
        val notified = parseNotified(settings.get(KEY_NOTIFIED))
        val fresh = under.filter { (e, price) -> notified[e.id]?.let { price < it } ?: true }
        if (fresh.isNotEmpty() && canNotify()) notify(fresh.map { (e, p) -> e.title to p })
        settings.set(KEY_NOTIFIED, under.joinToString(";") { (e, p) -> "${e.id}=$p" })
        return Result.success()
    }

    private fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    @Suppress("MissingPermission") // checked by canNotify()
    private fun notify(items: List<Pair<String, Double>>) {
        val context = applicationContext
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Envies sous ton prix", NotificationManager.IMPORTANCE_DEFAULT))
        val open =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE,
            )
        val title = if (items.size == 1) "${items.first().first} est sous ton prix" else "${items.size} envies sous ton prix"
        val text = items.take(MAX_LINES).joinToString("\n") { (t, p) -> "$t : ${formatEuro(p)}" }
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setContentTitle(title)
                .setContentText(text.lineSequence().first())
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    /** Companion helpers. */
    companion object {
        private const val WORK_NAME = "wish-alerts"
        private const val CHANNEL = "wish_alerts"
        private const val NOTIFICATION_ID = 4201
        private const val MAX_LINES = 5
        private const val PERIOD_HOURS = 12L

        /** Setting holding the last notified price per item. */
        const val KEY_NOTIFIED = "wish.notified"

        /** Schedules the periodic check (kept when already scheduled). */
        fun schedule(context: Context) {
            val request =
                PeriodicWorkRequestBuilder<WishAlertWorker>(PERIOD_HOURS, TimeUnit.HOURS)
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** `id=price;id=price` → map. */
        fun parseNotified(raw: String?): Map<String, Double> =
            raw
                .orEmpty()
                .split(';')
                .mapNotNull { part -> part.split('=').takeIf { it.size == 2 }?.let { (id, p) -> p.toDoubleOrNull()?.let { id to it } } }
                .toMap()
    }
}
