package com.example.jezz.locationtest

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationProvider
import android.os.Bundle
import android.os.IBinder
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.IntentCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnCompleteListener

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    lateinit var locationProvider: FusedLocationProviderClient
    lateinit var notificationManager: NotificationManagerCompat

    private var locationService: LocationService? = null
    private var bound = false


    private val serviceConnection = object:ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            locationService = null
            bound = false
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            var binder = service as LocationService.LocalBinder
            locationService = binder.service
            bound =true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        locationProvider = LocationServices.getFusedLocationProviderClient(this)
        notificationManager = NotificationManagerCompat.from(applicationContext)


        fab.setOnClickListener { view ->
            checkGoogleAPI()
            Log.d("HOLA", "DESDE EL COSA")
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                    .setAction("Action", null).show()
            val permissionCoarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            if (permissionCoarse == PackageManager.PERMISSION_GRANTED) {
//                val location = locationProvider.lastLocation.addOnCompleteListener { task ->
//                    Log.d("LOCATION", task.result.latitude.toString())
//                }

                locationService?.requestLocationUpdates()

                Snackbar.make(view, "Clicked", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        bindService( Intent(this, LocationService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkGoogleAPI() {
        val googleAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)

        when (googleAvailable) {
            ConnectionResult.SUCCESS -> Log.d("GOOGLE_SERVICES", "Active")
            else -> Log.d("GOOGLE_SERVICEs", "not available")
        }
//        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 10)
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)) {
                Log.d("GOOGLE", "show permissions extra")
                Snackbar.make(fab, "Acceso a la camara requerid", Snackbar.LENGTH_LONG).setAction("Action", null).show()
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 10)
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 10)
            }
        }
    }
}
