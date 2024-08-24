package com.example.myapplication;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {
    @POST("0bbf4a85-138c-4629-8655-4bc5f9b9d1aa")
    Call<ResponseBody> submitForm(@Body FormData formData);
}