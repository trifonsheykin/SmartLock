package com.smartlocks.trifonsheykin.smartlock.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

class FirebaseHelper {

    companion object {
        private var analytics: FirebaseAnalytics? = null

        @JvmStatic
        fun logEvent(
            context: Context,
            event: Event,
            params: Map<EventParam, String>? = null
        ) {
            if (analytics == null) {
                analytics = FirebaseAnalytics.getInstance(context)
            }
            val bundle = Bundle().apply {
                params?.forEach { (key, value) ->
                    putString(key.value, value)
                }
            }
            analytics?.logEvent(event.value, bundle)
        }
    }
}