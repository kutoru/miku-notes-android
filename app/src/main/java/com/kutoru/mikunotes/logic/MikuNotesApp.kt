package com.kutoru.mikunotes.logic

import android.app.Application
import com.kutoru.mikunotes.logic.requests.RequestManager

class MikuNotesApp : Application() {

    lateinit var requestManager: RequestManager
        private set

    override fun onCreate() {
        super.onCreate()
        requestManager = RequestManager(
            PersistentStorage(applicationContext),
            NotificationHelper(applicationContext),
        )
    }

}
