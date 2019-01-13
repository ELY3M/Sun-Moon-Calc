package own.sunmooncalc

import android.app.Activity
import android.location.Location
import android.os.Bundle
import android.widget.TextView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.widget.Toast
import com.google.android.gms.location.*
import org.shredzone.commons.suncalc.MoonTimes
import org.shredzone.commons.suncalc.SunTimes
import java.text.SimpleDateFormat
import java.util.*
import org.shredzone.commons.suncalc.MoonIllumination





class MainActivity : Activity()  {
    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null
    private val INTERVAL: Long = 2000
    private val FASTEST_INTERVAL: Long = 1000
    lateinit var mLastLocation: Location
    internal lateinit var mLocationRequest: LocationRequest
    private val REQUEST_PERMISSION_LOCATION = 10

    var mylat: Double = 0.0
    var mylon: Double = 0.0
    var buildtext: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        val text: TextView = findViewById(R.id.text)
        mLocationRequest = LocationRequest()

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps()
        }

        if (checkPermissionForLocation(this)) {
            startLocationUpdates()


    }

}




    private fun buildAlertMessageNoGps() {

        val builder = AlertDialog.Builder(this)
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes") { _, _ ->
                startActivityForResult(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), 11)
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.cancel()
                finish()
            }
        val alert: AlertDialog = builder.create()
        alert.show()


    }

    protected fun startLocationUpdates() {

        // Create the location request to start receiving updates

        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.setInterval(INTERVAL)
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL)

        // Create LocationSettingsRequest object using location request
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest)
        val locationSettingsRequest = builder.build()

        val settingsClient = LocationServices.getSettingsClient(this)
        settingsClient.checkLocationSettings(locationSettingsRequest)

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {


            return
        }
        mFusedLocationProviderClient!!.requestLocationUpdates(mLocationRequest, mLocationCallback,
            Looper.myLooper())
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            // do work here
            locationResult.lastLocation
            onLocationChanged(locationResult.lastLocation)
        }
    }

    fun onLocationChanged(location: Location) {
        // New location has now been determined
        mLastLocation = location
        val date: Date = Calendar.getInstance().time
        val sdf = SimpleDateFormat("hh:mm:ss a")
        ///txtTime.text = "Updated at : " + sdf.format(date)
        mylat = mLastLocation.latitude
        mylon = mLastLocation.longitude
        val text: TextView = findViewById(R.id.text)



        buildtext = "Lat: " + mylat + " lon: " + mylon + "\n\n"

        val now: Date = Date()
        val suntimes: SunTimes = SunTimes.compute()
            .on(now)       // set a date
            .at(mylat, mylon)   // set a location
            .execute()     // get the results

        val astronomical = SunTimes.compute().twilight(SunTimes.Twilight.ASTRONOMICAL).on(date).at(mylat, mylon).execute()
        val nautical = SunTimes.compute().twilight(SunTimes.Twilight.NAUTICAL).on(date).at(mylat, mylon).execute()
        val civil = SunTimes.compute().twilight(SunTimes.Twilight.CIVIL).on(date).at(mylat, mylon).execute()

        buildtext += "ASTRONOMICAL RISE: " + astronomical.rise + "\n"
        buildtext += "NAUTICAL RISE: " + nautical.rise + "\n"
        buildtext += "CIVIL RISE: " + civil.rise + "\n\n"

        buildtext += "Sunrise: " + suntimes.getRise() + "\n"
        buildtext += "Sunset: " + suntimes.getSet() + "\n\n"


        buildtext += "ASTRONOMICAL SET: " + astronomical.set + "\n"
        buildtext += "NAUTICAL SET: " + nautical.set + "\n"
        buildtext += "CIVIL SET: " + civil.set + "\n\n"


        buildtext += "ASTRONOMICAL: " + SunTimes.compute().twilight(SunTimes.Twilight.ASTRONOMICAL).on(date).at(mylat, mylon).execute() + "\n\n"
        buildtext += "NAUTICAL: " + SunTimes.compute().twilight(SunTimes.Twilight.NAUTICAL).on(date).at(mylat, mylon).execute() + "\n\n"
        buildtext += "CIVIL: " + SunTimes.compute().twilight(SunTimes.Twilight.CIVIL).on(date).at(mylat, mylon).execute() + "\n\n"




        val moontimes: MoonTimes = MoonTimes.compute()
            .on(now)       // set a date
            .at(mylat, mylon)   // set a location
            .execute();     // get the results
        buildtext += "MoonRise: " + moontimes.getRise() + "\n"
        buildtext += "Moonset: " + moontimes.getSet() + "\n"
        //buildtext += "Moon Phase: " + moontimes.

        val illumination = MoonIllumination.compute().on(date).timezone(TimeZone.getDefault()).execute()

        val moonphase = illumination.phase
        val normalized = moonphase + 180.0
        val age = 29.0 * (normalized / 360.0) + 1.0

        buildtext += "Moon Phase: "+illumination.phase+"\n"
        buildtext += "Moon Angle: "+illumination.angle+"\n"
        buildtext += "Moon Fraction: "+illumination.fraction+"\n"
        buildtext += "Moon Age: "+age+"\n"

        buildtext += "\n\n"
        text.setText(buildtext)
    }

    private fun stoplocationUpdates() {
        mFusedLocationProviderClient!!.removeLocationUpdates(mLocationCallback)
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_PERMISSION_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun checkPermissionForLocation(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
                true
            } else {
                // Show the permission request
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_PERMISSION_LOCATION)
                false
            }
        } else {
            true
        }
    }







}
