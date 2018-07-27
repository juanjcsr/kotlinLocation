package com.example.jezz.locationtest

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.*
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task


//https://github.com/googlesamples/android-play-location/blob/master/LocationUpdatesForegroundService/app/src/main/java/com/google/android/gms/location/sample/locationupdatesforegroundservice/LocationUpdatesService.java
class LocationService : Service() {

    companion object {
        val TAG = LocationService::class.simpleName
        const val PACKAGE_NAME = "com.example.jezz.locationtest"
        const val CHANNEL_ID = "channel_01"
        const val EXTRA_STARTED_FROM_NOTIFICATION = "$PACKAGE_NAME.started_from_notification"
        const val ACTION_BROADCAST  = "$PACKAGE_NAME.broadcast"
        const val EXTRA_LOCATION = "$PACKAGE_NAME.location"

        var runningInForeground: Boolean = false

        const val NOTIFICATION_ID = 123123
        const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000
        const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2
    }

    /**
     * Used to check whether the bound activity has really gone away and not unbound as part of an
     * orientation change. We create a foreground service notification only if the former takes
     * place.
     */
    private var mChangingConfiguration = false
    val binder = LocalBinder()
    private lateinit var locationRequest: LocationRequest
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var serviceHandler: Handler
    private lateinit var notificationManager: NotificationManager
    private lateinit var location: Location

    override fun onCreate() {
//        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(lr: LocationResult) {
                super.onLocationResult(lr)
                onNewLocation(lr.lastLocation)
//                Log.d("LOCATION", lr.lastLocation.toString())
            }
        }

        createLocationRequest()
        lastLocation()

        var ht = HandlerThread(TAG)
        ht.start()
        serviceHandler = Handler(ht.looper)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var name = getString(R.string.app_name)
            var channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("SERVICE_LOCATION", "Starting service")
        var startedFromNotification = intent?.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION, false)
        if(startedFromNotification!!) {
            removeLocationUpdates()
            stopSelf()
        }
        return Service.START_NOT_STICKY
    }



    override fun onBind(intent: Intent): IBinder? {
        Log.i(TAG, "onBind!")
        stopForeground(true)
        runningInForeground = false
        mChangingConfiguration = false
        return null
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "Last client unbound from service")
        if( !mChangingConfiguration ) {
            Log.i(TAG, "starting foreground service")
            startForeground(NOTIFICATION_ID, getNotification())
            runningInForeground = true
        }
        return true
    }

    override fun onDestroy() {
        serviceHandler.removeCallbacksAndMessages(null)
    }

    private fun execCommand() {
        Log.d("SERVICE_LOCATION", "START")
    }

    fun requestLocationUpdates() {
        Log.i(TAG, "Requesting locationupdates!!")
        startService(Intent(applicationContext, LocationService::class.java))
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
        } catch (notLikely: SecurityException) {
            //handle request of location permission
            Log.e(TAG, "could not get location permission")
        }
    }

    private fun getNotification(): Notification {
        val intent = Intent(this, LocationService::class.java)
        var text = "Recibiendo notificaciones"
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true)
        val pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val activityPendingIntent = PendingIntent.getActivity(this,
                0,
                Intent(this, MainActivity::class.java),
                0)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
        builder
                .addAction(R.drawable.ic_launch, getString(R.string.launch_activity), activityPendingIntent)
                .addAction(R.drawable.ic_cancel, getString(R.string.remove_location_update), pendingIntent)
                .setContentText(text)
                .setContentTitle("Ubicación")
                .setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(text)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID)
        }
        return builder.build()
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.interval = UPDATE_INTERVAL_IN_MILLISECONDS
        locationRequest.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private fun lastLocation() {
        try {
            fusedLocationClient.lastLocation.addOnCompleteListener(
                    OnCompleteListener<Location> { task: Task<Location> ->
                if(task.isSuccessful && task.result != null) {
                    location = task.result
                } else {
                    Log.d(TAG, "Failed to get location :( ")
                }
            })
        } catch (notLikely: SecurityException) {
            Log.e(TAG, "Could not get location permission")
        }
    }

    fun removeLocationUpdates() {
        Log.i(TAG, "Remove location updates")
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            stopSelf()
        } catch (notLikely: SecurityException) {
            Log.e(TAG, "Lost location permission")
        }
    }

    private fun onNewLocation(loc: Location) {
        Log.i(TAG, "New location: $loc")
        location = loc

        var intent = Intent(ACTION_BROADCAST)
        intent.putExtra(EXTRA_LOCATION, loc)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)

        if (runningInForeground) {
            notificationManager.notify(NOTIFICATION_ID, getNotification())
        }
    }

    inner class LocalBinder : Binder() {
        internal val service: LocationService
            get() = this@LocationService
    }

}
