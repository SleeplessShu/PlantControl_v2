package com.practicum.plantcontrol

import retrofit2.Call
import retrofit2.http.GET

interface ESP32Api {
    @GET("/")
    fun getStatus(): Call<String>
}