import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("main") val main: WeatherMain,
    @SerializedName("weather") val weather: List<Weather>,
    // Add other fields as needed
)

data class WeatherMain(
    @SerializedName("temp") val temperature: Double,
    // Add other fields as needed
)

data class Weather(
    @SerializedName("main") val main: String,
    @SerializedName("description") val description: String,
    // Add other fields as needed
)
