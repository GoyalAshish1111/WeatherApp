package ag.androiddev.weatherapp.models

import java.io.Serializable

data class Sys (
    val type: Int,
    val message: Double,
    val country: String,
    val sunrise: Long,
    val sunset: Long
) : Serializable {
    override fun toString(): String {
        return "Sys(type=$type, message=$message, country='$country', sunrise=$sunrise, sunset=$sunset)"
    }
}
