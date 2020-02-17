package com.divyasri.kotlinweather.Networking

import com.divyasri.kotlinweather.Models.WeatherData
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface MetaWeatherApi {
    @GET("weather")
    fun getLocationDetails(@Query("lat") lat: Double, @Query("lon") lng: Double,
                           @Query("appid")apikey:String): Observable<WeatherData>
}