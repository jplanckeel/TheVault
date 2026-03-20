package com.thevault.di

import com.thevault.domain.usecase.DeleteGameUseCase
import com.thevault.domain.usecase.GetGameByIdUseCase
import com.thevault.domain.usecase.ObserveGamesUseCase
import com.thevault.domain.usecase.ObserveVaultStatsUseCase
import com.thevault.domain.usecase.SaveGameUseCase
import com.thevault.domain.usecase.SearchGamesUseCase
import org.koin.dsl.module
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

val sharedModule =
    module {

        // ─── Network ──────────────────────────────────────────────────────────────
        single {
            HttpClient {
                install(ContentNegotiation) {
                    json(Json { 
                        ignoreUnknownKeys = true 
                        coerceInputValues = true
                    })
                }
            }
        }

        single<com.thevault.data.remote.GameRemoteDataSource> {
            val rawgFallback = com.thevault.data.remote.RawgRemoteDataSource(get())
            com.thevault.data.remote.IgdbRemoteDataSource(
                httpClient = get(),
                fallbackDataSource = rawgFallback,
                clientId = "lt0oga4m3242i4zbv981lqplfpm6u8",
                clientSecret = "48pek7ag0j71bjz37p5dnpyjbyj45r"
            )
        }

        // ─── Use Cases ────────────────────────────────────────────────────────────
        factory { ObserveGamesUseCase(get()) }
        factory { GetGameByIdUseCase(get()) }
        factory { SaveGameUseCase(get()) }
        factory { DeleteGameUseCase(get()) }
        factory { ObserveVaultStatsUseCase(get()) }
        factory { SearchGamesUseCase(get()) }
        factory { com.thevault.domain.usecase.SearchRemoteGamesUseCase(get()) }
        factory { com.thevault.domain.usecase.ObserveConsolesUseCase(get()) }
        factory { com.thevault.domain.usecase.GetConsoleByIdUseCase(get()) }
        factory { com.thevault.domain.usecase.ObserveAccessoriesUseCase(get()) }
        factory { com.thevault.domain.usecase.GetAccessoryByIdUseCase(get()) }
        factory { com.thevault.domain.usecase.CsvService(get(), get(), get()) }
        factory { com.thevault.domain.usecase.SyncVaultWithIgdbUseCase(get(), get()) }

        // ─── Repositories ──────────────────────────────────────────────────────────
        single<com.thevault.domain.repository.VaultStatsRepository> {
            com.thevault.data.repository.VaultStatsRepositoryImpl(get())
        }
        single<com.thevault.domain.repository.GameRepository> {
            com.thevault.data.repository.GameRepositoryImpl(get())
        }
        single<com.thevault.domain.repository.ConsoleRepository> {
            com.thevault.data.repository.ConsoleRepositoryImpl(get())
        }
        single<com.thevault.domain.repository.AccessoryRepository> {
            com.thevault.data.repository.AccessoryRepositoryImpl(get())
        }
    }
