package com.divyasri.kotlinweather.utils

import android.content.Context
import android.widget.Toast
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.divyasri.kotlinweather.Consts.Constants
import com.divyasri.kotlinweather.Models.WeatherData
import com.divyasri.kotlinweather.Networking.NetworkService
import com.google.gson.Gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File

public class WeatherWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    val context = ctx
    lateinit var lat: String
    lateinit var longi: String

    override fun doWork(): Result {
        lat=Cache.get(context,Constants.LATITUDE)
        longi=Cache.get(context,Constants.LONGITUDE)
        refresh(lat.toDouble() ,longi.toDouble())
        return Result.success()
    }

    fun refresh(lat: Double, long: Double) {
        NetworkService.getMetaWeatherApi()
            .getLocationDetails(lat, long, Constants.API_KEY)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ weatherData ->

                storeFileToExternalStorage(weatherData, context)
            }, { error ->
                Toast.makeText(
                    applicationContext,
                    "Something wrong while fetching...",
                    Toast.LENGTH_LONG
                )
            })
    }

    /*
  * Save the data in internal file as a json structure
  */
    private fun storeFileToExternalStorage(weatherData: WeatherData, context: Context?) {
        val gson = Gson()
        val weatherJson = gson.toJson(weatherData)

        val weatherFile = File(context?.filesDir, Constants.WEATHER_FILE_NAME)
        if (weatherFile.exists()) weatherFile.delete()
        weatherFile.createNewFile()

        val outputStream =
            context?.openFileOutput(Constants.WEATHER_FILE_NAME, Context.MODE_PRIVATE)
        outputStream?.write(weatherJson.toByteArray())
        outputStream?.close()
    }

}