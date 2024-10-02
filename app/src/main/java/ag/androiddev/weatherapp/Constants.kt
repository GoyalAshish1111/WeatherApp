package ag.androiddev.weatherapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build

object Constants{

    const val APP_ID:String = "paste_your_api_key_here"
    const val BASE_URL:String = "https://api.openweathermap.org/data/"
    const val METRIC_UNIT:String = "metric"

    fun isNetworkAvailable(context: Context) :Boolean{
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            val network = connectivityManager.activeNetwork ?: return false
            val activenetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                activenetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)->  true
                activenetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)->  true
                activenetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)->  true
                else -> false
            }
        }
        else
        {
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo !=null && networkInfo.isConnectedOrConnecting
        }

    }
}