package com.divyasri.kotlinweather.Home

//import com.divyasri.kotlinweather.Dialogs.ForecastDialogFragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import android.widget.Toast
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.divyasri.kotlinweather.Models.WeatherData
import com.divyasri.kotlinweather.R
import com.divyasri.kotlinweather.utils.WeatherWorker
import java.util.concurrent.TimeUnit


class HomeActivity : AppCompatActivity(), HomeContract.View {

    private val RC_ENABLE_LOCATION = 1
    private val RC_LOCATION_PERMISSION = 2
    private val TAG_FORECAST_DIALOG = "forecast_dialog"
    var mPresenter: HomeContract.Presenter? = null
    var mLocationManager: LocationManager? = null
    var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    var mLocation: Location? = null

    /*
    * Refresh the weather location on location changed
    * */
    var mLocationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location?) {
            mSwipeRefreshLayout?.isRefreshing = true
            mPresenter?.refresh(location?.latitude ?: 0.0, location?.longitude ?: 0.0)

            //Check if the location is not null
            //Remove the location listener as we don't need to fetch the weather again and again
            if (location?.latitude != null && location.latitude != 0.0 && location.longitude != 0.0) {
                mLocation = location
                mLocationManager?.removeUpdates(this)
            }
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        }

        override fun onProviderEnabled(provider: String?) {
        }

        override fun onProviderDisabled(provider: String?) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout) as SwipeRefreshLayout
        mLocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        mPresenter = HomePresenter()
        mPresenter?.subscribe(this)

        initViews()

        if (checkAndAskForLocationPermissions()) {
            checkGpsEnabledAndPrompt()
        }
//Check if connected to WIFI and call Worker to schedule in every 2 hours
        if (isConnectedToWifi(this)) {
            val myWorkRequest =
                PeriodicWorkRequest.Builder(WeatherWorker::class.java, 120, TimeUnit.MINUTES)
                    .build()
            WorkManager.getInstance().enqueue(myWorkRequest)
        }
    }

    fun isConnectedToWifi(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities =
                connectivityManager.getNetworkCapabilities(network)
                    ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
//            For the Below M versions
            val networkInfo =
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                    ?: return false
            networkInfo.isConnected
        }
    }

    private fun initViews() {
        mSwipeRefreshLayout?.setOnRefreshListener {
            if (mLocation != null) {
                mPresenter?.refresh(mLocation?.latitude ?: 0.0, mLocation?.longitude ?: 0.0)
            } else {
                mSwipeRefreshLayout?.isRefreshing = false
            }
        }
    }

    override fun getContext() = this

    override fun onStoredDataFetched(weatherData: WeatherData?) {
        updateUI(weatherData)
    }

    override fun onDataFetched(weatherData: WeatherData?) {
        //Stop the swipe refresh layout
        mSwipeRefreshLayout?.isRefreshing = false
        updateUI(weatherData)
    }

    /*
    * Update the UI when the weather data is fetched from the network
    * */
    private fun updateUI(weatherData: WeatherData?) {
        val temperatureTextView = findViewById(R.id.temperature_text_view) as TextView
        val windSpeedTextView = findViewById(R.id.wind_speed_text_view) as TextView
        val humidityTextView = findViewById(R.id.humidity_text_view) as TextView
        val weatherConditionTextView = findViewById(R.id.weather_condition_text_view) as TextView
        val cityNameTextView = findViewById(R.id.city_name_text_view) as TextView
        temperatureTextView.text = "Temp : " + weatherData?.main?.temp.toString()
        windSpeedTextView.text = "Wind speed : " + weatherData?.wind?.speed.toString()
        humidityTextView.text = "Humidity : " + weatherData?.main?.humidity.toString()
        weatherConditionTextView.text =
            "Weather Today : " + weatherData?.weather?.get(0)?.description.toString()
        cityNameTextView.text =
            "Place : " + weatherData?.name + " " + weatherData?.coord?.lat + " " + weatherData?.coord?.lon


    }

    override fun onError() {
        mSwipeRefreshLayout?.isRefreshing = false


        Toast.makeText(this, "Something went Wrong", Toast.LENGTH_LONG)
    }

    override fun onDestroy() {
        super.onDestroy()
        mPresenter?.unSubscribe()
        mLocationManager?.removeUpdates(mLocationListener)
    }

    /*
    * Check if the gps is enabled by the user
    * if not then prompt to enable gps
    * else start fetching the location
    * */
    private fun checkGpsEnabledAndPrompt() {
        //Check if the gps is enabled
        val isLocationEnabled =
            mLocationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isLocationEnabled) {
            //Show alert dialog to enable gps
            AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle("GPS is not enabled")
                .setMessage("This app required GPS to get the weather information. Do you want to enable GPS?")
                .setPositiveButton(android.R.string.ok, { dialog, which ->
                    //Start settings to enable location
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivityForResult(intent, RC_ENABLE_LOCATION)

                    dialog.dismiss()
                })
                .setNegativeButton(android.R.string.cancel, { dialog, which ->
                    dialog.dismiss()
                })
                .create()
                .show()
        } else {
            requestLocationUpdates()
        }
    }

    /*
    * Start receiving the location updates
    * */
    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        val provider = LocationManager.NETWORK_PROVIDER

        //Add the location listener and request updated
        mLocationManager?.requestLocationUpdates(provider, 0, 0.0f, mLocationListener)

        val location = mLocationManager?.getLastKnownLocation(provider)
        mLocationListener.onLocationChanged(location)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            RC_ENABLE_LOCATION -> {
                checkGpsEnabledAndPrompt()
            }
        }
    }

    /*
    * Check if location permission have been granted by the user
    * */
    private fun checkAndAskForLocationPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    RC_LOCATION_PERMISSION
                )
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            RC_LOCATION_PERMISSION -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkGpsEnabledAndPrompt()
                } else {
                    checkAndAskForLocationPermissions()
                }
            }
        }
    }
}
