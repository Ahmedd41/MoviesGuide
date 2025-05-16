package com.hanynemr.apiappg2

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

// هذه هي واجهة API الخاصة بـ JustWatch
interface JustWatchApi {

    // هذا هو الـ GET Request الذي سيقوم بالبحث عن الفيلم باستخدام اسم الفيلم (query)
    @GET("api/v2.0/contents/us/all")
    fun searchMovie(@Query("q") query: String): Call<MovieResponse>
}
