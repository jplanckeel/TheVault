package com.thevault.di

import com.thevault.ui.screen.dashboard.DashboardViewModel
import com.thevault.ui.screen.collection.CollectionViewModel
import com.thevault.ui.screen.console.ConsoleViewModel
import com.thevault.ui.screen.console.ConsoleDetailViewModel
import com.thevault.ui.screen.accessory.AccessoryViewModel
import com.thevault.ui.screen.accessory.AccessoryDetailViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module providing all common presentation-layer dependencies for the app.
 */
val commonModule =
    module {
        viewModel { DashboardViewModel(get(), get(), get(), get()) }
        viewModel { CollectionViewModel(get(), get(), get()) }
        viewModel { ConsoleViewModel(get()) }
        viewModel { ConsoleDetailViewModel(get()) }
        viewModel { AccessoryViewModel(get()) }
        viewModel { AccessoryDetailViewModel(get()) }
        viewModel { com.thevault.ui.screen.gamedetail.GameDetailViewModel(get(), get()) }
        viewModel { com.thevault.ui.screen.additem.AddItemViewModel(get(), get(), get()) }
    }
