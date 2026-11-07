package com.spaziopizza.admin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class OrderMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val manager = getSystemService(NotificationManager::class.java)
        val channelId = "spazio_new_orders"
        manager.createNotificationChannel(
            NotificationChannel(channelId, "Novos pedidos", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Alertas de novos pedidos da Spazio Pizza"
                enableVibration(true)
            }
        )
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = message.data["title"] ?: "Novo pedido na Spazio Pizza!"
        val body = message.data["body"] ?: "Abra o aplicativo para conferir e aceitar o pedido."
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()
        manager.notify(message.data["order_id"]?.hashCode() ?: System.currentTimeMillis().toInt(), notification)
    }

    override fun onNewToken(token: String) {
        // Próxima etapa: associar o token à sessão de administrador no Supabase,
        // usando autenticação e políticas RLS. Não enviar tokens anonimamente.
        super.onNewToken(token)
    }
}
