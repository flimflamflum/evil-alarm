package com.evilalarm.app

import android.app.Application
import android.content.Context
import com.evilalarm.app.alarm.Notifications
import com.evilalarm.app.data.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class EvilAlarmApp : Application() {
    lateinit var repository: AlarmRepository
        private set

    /** For small writes that must outlive the service or receiver that started them. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        repository = AlarmRepository(this)
        Notifications.createChannels(this)
    }
}

val Context.repository: AlarmRepository
    get() = (applicationContext as EvilAlarmApp).repository

val Context.appScope: CoroutineScope
    get() = (applicationContext as EvilAlarmApp).appScope
