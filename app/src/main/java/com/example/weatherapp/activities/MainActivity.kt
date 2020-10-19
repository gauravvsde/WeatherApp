package com.example.weatherapp.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.example.weatherapp.utils.Constants
import com.example.weatherapp.R
import com.example.weatherapp.models.WeatherResponse
import com.example.weatherapp.network.WeatherService
import com.google.android.gms.location.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_main.*
import retrofit.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    // TODO (Add a variable for FusedLocationProviderClient.)
    // START
    // A fused location client variable which is further used to get the user's current location
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    // END
    // TODO ( Create a global variable for ProgressDialog.)
    // A global variable for the Progress Dialog
    private var mProgressDialog: Dialog? = null

    // TODO ( Make the latitude and longitude variables global to use it in the menu item selection to refresh the data.)
    // START
    // A global variable for Current Latitude
    private var mLatitude: Double = 0.0
    // A global variable for Current Longitude
    private var mLongitude: Double = 0.0
    // END

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // TODO ( Initialize the fusedLocationProviderClient variable.)
        // START
        // Initialize the Fused location variable
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // END

        // TODO (Check here whether GPS is ON or OFF using the method which we have created)
        // START
        if (!isLocationEnabled()) {
            Toast.makeText(
                    this,
                    "Your location provider is turned off. Please turn it on.",
                    Toast.LENGTH_SHORT
            ).show()

            // This will redirect you to settings from where you need to turn on the location provider.
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            // TODO ( Asking the location permission on runtime.)
            // START
            Dexter.withActivity(this)
                    .withPermissions(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    .withListener(object : MultiplePermissionsListener {
                        override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                            if (report!!.areAllPermissionsGranted()) {
                                // TODO (STEP 7: Call the location request function here.)
                                // START
                                requestLocationData()
                                // END
                            }

                            if (report.isAnyPermissionPermanentlyDenied) {
                                Toast.makeText(
                                        this@MainActivity,
                                        "You have denied location permission. Please allow it is mandatory.",
                                        Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        override fun onPermissionRationaleShouldBeShown(
                                permissions: MutableList<PermissionRequest>?,
                                token: PermissionToken?
                        ) {
                            showRationalDialogForPermissions()
                        }
                    }).onSameThread()
                    .check()
        }
        // END
    }

    // TODO ( Now add the override methods to load the menu file and perform the selection on item click.)
    // START
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // TODO ( Now finally, make an api call on item selection.)
            // START
            R.id.action_refresh -> {
                getLocationWeatherDetails()
                true
            }
            else -> super.onOptionsItemSelected(item)
            // END
        }
    }

    /**
     * A function which is used to verify that the location or GPS is enabled or not.
     */
    private fun isLocationEnabled(): Boolean {

        // This provides access to the system location services.
        val locationManager: LocationManager =
                getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
        )
    }


    // TODO ( A alert dialog for denied permissions and if needed to allow it from the settings app info.)
    // START
    /**
     * A function used to show the alert dialog when the permissions are denied and need to allow it from settings app info.
     */
    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
                .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
                .setPositiveButton(
                        "GO TO SETTINGS"
                ) { _, _ ->
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        e.printStackTrace()
                    }
                }
                .setNegativeButton("Cancel") { dialog,
                                               _ ->
                    dialog.dismiss()
                }.show()
    }


    // TODO ( Add a function to get the location of the device using the fusedLocationProviderClient.)
    // START
    /**
     * A function to request the current location. Using the fused location provider client.
     */
    @SuppressLint("MissingPermission")
    private fun requestLocationData() {

        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationClient.requestLocationUpdates(
                mLocationRequest, mLocationCallback,
                Looper.myLooper()
        )
    }
    // END

    // TODO ( Register a request location callback to get the location.)
    // START
    /**
     * A location callback object of fused location provider client where we will get the current location details.
     */
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation
            val latitude = mLastLocation.latitude
            Log.i("Current Latitude", "$latitude")

            val longitude = mLastLocation.longitude
            Log.i("Current Longitude", "$longitude")
            getLocationWeatherDetails()
        }
    }
    // END

    // TODO (Create a function to make an api call using Retrofit Network Library.)
    /**
     * Function is used to get the weather details of the current location based on the latitude longitude
     */
    private fun getLocationWeatherDetails() {

        if (Constants.isNetworkAvailable(this@MainActivity)) {

            // TODO ( Make an api call using retrofit.)
            // START
            /**
             * Add the built-in converter factory first. This prevents overriding its
             * behavior but also ensures correct behavior when using converters that consume all types.
             */
            // here we are using it from the documentation of retrofit.
            val retrofit: Retrofit = Retrofit.Builder()
                    // API base URL.
                    .baseUrl(Constants.BASE_URL)
                    /** Add converter factory for serialization and deserialization of objects. */
                    /**
                     * Create an instance using a default {@link Gson} instance for conversion. Encoding to JSON and
                     * decoding from JSON (when no charset is specified by a header) will use UTF-8.
                     */
                    .addConverterFactory(GsonConverterFactory.create())
                    /** Create the Retrofit instances. */
                    .build()
            // END

            // create a new interface go to network folder then weatherservice.

            // TODO ( Further step for API call)
            // START
            /**
             * Here we map the service interface in which we declares the end point and the API type
             *i.e GET, POST and so on along with the request parameter which are required.
             */
            // creating a WeatherService object which will use our weather service interface.
            // This will be using our retrofit connection and its going to create weather service.
            val service: WeatherService =
                    retrofit.create<WeatherService>(WeatherService::class.java)

            /** An invocation of a Retrofit method that sends a request to a web-server and returns a response.
             * Here we pass the required param in the service
             */
            val listCall: Call<WeatherResponse> = service.getWeather(
                    mLatitude, mLongitude, Constants.METRIC_UNIT, Constants.APP_ID
            )

            showCustomProgressDialog()

            // Callback methods are executed using the Retrofit callback executor.
            listCall.enqueue(object : Callback<WeatherResponse> {
                @RequiresApi(Build.VERSION_CODES.N)
                @SuppressLint("SetTextI18n")
                override fun onResponse(
                        response: Response<WeatherResponse>,
                        retrofit: Retrofit
                ) {

                    // Check weather the response is success or not.
                    if (response.isSuccess) {
                        hideProgressDialog()
                        /** The de-serialized response body of a successful response. */
                        val weatherList: WeatherResponse = response.body()
                        Log.i("Response Result", "$weatherList")

                        setupUI(weatherList)

                    } else {
                        // If the response is not success then we check the response code.
                        val sc = response.code()
                        when (sc) {
                            400 -> {
                                Log.e("Error 400", "Bad Request")
                            }
                            404 -> {
                                Log.e("Error 404", "Not Found")
                            }
                            else -> {
                                Log.e("Error", "Generic Error")
                            }
                        }
                    }
                }

                override fun onFailure(t: Throwable) {
                    hideProgressDialog()
                    Log.e("Errorrrrr", t.message.toString())
                }
            })
            // END

        } else {
            Toast.makeText(
                    this@MainActivity,
                    "No internet connection available.",
                    Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Method is used to show the Custom Progress Dialog.
     */
    private fun showCustomProgressDialog() {
        mProgressDialog = Dialog(this)

        /*Set the screen content from a layout resource.
        The resource will be inflated, adding all top-level views to the screen.*/
        mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)

        //Start the dialog and display it on screen.
        mProgressDialog!!.show()
    }

    /**
     * This function is used to dismiss the progress dialog if it is visible to user.
     */
    private fun hideProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog!!.dismiss()
        }
    }

    // TODO ( We have set the values to the UI and also added some required methods for Unit and Time below.)
    /**
     * Function is used to set the result in the UI elements.
     */
    @RequiresApi(Build.VERSION_CODES.N)
    private fun setupUI(weatherList: WeatherResponse) {

        // For loop to get the required data. And all are populated in the UI.
        for (z in weatherList.weather.indices) {
            Log.i("NAMEEEEEEEE", weatherList.weather[z].main)

            // main is here for weather name.
            tv_main.text = weatherList.weather[z].main
            tv_main_description.text = weatherList.weather[z].description

            // converting into units to display degree.
            tv_temp.text =
                    weatherList.main.temp.toString() + getUnit(application.resources.configuration.locales.toString())
            // display humidity
            tv_humidity.text = weatherList.main.humidity.toString() + " per cent"
            // display min temp.
            tv_min.text = weatherList.main.tempMin.toString() + " min"
            //display max temp.
            tv_max.text = weatherList.main.tempMax.toString() + " max"
            //display wind speed.
            tv_speed.text = weatherList.wind.speed.toString()

            tv_name.text = weatherList.name
            // display country name
            tv_country.text = weatherList.sys.country
            tv_sunrise_time.text = unixTime(weatherList.sys.sunrise.toLong())
            tv_sunset_time.text = unixTime(weatherList.sys.sunset.toLong())

            // Here we update the main icon
            when (weatherList.weather[z].icon) {
                "01d" -> iv_main.setImageResource(R.drawable.sunny)
                "02d" -> iv_main.setImageResource(R.drawable.cloud)
                "03d" -> iv_main.setImageResource(R.drawable.cloud)
                "04d" -> iv_main.setImageResource(R.drawable.cloud)
                "04n" -> iv_main.setImageResource(R.drawable.cloud)
                "10d" -> iv_main.setImageResource(R.drawable.rain)
                "11d" -> iv_main.setImageResource(R.drawable.storm)
                "13d" -> iv_main.setImageResource(R.drawable.snowflake)
                "01n" -> iv_main.setImageResource(R.drawable.cloud)
                "02n" -> iv_main.setImageResource(R.drawable.cloud)
                "03n" -> iv_main.setImageResource(R.drawable.cloud)
                "10n" -> iv_main.setImageResource(R.drawable.cloud)
                "11n" -> iv_main.setImageResource(R.drawable.rain)
                "13n" -> iv_main.setImageResource(R.drawable.snowflake)
            }
        }
    }

    /**
     * Function is used to get the temperature unit value.
     */
    private fun getUnit(value: String): String? {
        Log.i("unitttttt", value)
        var value = "°C"
        if ("US" == value || "LR" == value || "MM" == value) {
            value = "°F"
        }
        return value
    }

    /**
     * The function is used to get the formatted time based on the Format and the LOCALE we pass to it.
     */
    private fun unixTime(timex: Long): String? {
        val date = Date(timex * 1000L)
        @SuppressLint("SimpleDateFormat") val sdf =
                SimpleDateFormat("HH:mm:ss")
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }
    // END

}