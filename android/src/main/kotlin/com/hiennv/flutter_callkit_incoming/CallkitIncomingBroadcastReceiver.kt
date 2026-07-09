package com.hiennv.flutter_callkit_incoming

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class CallkitIncomingBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CallkitIncomingReceiver"

        private const val TAXI_PREFS = "taxi_callkit_pending_actions"
        private const val TAXI_PENDING_ACTION = "pending_action"
        private const val TAXI_PENDING_CALL_ID = "pending_call_id"
        private const val TAXI_PENDING_CHANNEL = "pending_channel_name"
        private const val TAXI_PENDING_CALLER_UID = "pending_caller_uid"
        private const val TAXI_PENDING_RECEIVER_UID = "pending_receiver_uid"

        var silenceEvents = false

        fun getIntent(context: Context, action: String, data: Bundle?) =
            Intent(context, CallkitIncomingBroadcastReceiver::class.java).apply {
                this.action = "${context.packageName}.${action}"
                putExtra(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA, data)
            }

        fun getIntentIncoming(context: Context, data: Bundle?) =
            Intent(context, CallkitIncomingBroadcastReceiver::class.java).apply {
                action = "${context.packageName}.${CallkitConstants.ACTION_CALL_INCOMING}"
                putExtra(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA, data)
            }

        fun getIntentStart(context: Context, data: Bundle?) =
            Intent(context, CallkitIncomingBroadcastReceiver::class.java).apply {
                action = "${context.packageName}.${CallkitConstants.ACTION_CALL_START}"
                putExtra(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA, data)
            }

        fun getIntentAccept(context: Context, data: Bundle?) =
            Intent(context, CallkitIncomingBroadcastReceiver::class.java).apply {
                action = "${context.packageName}.${CallkitConstants.ACTION_CALL_ACCEPT}"
                putExtra(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA, data)
            }

        fun getIntentDecline(context: Context, data: Bundle?) =
            Intent(context, CallkitIncomingBroadcastReceiver::class.java).apply {
                action = "${context.packageName}.${CallkitConstants.ACTION_CALL_DECLINE}"
                putExtra(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA, data)
            }

        fun getIntentEnded(context: Context, data: Bundle?) =
            Intent(context, CallkitIncomingBroadcastReceiver::class.java).apply {
                action = "${context.packageName}.${CallkitConstants.ACTION_CALL_ENDED}"
                putExtra(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA, data)
            }

        fun getIntentTimeout(context: Context, data: Bundle?) =
            Intent(context, CallkitIncomingBroadcastReceiver::class.java).apply {
                action = "${context.packageName}.${CallkitConstants.ACTION_CALL_TIMEOUT}"
                putExtra(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA, data)
            }

        fun getIntentCallback(context: Context, data: Bundle?) =
            Intent(context, CallkitIncomingBroadcastReceiver::class.java).apply {
                action = "${context.packageName}.${CallkitConstants.ACTION_CALL_CALLBACK}"
                putExtra(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA, data)
            }

        fun getIntentHeldByCell(context: Context, data: Bundle?) =
            Intent(context, CallkitIncomingBroadcastReceiver::class.java).apply {
                action = "${context.packageName}.${CallkitConstants.ACTION_CALL_HELD}"
                putExtra(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA, data)
            }

        fun getIntentUnHeldByCell(context: Context, data: Bundle?) =
            Intent(context, CallkitIncomingBroadcastReceiver::class.java).apply {
                action = "${context.packageName}.${CallkitConstants.ACTION_CALL_UNHELD}"
                putExtra(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA, data)
            }

        fun getIntentConnected(context: Context, data: Bundle?) =
            Intent(context, CallkitIncomingBroadcastReceiver::class.java).apply {
                action = "${context.packageName}.${CallkitConstants.ACTION_CALL_CONNECTED}"
                putExtra(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA, data)
            }
    }

    private val callkitNotificationManager: CallkitNotificationManager? =
        FlutterCallkitIncomingPlugin.getInstance()?.getCallkitNotificationManager()

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val data = intent.extras?.getBundle(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA) ?: return

        when (action) {
            "${context.packageName}.${CallkitConstants.ACTION_CALL_INCOMING}" -> {
                try {
                    callkitNotificationManager?.showIncomingNotification(data)
                    sendEventFlutter(CallkitConstants.ACTION_CALL_INCOMING, data)
                    addCall(context, Data.fromBundle(data))
                } catch (error: Exception) {
                    Log.e(TAG, null, error)
                }
            }

            "${context.packageName}.${CallkitConstants.ACTION_CALL_START}" -> {
                try {
                    CallkitNotificationService.startServiceWithAction(
                        context,
                        CallkitConstants.ACTION_CALL_START,
                        data
                    )
                    sendEventFlutter(CallkitConstants.ACTION_CALL_START, data)
                    addCall(context, Data.fromBundle(data), true)
                } catch (error: Exception) {
                    Log.e(TAG, null, error)
                }
            }

            "${context.packageName}.${CallkitConstants.ACTION_CALL_ACCEPT}" -> {
                try {
                    saveTaxiPendingAction(context, "accept", data)
                    updateTaxiAgoraCall(context, data, "accepted")

                    CallkitNotificationService.startServiceWithAction(
                        context,
                        CallkitConstants.ACTION_CALL_ACCEPT,
                        data
                    )

                    sendEventFlutter(CallkitConstants.ACTION_CALL_ACCEPT, data)
                    addCall(context, Data.fromBundle(data), true)
                } catch (error: Exception) {
                    Log.e(TAG, null, error)
                }
            }

            "${context.packageName}.${CallkitConstants.ACTION_CALL_DECLINE}" -> {
                try {
                    callkitNotificationManager?.clearIncomingNotification(data, false)
                    updateTaxiAgoraCall(context, data, "declined")
                    sendEventFlutter(CallkitConstants.ACTION_CALL_DECLINE, data)
                    removeCall(context, Data.fromBundle(data))
                } catch (error: Exception) {
                    Log.e(TAG, null, error)
                }
            }

            "${context.packageName}.${CallkitConstants.ACTION_CALL_ENDED}" -> {
                try {
                    callkitNotificationManager?.clearIncomingNotification(data, false)
                    updateTaxiAgoraCall(context, data, "ended")
                    CallkitNotificationService.stopService(context)
                    sendEventFlutter(CallkitConstants.ACTION_CALL_ENDED, data)
                    removeCall(context, Data.fromBundle(data))
                } catch (error: Exception) {
                    Log.e(TAG, null, error)
                }
            }

            "${context.packageName}.${CallkitConstants.ACTION_CALL_TIMEOUT}" -> {
                try {
                    callkitNotificationManager?.clearIncomingNotification(data, false)
                    callkitNotificationManager?.showMissCallNotification(data)
                    updateTaxiAgoraCall(context, data, "missed")
                    sendEventFlutter(CallkitConstants.ACTION_CALL_TIMEOUT, data)
                    removeCall(context, Data.fromBundle(data))
                } catch (error: Exception) {
                    Log.e(TAG, null, error)
                }
            }

            "${context.packageName}.${CallkitConstants.ACTION_CALL_CONNECTED}" -> {
                try {
                    callkitNotificationManager?.showOngoingCallNotification(data, true)
                    sendEventFlutter(CallkitConstants.ACTION_CALL_CONNECTED, data)
                } catch (error: Exception) {
                    Log.e(TAG, null, error)
                }
            }

            "${context.packageName}.${CallkitConstants.ACTION_CALL_CALLBACK}" -> {
                try {
                    callkitNotificationManager?.clearMissCallNotification(data)
                    sendEventFlutter(CallkitConstants.ACTION_CALL_CALLBACK, data)
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                        val closeNotificationPanel = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
                        context.sendBroadcast(closeNotificationPanel)
                    }
                } catch (error: Exception) {
                    Log.e(TAG, null, error)
                }
            }
        }
    }

    private fun getTaxiExtra(data: Bundle, key: String): String {
        return try {
            val extra = data.getSerializable(CallkitConstants.EXTRA_CALLKIT_EXTRA)
            if (extra is HashMap<*, *>) {
                extra[key]?.toString() ?: ""
            } else {
                ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun getTaxiCallId(data: Bundle): String {
        val fromExtra = getTaxiExtra(data, "callId")
        if (fromExtra.isNotBlank()) return fromExtra

        return data.getString(CallkitConstants.EXTRA_CALLKIT_ID, "") ?: ""
    }

    private fun saveTaxiPendingAction(context: Context, action: String, data: Bundle) {
        try {
            val callId = getTaxiCallId(data)
            if (callId.isBlank()) return

            context.getSharedPreferences(TAXI_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(TAXI_PENDING_ACTION, action)
                .putString(TAXI_PENDING_CALL_ID, callId)
                .putString(TAXI_PENDING_CHANNEL, getTaxiExtra(data, "channelName"))
                .putString(TAXI_PENDING_CALLER_UID, getTaxiExtra(data, "callerUid"))
                .putString(TAXI_PENDING_RECEIVER_UID, getTaxiExtra(data, "receiverUid"))
                .apply()
        } catch (error: Exception) {
            Log.e(TAG, "Failed to save taxi pending action", error)
        }
    }

    private fun updateTaxiAgoraCall(context: Context, data: Bundle, status: String) {
        try {
            val callId = getTaxiCallId(data)
            if (callId.isBlank()) return

            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }

            val updates = hashMapOf<String, Any>(
                "status" to status
            )

            when (status) {
                "accepted" -> updates["acceptedAt"] = FieldValue.serverTimestamp()
                "declined", "missed", "ended" -> updates["endedAt"] = FieldValue.serverTimestamp()
            }

            FirebaseFirestore.getInstance()
                .collection("agoraCalls")
                .document(callId)
                .update(updates)
                .addOnFailureListener { error ->
                    Log.e(TAG, "Failed to update agoraCalls/$callId", error)
                }
        } catch (error: Exception) {
            Log.e(TAG, "Failed to update taxi agora call", error)
        }
    }

    private fun sendEventFlutter(event: String, data: Bundle) {
        if (silenceEvents) return

        val android = mapOf(
            "isCustomNotification" to data.getBoolean(CallkitConstants.EXTRA_CALLKIT_IS_CUSTOM_NOTIFICATION, false),
            "isCustomSmallExNotification" to data.getBoolean(CallkitConstants.EXTRA_CALLKIT_IS_CUSTOM_SMALL_EX_NOTIFICATION, false),
            "ringtonePath" to data.getString(CallkitConstants.EXTRA_CALLKIT_RINGTONE_PATH, ""),
            "backgroundColor" to data.getString(CallkitConstants.EXTRA_CALLKIT_BACKGROUND_COLOR, ""),
            "backgroundUrl" to data.getString(CallkitConstants.EXTRA_CALLKIT_BACKGROUND_URL, ""),
            "actionColor" to data.getString(CallkitConstants.EXTRA_CALLKIT_ACTION_COLOR, ""),
            "textColor" to data.getString(CallkitConstants.EXTRA_CALLKIT_TEXT_COLOR, ""),
            "incomingCallNotificationChannelName" to data.getString(CallkitConstants.EXTRA_CALLKIT_INCOMING_CALL_NOTIFICATION_CHANNEL_NAME, ""),
            "missedCallNotificationChannelName" to data.getString(CallkitConstants.EXTRA_CALLKIT_MISSED_CALL_NOTIFICATION_CHANNEL_NAME, ""),
            "isImportant" to data.getBoolean(CallkitConstants.EXTRA_CALLKIT_IS_IMPORTANT, true),
            "isBot" to data.getBoolean(CallkitConstants.EXTRA_CALLKIT_IS_BOT, false),
        )

        val missedCallNotification = mapOf(
            "id" to data.getInt(CallkitConstants.EXTRA_CALLKIT_MISSED_CALL_ID),
            "showNotification" to data.getBoolean(CallkitConstants.EXTRA_CALLKIT_MISSED_CALL_SHOW),
            "count" to data.getInt(CallkitConstants.EXTRA_CALLKIT_MISSED_CALL_COUNT),
            "subtitle" to data.getString(CallkitConstants.EXTRA_CALLKIT_MISSED_CALL_SUBTITLE),
            "callbackText" to data.getString(CallkitConstants.EXTRA_CALLKIT_MISSED_CALL_CALLBACK_TEXT),
            "isShowCallback" to data.getBoolean(CallkitConstants.EXTRA_CALLKIT_MISSED_CALL_CALLBACK_SHOW),
        )

        val callingNotification = mapOf(
            "id" to data.getString(CallkitConstants.EXTRA_CALLKIT_CALLING_ID),
            "showNotification" to data.getBoolean(CallkitConstants.EXTRA_CALLKIT_CALLING_SHOW),
            "subtitle" to data.getString(CallkitConstants.EXTRA_CALLKIT_CALLING_SUBTITLE),
            "callbackText" to data.getString(CallkitConstants.EXTRA_CALLKIT_CALLING_HANG_UP_TEXT),
            "isShowCallback" to data.getBoolean(CallkitConstants.EXTRA_CALLKIT_CALLING_HANG_UP_SHOW),
        )

        val forwardData = mapOf(
            "id" to data.getString(CallkitConstants.EXTRA_CALLKIT_ID, ""),
            "nameCaller" to data.getString(CallkitConstants.EXTRA_CALLKIT_NAME_CALLER, ""),
            "avatar" to data.getString(CallkitConstants.EXTRA_CALLKIT_AVATAR, ""),
            "number" to data.getString(CallkitConstants.EXTRA_CALLKIT_HANDLE, ""),
            "type" to data.getInt(CallkitConstants.EXTRA_CALLKIT_TYPE, 0),
            "duration" to data.getLong(CallkitConstants.EXTRA_CALLKIT_DURATION, 0L),
            "textAccept" to data.getString(CallkitConstants.EXTRA_CALLKIT_TEXT_ACCEPT, ""),
            "textDecline" to data.getString(CallkitConstants.EXTRA_CALLKIT_TEXT_DECLINE, ""),
            "extra" to data.getSerializable(CallkitConstants.EXTRA_CALLKIT_EXTRA),
            "missedCallNotification" to missedCallNotification,
            "callingNotification" to callingNotification,
            "android" to android
        )

        FlutterCallkitIncomingPlugin.sendEvent(event, forwardData)
    }
}
