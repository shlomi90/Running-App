import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherMapService {
    @GET("weather")
    fun getWeather(
        @Query("q") location: String,
        @Query("appid") apiKey: String
    ): Call<WeatherResponse>
}
