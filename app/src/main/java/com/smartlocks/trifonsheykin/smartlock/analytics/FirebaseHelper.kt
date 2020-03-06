package com.smartlocks.trifonsheykin.smartlock.analytics

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics

class FirebaseHelper {

    companion object {
        private var analytics: FirebaseAnalytics? = null

        @JvmStatic
        fun logEvent(context: Context, event: Event) {
            if (analytics == null) {
                analytics = FirebaseAnalytics.getInstance(context)
            }
            analytics?.logEvent(event.value, null)
        }
    }
}