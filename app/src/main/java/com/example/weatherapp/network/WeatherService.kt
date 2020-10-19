package com.example.weatherapp.network

import com.example.weatherapp.models.WeatherResponse
import retrofit.Call
import retrofit.http.GET
import retrofit.http.Query

interface WeatherService {

    // when we call this function getweather() we want to run a call which will
    // use a weatherResponse as the response of whole call so basically we want to have a weather response
   // object as a result which will be a json object which will contain all the information.
    @GET("2.5/weather")
    fun getWeather(
            @Query("lat") lat: Double,
            @Query("lon") lon: Double,
            @Query("units") units: String?,
            @Query("appid") appid: String?
    ): Call<WeatherResponse>
}