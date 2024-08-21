package com.kutoru.mikunotes.logic

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.kutoru.mikunotes.logic.requests.RequestManager

class MikuNotesApp : Application() {

    private val activityCallbacks = ActivityLifecycleCallbacks()
    val currentActivity get() = activityCallbacks.currentActivity

    lateinit var requestManager: RequestManager
        private set

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(activityCallbacks)

        requestManager = RequestManager(
            applicationContext,
            PersistentStorage(applicationContext),
            NotificationHelper(applicationContext),
        )
    }

    override fun onTerminate() {
        unregisterActivityLifecycleCallbacks(activityCallbacks)
        super.onTerminate()
    }

    class ActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {

        var currentActivity: Activity? = null
            private set

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            currentActivity = activity
        }

        override fun onActivityStarted(activity: Activity) {}

        override fun onActivityResumed(activity: Activity) {}

        override fun onActivityPaused(activity: Activity) {}

        override fun onActivityStopped(activity: Activity) {}

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {
            if (activity === currentActivity) {
                currentActivity = null
            }
        }
    }
}
