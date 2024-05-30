import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import org.json.JSONArray;

public class WeatherByIP {
    private static final String IPIFY_API_URL = "https://api.ipify.org?format=text";
    private static final String IPAPI_URL = "http://ip-api.com/json/%s";
    private static final String WEATHER_API_KEY = "14ebc17d2c96b9f29f24fb090287babf";
    private static final String WEATHER_API_URL = "http://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&appid=%s&units=metric";

    private static final OkHttpClient client = new OkHttpClient();

    public static void getWeatherInfo() throws Exception {
        try {
            String ip = getPublicIP();
            LocationInfo locationInfo = getLocationFromIP(ip);
            String lat = locationInfo.lat();
            String lon = locationInfo.lon();
            WeatherInfo weatherInfo = getWeatherDetails(lat, lon);
            weatherInfo.setCity(locationInfo.city());
        } catch (Exception e) {
            throw new Exception("Error while getting weather information: " + e.getMessage(), e);
        }
    }

    private static String getPublicIP() throws Exception {
        Request request = new Request.Builder()
                .url(IPIFY_API_URL)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.body() != null) {
                return response.body().string();
            } else {
                throw new Exception("Failed to fetch public IP");
            }
        }
    }

    private static LocationInfo getLocationFromIP(String ip) throws Exception {
        String url = String.format(IPAPI_URL, ip);
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.body() != null) {
                String jsonResponse = response.body().string();
                JSONObject json = new JSONObject(jsonResponse);
                double lat = json.getDouble("lat");
                double lon = json.getDouble("lon");
                String city = json.getString("city");
                return new LocationInfo(String.valueOf(lat), String.valueOf(lon), city);
            } else {
                throw new Exception("Failed to fetch location from IP");
            }
        }
    }

    private static WeatherInfo getWeatherDetails(String lat, String lon) throws Exception {
        String url = String.format(WEATHER_API_URL, lat, lon, WEATHER_API_KEY);
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.body() != null) {
                String jsonResponse = response.body().string();
                JSONObject json = new JSONObject(jsonResponse);

                JSONArray weatherArray = json.getJSONArray("weather");
                JSONObject weather = weatherArray.getJSONObject(0);
                String iconCode = weather.getString("icon");

                double temp = json.getJSONObject("main").getDouble("temp");

                return new WeatherInfo(iconCode, temp);
            } else {
                throw new Exception("Failed to fetch weather details");
            }
        }
    }

    public static class WeatherInfo {
        private static String iconCode;
        private static double temperature;
        private static String city;

        public WeatherInfo(String iconCode, double temperature) {
            WeatherInfo.iconCode = iconCode;
            WeatherInfo.temperature = temperature;
        }

        public static String getIconCode() {
            return iconCode;
        }

        public static double getTemperature() {
            return temperature;
        }

        public static String getCity() {
            return city;
        }

        public void setCity(String city) {
            WeatherInfo.city = city;
        }
    }
    public record LocationInfo(String lat, String lon, String city) {
    }
}