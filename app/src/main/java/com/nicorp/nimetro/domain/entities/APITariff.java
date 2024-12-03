package com.nicorp.nimetro.domain.entities;

import android.util.Log;

import com.google.gson.JsonObject;
import com.nicorp.nimetro.data.api.YandexRaspApi;
import com.nicorp.nimetro.data.models.YandexRaspResponse;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class APITariff implements Tariff {

    @Override
    public void calculateCost(RouteSegment segment, TariffCallback callback) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.rasp.yandex.net/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        YandexRaspApi yandexRaspApi = retrofit.create(YandexRaspApi.class);
        Call<YandexRaspResponse> call = yandexRaspApi.getSchedule("ru_RU", "json", "e4d3d8fe-a921-4206-8048-8c7217648728", segment.getStations().get(0).getESP(), segment.getStations().get(segment.getStations().size() - 1).getESP(), "esr", "2024-11-24", 1000);

        final int[] price = {0};

        call.enqueue(new Callback<YandexRaspResponse>() {
            @Override
            public void onResponse(Call<YandexRaspResponse> call, Response<YandexRaspResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    YandexRaspResponse yandexRaspResponse = response.body();
                    if (yandexRaspResponse != null) {
                        for (YandexRaspResponse.Segment segment : yandexRaspResponse.getSegments()) {
                            if (segment.getTicketsInfo() != null && !segment.getTicketsInfo().getPlaces().isEmpty()) {
                                double price = segment.getTicketsInfo().getPlaces().get(0).getPrice().getWhole();
                                Log.d("APITariff", "Price: " + price);
                                callback.onCostCalculated(price);
                                return;
                            }
                        }
                    } else {
                        Log.d("APITariff", "Response is null");
                    }
                } else {
                    Log.d("APITariff", "Error: " + response.message());
                }
                callback.onCostCalculated(0.0);
            }

            @Override
            public void onFailure(Call<YandexRaspResponse> call, Throwable t) {
                Log.d("APITariff", "Error: " + t.getMessage());
                callback.onCostCalculated(0.0);
            }
        });
    }

    @Override
    public JsonObject getJson() {
        // Convert APITariff to JsonObject and return it
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "APITariff");
        return jsonObject;

    }

    @Override
    public String getName() {
        return "APITariff";
    }

}