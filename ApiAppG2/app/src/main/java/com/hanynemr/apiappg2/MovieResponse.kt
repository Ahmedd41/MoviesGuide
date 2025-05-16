package com.hanynemr.apiappg2
data class MovieResponse(
    val items: List<MovieItem>
)

data class MovieItem(
    val title: String,
    val offers: List<Offer>
)

data class Offer(
    val url: String
)

