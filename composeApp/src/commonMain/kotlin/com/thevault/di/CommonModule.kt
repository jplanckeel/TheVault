package com.thevault.di

import com.thevault.ui.app.AppViewModel
import com.thevault.ui.screen.add.AddViewModel
import com.thevault.ui.screen.additem.AddItemViewModel
import com.thevault.ui.screen.collection.CollectionViewModel
import com.thevault.ui.screen.collection.ItemActions
import com.thevault.ui.screen.collection.PlatformGamesViewModel
import com.thevault.ui.screen.collection.VaultEntriesSource
import com.thevault.ui.screen.gamedetail.GameDetailViewModel
import com.thevault.ui.screen.home.HomeViewModel
import com.thevault.ui.screen.item.ItemDetailViewModel
import com.thevault.ui.screen.item.ItemEditViewModel
import com.thevault.ui.screen.profile.ProfileViewModel
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
        factory { VaultEntriesSource(get(), get(), get(), get(), get(), get()) }
        factory {
            com.thevault.ui.screen.collection
                .ItemPhotos(get(), get(), get())
        }
        factory { ItemActions(get(), get(), get(), get(), get(), get(), get(), get(), get()) }

        viewModel { AppViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
        viewModel { HomeViewModel(get(), get(), get(), get()) }
        viewModel {
            com.thevault.ui.screen.report
                .QuoteReportViewModel(get(), get())
        }
        viewModel { CollectionViewModel(get()) }
        viewModel { PlatformGamesViewModel(get(), get()) }
        viewModel { WishlistViewModel(get()) }
        viewModel { ProfileViewModel(get()) }
        viewModel {
            com.thevault.ui.screen.cloud
                .CloudViewModel(get(), get(), get(), get(), get(), get(), get(), get())
        }
        viewModel {
            com.thevault.ui.screen.check
                .QuickCheckViewModel(get())
        }
        viewModel {
            com.thevault.ui.screen.sets
                .SetsViewModel(get(), get(), get())
        }
        viewModel {
            com.thevault.ui.screen.sets
                .SetDetailViewModel(get(), get(), get(), get(), get(), get(), get())
        }
        viewModel { StatsViewModel(get()) }
        viewModel { AddViewModel(get(), get(), get(), get(), get()) }
        viewModel { SettingsViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
        viewModel { ItemDetailViewModel(get(), get(), get()) }
        viewModel { ItemEditViewModel(get(), get(), get(), get()) }
        viewModel { GameDetailViewModel(get(), get(), get(), get(), get(), get(), get()) }
        viewModel { AddItemViewModel(get(), get(), get()) }
    }
