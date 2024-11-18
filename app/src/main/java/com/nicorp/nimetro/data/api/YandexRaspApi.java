package com.nicorp.nimetro.data.api;

import com.nicorp.nimetro.data.models.YandexRaspResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface YandexRaspApi {
    @GET("v3.0/search")
    Call<YandexRaspResponse> getSchedule(
            @Query("lang") String lang,
            @Query("format") String format,
            @Query("apikey") String apikey,
            @Query("from") String from,
            @Query("to") String to,
            @Query("system") String system,
            @Query("date") String date,
            @Query("limit") int limit
    );
}