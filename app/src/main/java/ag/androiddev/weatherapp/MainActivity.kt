package ag.androiddev.weatherapp

import WeatherResponse
import ag.androiddev.weatherapp.databinding.ActivityMainBinding
import ag.androiddev.weatherapp.network.WeatherService
import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.PermissionRequest
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : AppCompatActivity() {

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var mProgressDialog: Dialog? = null

    // View Binding
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize the Fused location variable
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (!isLocationEnabled()) {
            Toast.makeText(
                this,
                "Your location provider is turned off. Please turn it on.",
                Toast.LENGTH_SHORT
            ).show()

            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            Dexter.withActivity(this)
                .withPermissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report!!.areAllPermissionsGranted()) {
                            requestLocationData()
                        }

                        if (report.isAnyPermissionPermanentlyDenied) {
                            Toast.makeText(
                                this@MainActivity,
                                "You have denied location permission. Please allow it is mandatory.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        showRationalDialogForPermissions()
                    }
                }).onSameThread()
                .check()
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setPositiveButton(
                "GO TO SETTINGS"
            ) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog,
                                           _ ->
                dialog.dismiss()
            }.show()
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location? = locationResult.lastLocation
            val latitude = mLastLocation?.latitude
            Log.i("Current Latitude", "$latitude")

            val longitude = mLastLocation?.longitude
            Log.i("Current Longitude", "$longitude")

            if (latitude != null && longitude != null) {
                getLocationWeatherDetails(latitude, longitude)
            }
        }
    }

    private fun getLocationWeatherDetails(latitude: Double, longitude: Double) {
        if (Constants.isNetworkAvailable(this@MainActivity)) {
            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service: WeatherService =
                retrofit.create<WeatherService>(WeatherService::class.java)

            val listCall: Call<WeatherResponse> = service.getWeather(
                latitude, longitude, Constants.METRIC_UNIT, Constants.APP_ID
            )

            showCustomProgressDialog()

            listCall.enqueue(object : Callback<WeatherResponse> {
                @SuppressLint("SetTextI18n")
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    if (response.isSuccessful) {
                        hideProgressDialog()
                        val weatherList: WeatherResponse? = response.body()
                        if (weatherList != null) {
                            setupUI(weatherList)
                        }
                        Log.i("Response Result", "$weatherList")
                    } else {
                        val sc = response.code()
                        when (sc) {
                            400 -> Log.e("Error 400", "Bad Request")
                            404 -> Log.e("Error 404", "Not Found")
                            else -> Log.e("Error", "Generic Error")
                        }
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    hideProgressDialog()
                    Log.e("Errorrrrr", t.message.toString())
                }
            })
        } else {
            Toast.makeText(
                this@MainActivity,
                "No internet connection available.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showCustomProgressDialog() {
        mProgressDialog = Dialog(this)
        mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)
        mProgressDialog!!.show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main ,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.action_refresh->{
                requestLocationData()
                true
            }else -> super.onOptionsItemSelected(item)
        }
    }

    private fun hideProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog!!.dismiss()
        }
    }

    private fun setupUI(weatherList: WeatherResponse) {
        for (i in weatherList.weather.indices) {
            Log.i("Weather Name : ", weatherList.weather.toString())

            // Use View Binding to access views
            binding.tvWeatherCondition.text = weatherList.weather[i].main
            binding.tvTemperature.text = weatherList.main.temp.toString() + getUnit(application.resources.configuration.locales.toString())

            binding.tvSunriseTime.text="Sunrise Time : "+unixTime(weatherList.sys.sunset)+" AM"
            binding.tvSunsetTime.text="Sunset Time : "+unixTime(weatherList.sys.sunrise)+" PM"
            binding.wind.text= "Wind Speed : "+ weatherList.wind.speed.toString() +" Km/hr"
            binding.tvMinMaxTemp.text = "Min: "+weatherList.main.temp_min.toString()+"  |  "+"Max: "+weatherList.main.temp_max.toString()
            binding.tvPlaceName.text= weatherList.name +", "+weatherList.sys.country.toString()
            binding.tvDegreeType.text="Humidity: "+weatherList.main.humidity.toString()+" %  |  "+"Pressure: "+weatherList.main.pressure.toString()

//            when(weatherList.weather[i].icon)
//            {
//                "01d"-> binding.back.setBackgroundResource(R.drawable.sunny)
//                "02d"-> binding.back.setBackgroundResource(R.drawable.few_cloud)
//                "03d"-> binding.back.setBackgroundResource(R.drawable.scattered_cloud)
//                "04d"-> binding.back.setBackgroundResource(R.drawable.broken_cloud)
//                "09d"-> binding.back.setBackgroundResource(R.drawable.shower_rain)
//                "10d"-> binding.back.setBackgroundResource(R.drawable.rain)
//                "11d"-> binding.back.setBackgroundResource(R.drawable.storm)
//                "13d"-> binding.back.setBackgroundResource(R.drawable.snow)
//                "50d"-> binding.back.setBackgroundResource(R.drawable.mist)
//                "01n"-> binding.back.setBackgroundResource(R.drawable.sunny)
//                "02n"-> binding.back.setBackgroundResource(R.drawable.few_cloud)
//                "03n"-> binding.back.setBackgroundResource(R.drawable.scattered_cloud)
//                "04n"-> binding.back.setBackgroundResource(R.drawable.broken_cloud)
//                "09n"-> binding.back.setBackgroundResource(R.drawable.shower_rain)
//                "10n"-> binding.back.setBackgroundResource(R.drawable.rain)
//                "11n"-> binding.back.setBackgroundResource(R.drawable.storm)
//                "13n"-> binding.back.setBackgroundResource(R.drawable.snow)
//                "50n"-> binding.back.setBackgroundResource(R.drawable.mist)
//
//            }
        }
    }

    private fun getUnit(value: String): String {
        return if ("US" == value || "LR" == value || "MM" == value) {
            "°F"
        } else {
            "°C"
        }
    }

    private  fun unixTime(timex:Long):String?{
        val date= Date(timex*1000L)
        val sdf = SimpleDateFormat("HH:mm", Locale.UK)
        sdf.timeZone= TimeZone.getDefault()
        return sdf.format(date)
    }
}

