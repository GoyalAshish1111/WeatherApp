package ag.androiddev.weatherapp.models

import java.io.Serializable

data class Wind (
    val speed: Double,
    val deg: Int
) : Serializable {
    override fun toString(): String {
        return "Wind(speed=$speed, deg=$deg)"
    }
}
