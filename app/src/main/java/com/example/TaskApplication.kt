package com.example

import android.app.Application
import com.example.notifications.NotificationHelper

class TaskApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        NotificationHelper.restoreAllNotifications(this)
    }
}
