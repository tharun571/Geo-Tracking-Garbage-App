package com.example.geogarbagetracker

data class GarbageLocation(
    var lon: Double = 0.0,
    var lat: Double = 0.0,
    var alt: Double = 0.0,
    var title: String = "",
    var url: String = ""
)
