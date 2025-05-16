package com.hanynemr.apiappg2


import com.google.gson.annotations.SerializedName

data class Meal(
    @SerializedName("category")
    val category: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("image")
    val image: String,
    @SerializedName("itemName")
    val itemName: String,
    @SerializedName("price")
    val price: String,
    @SerializedName("sort")
    val sort: String
)