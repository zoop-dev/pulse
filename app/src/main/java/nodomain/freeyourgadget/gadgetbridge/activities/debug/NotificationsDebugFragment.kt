package nodomain.freeyourgadget.gadgetbridge.activities.debug

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.preference.ListPreference
import nodomain.freeyourgadget.gadgetbridge.BuildConfig
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType
import nodomain.freeyourgadget.gadgetbridge.util.GB
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.Objects

class NotificationsDebugFragment : AbstractDebugFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setupPreferences()
    }

    private fun setupPreferences() {
        setPreferencesFromResource(R.xml.debug_preferences_notifications, null)

        onClick(PREF_DEBUG_NOTIFICATIONS_SEND) { sendNotificationSpec() }
        onClick(PREF_DEBUG_PEBBLEKIT_NOTIFICATION) { testPebbleKitNotification() }
        onClick(PREF_DEBUG_CREATE_TEST_NOTIFICATION) { createTestNotification() }
        onClick(PREF_DEBUG_NOTIFICATIONS_RESET) { resetPreferences() }

        val callCommandPref = findPreference<ListPreference>(PREF_DEBUG_NOTIFICATIONS_TYPE)!!
        callCommandPref.entries = NotificationType.sortedValues().map { it.name }.toList().toTypedArray()
        callCommandPref.entryValues = callCommandPref.entries
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter()
        filter.addAction(ACTION_REPLY)
        ContextCompat.registerReceiver(requireContext(), mReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    override fun onStop() {
        super.onStop()
        try {
            requireContext().unregisterReceiver(mReceiver)
        } catch (e: Exception) {
            LOG.error("Failed to unregister receiver", e)
        }
    }

    private fun sendNotificationSpec() {
        val sharedPreferences = preferenceManager.sharedPreferences!!

        val notificationSpec = NotificationSpec()
        notificationSpec.flags = sharedPreferences.getString(PREF_DEBUG_NOTIFICATIONS_FLAGS, "0")!!.toInt()
        notificationSpec.key = sharedPreferences.getString(PREF_DEBUG_NOTIFICATIONS_KEY, "0|nodomain.freeyourgadget.gadgetbridge|-493519667|null|10679")
        notificationSpec.sender = sharedPreferences.getString(PREF_DEBUG_NOTIFICATIONS_SENDER, null)
        notificationSpec.phoneNumber = sharedPreferences.getString(PREF_DEBUG_NOTIFICATIONS_PHONENUMBER, null)
        notificationSpec.title = sharedPreferences.getString(PREF_DEBUG_NOTIFICATIONS_TITLE, "Title")
        notificationSpec.subject = sharedPreferences.getString(PREF_DEBUG_NOTIFICATIONS_SUBJECT, "Subject")
        notificationSpec.body = sharedPreferences.getString(PREF_DEBUG_NOTIFICATIONS_BODY, "Notification body")
        notificationSpec.type = NotificationType.valueOf(sharedPreferences.getString(PREF_DEBUG_NOTIFICATIONS_TYPE, "UNKNOWN")!!)
        notificationSpec.sourceName = sharedPreferences.getString(PREF_DEBUG_NOTIFICATIONS_SOURCENAME, "Gadgetbridge")
        if (notificationSpec.type != NotificationType.GENERIC_SMS) {
            // SMS notifications don't have a source app ID when sent by the SMSReceiver,
            // so let's not set it here as well for consistency
            notificationSpec.sourceAppId = sharedPreferences.getString(PREF_DEBUG_NOTIFICATIONS_SOURCEAPPID, BuildConfig.APPLICATION_ID)
        }
        notificationSpec.channelId = sharedPreferences.getString(PREF_DEBUG_NOTIFICATIONS_CHANNELID, "the_channel")
        notificationSpec.category = sharedPreferences.getString(PREF_DEBUG_NOTIFICATIONS_CATEGORY, null)
        notificationSpec.iconId = sharedPreferences.getString(PREF_DEBUG_NOTIFICATIONS_ICONID, "0")!!.toInt()

        if (sharedPreferences.getBoolean(PREF_DEBUG_NOTIFICATIONS_PICTUREPATH_BOOL, false)) {
            notificationSpec.picturePath = getTestPicture()?.absolutePath
        }

        notificationSpec.dndSuppressed = if (sharedPreferences.getBoolean(PREF_DEBUG_NOTIFICATIONS_DNDSUPPRESSED, false)) 1 else 0
        // TODO notificationSpec.cannedReplies = sharedPreferences.getString(PREF_DEBUG_NOTIFICATIONS_CANNEDREPLIES, null)
        notificationSpec.attachedActions = ArrayList<NotificationSpec.Action?>()

        // DISMISS action
        val dismissAction = NotificationSpec.Action()
        dismissAction.title = getString(R.string.dismiss)
        dismissAction.type = NotificationSpec.Action.TYPE_SYNTECTIC_DISMISS
        notificationSpec.attachedActions.add(dismissAction)

        if (sharedPreferences.getBoolean(PREF_DEBUG_NOTIFICATIONS_ATTACHEDACTIONS_REPLY, false)) {
            // REPLY action
            if (notificationSpec.type == NotificationType.GENERIC_SMS) {
                val replyAction = NotificationSpec.Action()
                replyAction.title = getString(R.string._pebble_watch_reply)
                replyAction.type = NotificationSpec.Action.TYPE_SYNTECTIC_REPLY_PHONENR
                notificationSpec.attachedActions.add(replyAction)
            } else {
                val replyAction = NotificationSpec.Action()
                replyAction.title = getString(R.string._pebble_watch_reply)
                replyAction.type = NotificationSpec.Action.TYPE_WEARABLE_REPLY
                notificationSpec.attachedActions.add(replyAction)
            }
        }

        runOnDebugDevices("Send NotificationSpec") {
            GBApplication.deviceService(it).onNotification(notificationSpec)
        }
    }

    private fun resetPreferences() {
        preferenceScreen.removeAll()

        preferenceManager.sharedPreferences!!.edit(true) {
            remove(PREF_DEBUG_NOTIFICATIONS_FLAGS)
            remove(PREF_DEBUG_NOTIFICATIONS_KEY)
            remove(PREF_DEBUG_NOTIFICATIONS_SENDER)
            remove(PREF_DEBUG_NOTIFICATIONS_PHONENUMBER)
            remove(PREF_DEBUG_NOTIFICATIONS_TITLE)
            remove(PREF_DEBUG_NOTIFICATIONS_SUBJECT)
            remove(PREF_DEBUG_NOTIFICATIONS_BODY)
            remove(PREF_DEBUG_NOTIFICATIONS_TYPE)
            remove(PREF_DEBUG_NOTIFICATIONS_SOURCENAME)
            remove(PREF_DEBUG_NOTIFICATIONS_CHANNELID)
            remove(PREF_DEBUG_NOTIFICATIONS_CATEGORY)
            remove(PREF_DEBUG_NOTIFICATIONS_ATTACHEDACTIONS_REPLY)
            remove(PREF_DEBUG_NOTIFICATIONS_SOURCEAPPID)
            remove(PREF_DEBUG_NOTIFICATIONS_ICONID)
            remove(PREF_DEBUG_NOTIFICATIONS_PICTUREPATH_BOOL)
        }

        // Reload the preference screen to reflect the changes
        setupPreferences()
    }

    private fun createTestNotification() {
        val notificationIntent = Intent(requireContext(), DebugActivityV2::class.java)
        notificationIntent.setPackage(BuildConfig.APPLICATION_ID)
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val pendingIntent = PendingIntent.getActivity(requireContext(), 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val remoteInput = RemoteInput.Builder(EXTRA_REPLY).build()

        val replyIntent = Intent(ACTION_REPLY)
        replyIntent.setPackage(BuildConfig.APPLICATION_ID)

        val replyPendingIntent = PendingIntent.getBroadcast(requireContext(), 0, replyIntent, PendingIntent.FLAG_MUTABLE)

        val action = NotificationCompat.Action.Builder(android.R.drawable.ic_input_add, "Reply", replyPendingIntent)
            .addRemoteInput(remoteInput)
            .build()

        val wearableExtender = NotificationCompat.WearableExtender().addAction(action)

        val pictureUri = getTestPicture()?.let {
            @SuppressLint("SetWorldReadable") it.setReadable(true, false)
            FileProvider.getUriForFile(
                requireContext(),
                "${BuildConfig.APPLICATION_ID}.screenshot_provider",
                it
            )
        }

        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(requireContext(), GB.NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getString(R.string.test_notification))
                .setContentText(getString(R.string.this_is_a_test_notification_from_gadgetbridge))
                .setTicker(getString(R.string.this_is_a_test_notification_from_gadgetbridge))
                .setSmallIcon(R.drawable.ic_notification)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .extend(wearableExtender)

        if (pictureUri != null) {
            try {
                val bitmap = BitmapFactory.decodeStream(requireContext().contentResolver.openInputStream(pictureUri))
                builder.setLargeIcon(bitmap)
                    .setStyle(NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .bigLargeIcon(null as android.graphics.Bitmap?))
            } catch (e: Exception) {
                LOG.error("Failed to decode image for notification", e)
            }
        }

        GB.notify(System.currentTimeMillis().toInt(), builder.build(), requireContext())
    }

    private fun getTestPicture(): File? {
        try {
            requireContext().assets.open("fossil_hr/default_background.png").use {
                val tempDir = File(requireContext().cacheDir, "images")
                tempDir.mkdir()
                val tempFile = File(tempDir, "test_notification_image.png")
                tempFile.outputStream().use { output ->
                    it.copyTo(output)
                }
                return tempFile
            }
        } catch (e: Exception) {
            LOG.error("Failed to copy test picture to cache", e)
            GB.toast("Test picture failed", Toast.LENGTH_SHORT, GB.ERROR)
        }

        return null
    }

    private fun testPebbleKitNotification() {
        val pebbleKitIntent = Intent("com.getpebble.action.SEND_NOTIFICATION")
        pebbleKitIntent.putExtra("messageType", "PEBBLE_ALERT")
        pebbleKitIntent.putExtra(
            "notificationData",
            """
                [{
                    "title": "PebbleKitTest",
                    "body": "Sent from Gadgetbridge"
                }]
            """.trimIndent()
        )
        GBApplication.getContext().sendBroadcast(pebbleKitIntent)
    }

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            when (Objects.requireNonNull(intent.action)) {
                ACTION_REPLY -> {
                    val remoteInput = RemoteInput.getResultsFromIntent(intent)
                    val reply = remoteInput!!.getCharSequence(EXTRA_REPLY)
                    LOG.info("Got wearable reply: $reply")
                    GB.toast(context, "Reply: $reply", Toast.LENGTH_SHORT, GB.INFO)
                }

                else -> LOG.warn("Unknown intent action: {}", intent.action)
            }
        }
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(DebugActivityV2::class.java)

        private const val EXTRA_REPLY = "reply"
        private const val ACTION_REPLY = "nodomain.freeyourgadget.gadgetbridge.DebugActivity.action.reply"

        private const val PREF_DEBUG_NOTIFICATIONS_SEND = "pref_debug_notifications_send"
        private const val PREF_DEBUG_PEBBLEKIT_NOTIFICATION = "pref_debug_pebblekit_notification"
        private const val PREF_DEBUG_CREATE_TEST_NOTIFICATION = "pref_debug_create_test_notification"
        private const val PREF_DEBUG_NOTIFICATIONS_RESET = "pref_debug_notifications_reset"
        private const val PREF_DEBUG_HEADER_CALLSPEC = "pref_header_callspec"
        private const val PREF_DEBUG_NOTIFICATIONS_FLAGS = "pref_debug_notifications_flags"
        private const val PREF_DEBUG_NOTIFICATIONS_KEY = "pref_debug_notifications_key"
        private const val PREF_DEBUG_NOTIFICATIONS_SENDER = "pref_debug_notifications_sender"
        private const val PREF_DEBUG_NOTIFICATIONS_PHONENUMBER = "pref_debug_notifications_phoneNumber"
        private const val PREF_DEBUG_NOTIFICATIONS_TITLE = "pref_debug_notifications_title"
        private const val PREF_DEBUG_NOTIFICATIONS_SUBJECT = "pref_debug_notifications_subject"
        private const val PREF_DEBUG_NOTIFICATIONS_BODY = "pref_debug_notifications_body"
        private const val PREF_DEBUG_NOTIFICATIONS_TYPE = "pref_debug_notifications_type"
        private const val PREF_DEBUG_NOTIFICATIONS_SOURCENAME = "pref_debug_notifications_sourceName"
        private const val PREF_DEBUG_NOTIFICATIONS_CHANNELID = "pref_debug_notifications_channelId"
        private const val PREF_DEBUG_NOTIFICATIONS_CATEGORY = "pref_debug_notifications_category"
        private const val PREF_DEBUG_NOTIFICATIONS_ATTACHEDACTIONS_REPLY = "pref_debug_notifications_attachedActions_reply"
        private const val PREF_DEBUG_NOTIFICATIONS_SOURCEAPPID = "pref_debug_notifications_sourceAppId"
        private const val PREF_DEBUG_NOTIFICATIONS_ICONID = "pref_debug_notifications_iconId"
        private const val PREF_DEBUG_NOTIFICATIONS_PICTUREPATH_BOOL = "pref_debug_notifications_picturePath_bool"
        private const val PREF_DEBUG_NOTIFICATIONS_DNDSUPPRESSED = "pref_debug_notifications_dndSuppressed"
    }
}
