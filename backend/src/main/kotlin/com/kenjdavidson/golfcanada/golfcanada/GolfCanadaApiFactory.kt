package com.kenjdavidson.golfcanada.golfcanada

import com.kenjdavidson.golfcanada.golfcanada.api.AuthenticationApi
import com.kenjdavidson.golfcanada.golfcanada.api.MembersApi
import com.kenjdavidson.golfcanada.golfcanada.api.ScoresApi
import com.kenjdavidson.golfcanada.golfcanada.client.ApiClient
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton

@Factory
class GolfCanadaApiFactory(
    @param:Value("\${golf-canada.api.base-url:https://scg.golfcanada.ca}") private val baseUrl: String,
) {
    @Singleton
    fun golfCanadaApiClient(): ApiClient = ApiClient().setBasePath(baseUrl)

    @Singleton
    fun authenticationApi(apiClient: ApiClient): AuthenticationApi = AuthenticationApi(apiClient)

    @Singleton
    fun membersApi(apiClient: ApiClient): MembersApi = MembersApi(apiClient)

    @Singleton
    fun scoresApi(apiClient: ApiClient): ScoresApi = ScoresApi(apiClient)
}
