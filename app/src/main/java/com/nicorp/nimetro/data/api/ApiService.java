package com.nicorp.nimetro.data.api;

import com.nicorp.nimetro.data.api.dto.ApiMapData;
import com.nicorp.nimetro.data.api.dto.ApiMapItem;
import com.nicorp.nimetro.data.api.dto.ApiNotification;

import java.util.List;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    
    @GET("maps")
    Call<List<ApiMapItem>> getMaps();
    
    @GET("maps/{id}")
    Call<ApiMapData> getMapById(@Path("id") UUID id);
    
    @GET("maps/by-name/{fileName}")
    Call<ApiMapData> getMapByFileName(@Path("fileName") String fileName);
    
    @GET("notifications")
    Call<List<ApiNotification>> getNotifications(
        @Query("stationId") String stationId,
        @Query("lineId") String lineId
    );
}

