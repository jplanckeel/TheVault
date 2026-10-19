package com.thevault.di

import com.thevault.ui.screen.accessory.AccessoryDetailViewModel
import com.thevault.ui.screen.add.AddViewModel
import com.thevault.ui.screen.additem.AddItemViewModel
import com.thevault.ui.screen.collection.CollectionViewModel
import com.thevault.ui.screen.collection.ItemActions
import com.thevault.ui.screen.collection.PlatformGamesViewModel
import com.thevault.ui.screen.collection.VaultEntriesSource
import com.thevault.ui.screen.console.ConsoleDetailViewModel
import com.thevault.ui.screen.gamedetail.GameDetailViewModel
import com.thevault.ui.screen.home.HomeViewModel
import com.thevault.ui.screen.settings.SettingsViewModel
import com.thevault.ui.screen.stats.StatsViewModel
import com.thevault.ui.screen.wishlist.WishlistViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module providing all common presentation-layer dependencies for the app.
 */
val commonModule =
    module {
        factory { VaultEntriesSource(get(), get(), get()) }
        factory { ItemActions(get(), get(), get(), get(), get(), get()) }

        viewModel { HomeViewModel(get()) }
        viewModel { CollectionViewModel(get()) }
        viewModel { PlatformGamesViewModel(get()) }
        viewModel { WishlistViewModel(get(), get()) }
        viewModel { StatsViewModel(get()) }
        viewModel { AddViewModel(get(), get(), get(), get(), get()) }
        viewModel { SettingsViewModel(get(), get()) }
        viewModel { ConsoleDetailViewModel(get(), get()) }
        viewModel { AccessoryDetailViewModel(get(), get()) }
        viewModel { GameDetailViewModel(get(), get(), get()) }
        viewModel { AddItemViewModel(get(), get(), get()) }
    }
