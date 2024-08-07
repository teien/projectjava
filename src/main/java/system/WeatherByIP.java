package system;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class WeatherByIP {
    private static final String IPIFY_API_URL = "https://api.ipify.org?format=text";
    private static final String IPAPI_URL = "http://ip-api.com/json/%s";
    private static final String WEATHER_API_KEY = "14ebc17d2c96b9f29f24fb090287babf";
    private static final String WEATHER_API_URL = "http://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&appid=%s&units=metric";

    private static final OkHttpClient client = new OkHttpClient();
    private static LocationInfo cachedLocation = null;

    public static CompletableFuture<WeatherInfo> getWeatherInfo() {
        if (cachedLocation != null) {
            return getWeatherDetails(cachedLocation.lat(), cachedLocation.lon())
                    .thenApply(weatherInfo -> {
                        weatherInfo.setCity(cachedLocation.city());
                        return weatherInfo;
                    });
        }
        return getPublicIP()
                .thenCompose(WeatherByIP::getLocationFromIP)
                .thenCompose(locationInfo -> {
                    if (!"0".equals(locationInfo.lat()) && !"0".equals(locationInfo.lon())) {
                        cachedLocation = locationInfo;
                    }
                    return getWeatherDetails(locationInfo.lat(), locationInfo.lon())
                            .thenApply(weatherInfo -> {
                                weatherInfo.setCity(locationInfo.city());
                                return weatherInfo;
                            });
                })
                .exceptionally(e -> {
                    System.err.println("Error occurred during fetching weather info: " + e.getMessage());
                    return new WeatherInfo("unknown", 0.0);
                });
    }

    private static CompletableFuture<String> getPublicIP() {
        return CompletableFuture.supplyAsync(() -> {
            Request request = new Request.Builder()
                    .url(IPIFY_API_URL)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                assert response.body() != null;
                return response.body().string();
            } catch (IOException e) {
                throw new RuntimeException("Failed to get public IP", e);
            }
        }).exceptionally(e -> {
            System.err.println("Error occurred during fetching public IP: " + e.getMessage());
            return null;
        });
    }

    private static CompletableFuture<LocationInfo> getLocationFromIP(String ip) {
        return CompletableFuture.supplyAsync(() -> {
            if (ip == null) {
                return new LocationInfo("0", "0", "Unknown");
            }
            try {
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
                        throw new RuntimeException("Failed to fetch location from IP");
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } catch (Exception e) {
                System.err.println("Error retrieving location from IP: " + e.getMessage());
                return new LocationInfo("0", "0", "Unknown");
            }
        }).exceptionally(e -> {
            System.err.println("Error occurred during fetching location from IP: " + e.getMessage());
            return new LocationInfo("0", "0", "Unknown");
        });
    }

    private static CompletableFuture<WeatherInfo> getWeatherDetails(String lat, String lon) {
        return CompletableFuture.supplyAsync(() -> {
            if ("0".equals(lat) && "0".equals(lon)) {
                return new WeatherInfo("unknown", 0.0);
            }
            String url = String.format(WEATHER_API_URL, lat, lon, WEATHER_API_KEY);
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.body() != null) {
                    String jsonResponse = response.body().string();
                    JSONObject json = new JSONObject(jsonResponse);

                    if (!json.has("weather")) {
                        throw new RuntimeException("Weather information not found in response");
                    }

                    JSONArray weatherArray = json.getJSONArray("weather");
                    JSONObject weather = weatherArray.getJSONObject(0);
                    String iconCode = weather.getString("icon");

                    double temp = json.getJSONObject("main").getDouble("temp");
                    return new WeatherInfo(iconCode, temp);
                } else {
                    throw new RuntimeException("Failed to fetch weather details");
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).exceptionally(e -> {
            System.err.println("Error occurred during fetching weather details: " + e.getMessage());
            return new WeatherInfo("unknown", 0.0);
        });
    }

    public static class WeatherInfo {
        private final String iconCode;
        private final double temperature;
        private String city;

        public WeatherInfo(String iconCode, double temperature) {
            this.iconCode = iconCode;
            this.temperature = temperature;
        }

        public String getIconCode() {
            return iconCode;
        }

        public double getTemperature() {
            return (int) Math.ceil(temperature) ;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }
    }

    public record LocationInfo(String lat, String lon, String city) {
    }
}
