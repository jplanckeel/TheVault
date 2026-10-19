package com.thevault.di

import com.thevault.ui.screen.accessory.AccessoryDetailViewModel
import com.thevault.ui.screen.additem.AddItemViewModel
import com.thevault.ui.screen.collection.CollectionViewModel
import com.thevault.ui.screen.collection.WishlistViewModel
import com.thevault.ui.screen.console.ConsoleDetailViewModel
import com.thevault.ui.screen.dashboard.DashboardViewModel
import com.thevault.ui.screen.gamedetail.GameDetailViewModel
import com.thevault.ui.screen.settings.SettingsViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module providing all common presentation-layer dependencies for the app.
 */
val commonModule =
    module {
        viewModel { DashboardViewModel(get(), get(), get(), get()) }
        viewModel { CollectionViewModel(get(), get(), get()) }
        viewModel { WishlistViewModel(get(), get(), get()) }
        viewModel { SettingsViewModel(get(), get()) }
        viewModel { ConsoleDetailViewModel(get()) }
        viewModel { AccessoryDetailViewModel(get()) }
        viewModel { GameDetailViewModel(get(), get()) }
        viewModel { AddItemViewModel(get(), get(), get()) }
    }
