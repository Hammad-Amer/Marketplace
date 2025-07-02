package com.shayaankhalid.marketplace

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface NotificationInterface {
    @POST("/v1/projects/marketplace-7143e/messages:send")
    @Headers(
        "Content-Type: application/json",
        "Accept: application/json"
    )
    fun sendNotification(
        @Body message:Notification,
        @Header("Authorization") accessToken: String = "Bearer ${AccessToken.getAccessToken()}"

    ): Call<Notification>
}