package com.divyasri.kotlinweather.Home

import android.content.Context
import com.divyasri.kotlinweather.Models.WeatherData

interface HomeContract {
    interface View {
        fun onDataFetched(weatherData: WeatherData?)

        fun onStoredDataFetched(weatherData: WeatherData?)

        fun onError()

        fun getContext(): Context
    }

    interface Presenter {
        fun subscribe(view: HomeContract.View)

        fun unSubscribe()

        fun refresh(lat: Double, long: Double);
    }
}