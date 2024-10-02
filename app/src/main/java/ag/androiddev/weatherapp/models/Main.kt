package ag.androiddev.weatherapp.models

import java.io.Serializable
data class Main (
    val temp: Double,
    val pressure: Double,
    val humidity: Double,
    val temp_min: Double,
    val temp_max: Double,
    val sea_level: Double?,
    val gmd_level: Double?
) : Serializable {
    override fun toString(): String {
        return "Main(temp=$temp, pressure=$pressure, humidity=$humidity, temp_min=$temp_min, temp_max=$temp_max, sea_level=$sea_level, gmd_level=$gmd_level)"
    }
}
