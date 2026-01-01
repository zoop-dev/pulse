package nodomain.freeyourgadget.gadgetbridge

import android.content.Intent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager


class AppLifecycleObserver : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        LocalBroadcastManager.getInstance(GBApplication.getContext()).sendBroadcast( Intent(GBApplication.ACTION_APP_IS_IN_FOREGROUND))
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        LocalBroadcastManager.getInstance(GBApplication.getContext()).sendBroadcast( Intent(GBApplication.ACTION_APP_IS_IN_BACKGROUND))
    }

}