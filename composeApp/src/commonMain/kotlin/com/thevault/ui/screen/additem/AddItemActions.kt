package com.thevault.ui.screen.additem

import com.thevault.domain.model.ItemCondition
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.Region
import com.thevault.domain.model.RemoteGame

/**
 * Callbacks of the add / edit form.
 *
 * @property onBack Leaves the form.
 * @property onTitleChange Title typed (triggers the IGDB search).
 * @property onSelectRemote IGDB result picked.
 * @property onPlatformChange Platform changed.
 * @property onStatusChange Ownership status changed.
 * @property onConditionChange Condition changed.
 * @property onRegionChange Region changed.
 * @property onPriceChange Purchase price typed.
 * @property onValueChange Estimated value typed.
 * @property onRatingChange Personal rating changed.
 * @property onNotesChange Notes typed.
 * @property onSave Saves the item.
 */
data class AddItemActions(
    val onBack: () -> Unit = {},
    val onTitleChange: (String) -> Unit = {},
    val onSelectRemote: (RemoteGame) -> Unit = {},
    val onPlatformChange: (String) -> Unit = {},
    val onStatusChange: (ItemStatus) -> Unit = {},
    val onConditionChange: (ItemCondition) -> Unit = {},
    val onRegionChange: (Region) -> Unit = {},
    val onPriceChange: (String) -> Unit = {},
    val onValueChange: (String) -> Unit = {},
    val onRatingChange: (Int) -> Unit = {},
    val onNotesChange: (String) -> Unit = {},
    val onSave: () -> Unit = {},
)
