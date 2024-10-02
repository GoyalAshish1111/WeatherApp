package ag.androiddev.weatherapp.models

import java.io.Serializable

data class Coord (
    val lon: Double,
    val lat: Double
) : Serializable {
    override fun toString(): String {
        return "Coord(lon=$lon, lat=$lat)"
    }
}
