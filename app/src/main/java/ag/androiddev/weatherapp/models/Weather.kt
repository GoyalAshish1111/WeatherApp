package ag.androiddev.weatherapp.models

import java.io.Serializable

data class Weather (
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
) : Serializable {
    override fun toString(): String {
        return "Weather(id=$id, main='$main', description='$description', icon='$icon')"
    }
}
