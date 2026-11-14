package com.thevault.domain

import com.thevault.domain.model.Accessory
import com.thevault.domain.model.Account
import com.thevault.domain.model.Console
import com.thevault.domain.model.Game
import com.thevault.domain.model.ItemSet
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.SetEntry
import com.thevault.domain.model.ShareLink
import com.thevault.domain.model.ShareScope
import com.thevault.domain.model.SyncOutcome
import com.thevault.domain.model.SyncPayload
import com.thevault.domain.model.VaultContent
import com.thevault.domain.repository.AccessoryRepository
import com.thevault.domain.repository.CatalogSyncRepository
import com.thevault.domain.repository.ConsoleRepository
import com.thevault.domain.repository.GameRepository
import com.thevault.domain.repository.SetRepository
import com.thevault.domain.repository.VaultRepository
import com.thevault.domain.usecase.SyncScheduler
import com.thevault.domain.usecase.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/** In-memory repositories for the use case tests. */
class FakeGames : GameRepository {
    val items = MutableStateFlow<Map<String, Game>>(emptyMap())

    override fun observeAll(): Flow<List<Game>> = items.map { it.values.toList() }

    override suspend fun getById(id: String) = items.value[id]

    override fun observeByStatus(status: ItemStatus) = items.map { m -> m.values.filter { it.status == status } }

    override suspend fun save(game: Game) {
        items.value = items.value + (game.id to game)
    }

    override suspend fun delete(id: String) {
        items.value = items.value - id
    }

    override fun search(query: String) = items.map { m -> m.values.filter { it.title.contains(query, ignoreCase = true) } }
}

/** In-memory consoles. */
class FakeConsoles : ConsoleRepository {
    val items = MutableStateFlow<Map<String, Console>>(emptyMap())

    override fun observeAll(): Flow<List<Console>> = items.map { it.values.toList() }

    override suspend fun getById(id: String) = items.value[id]

    override fun observeByStatus(status: ItemStatus) = items.map { m -> m.values.filter { it.status == status } }

    override suspend fun save(console: Console) {
        items.value = items.value + (console.id to console)
    }

    override suspend fun delete(id: String) {
        items.value = items.value - id
    }
}

/** In-memory accessories. */
class FakeAccessories : AccessoryRepository {
    val items = MutableStateFlow<Map<String, Accessory>>(emptyMap())

    override fun observeAll(): Flow<List<Accessory>> = items.map { it.values.toList() }

    override suspend fun getById(id: String) = items.value[id]

    override fun observeByStatus(status: ItemStatus) = items.map { m -> m.values.filter { it.status == status } }

    override suspend fun save(accessory: Accessory) {
        items.value = items.value + (accessory.id to accessory)
    }

    override suspend fun delete(id: String) {
        items.value = items.value - id
    }
}

/** In-memory sets. */
class FakeSets : SetRepository {
    val sets = MutableStateFlow<Map<String, ItemSet>>(emptyMap())

    override fun observe() = sets.map { it.values.toList() }

    override suspend fun save(set: ItemSet) {
        sets.value = sets.value + (set.id to set)
    }

    override suspend fun delete(id: String) {
        sets.value = sets.value - id
    }

    override suspend fun addEntry(
        setId: String,
        entry: SetEntry,
    ) {
        sets.value[setId]?.let { save(it.copy(entries = it.entries + entry)) }
    }

    override suspend fun removeEntry(
        setId: String,
        entry: SetEntry,
    ) {
        sets.value[setId]?.let { save(it.copy(entries = it.entries - entry)) }
    }
}

/** Sync queue answering with [outcome]. */
class FakeSync(
    var outcome: SyncOutcome = SyncOutcome.Synced,
) : CatalogSyncRepository {
    val synced = MutableStateFlow<Set<String>>(emptySet())
    val pushed = mutableListOf<String>()
    val dirty = mutableListOf<String>()

    override fun observeSyncedIds() = synced

    override suspend fun markDirty(itemId: String) {
        dirty += itemId
        synced.value = synced.value - itemId
    }

    override suspend fun push(payload: SyncPayload): SyncOutcome {
        pushed += payload.externalId
        if (outcome != SyncOutcome.Failed) synced.value = synced.value + payload.externalId
        return outcome
    }
}

/** Scheduler counting the requests. */
class FakeScheduler : SyncScheduler {
    var requests = 0

    override val status: StateFlow<SyncStatus> = MutableStateFlow(SyncStatus())

    override fun request() {
        requests++
    }
}

/** Vault holding one backup in memory. */
class FakeVault : VaultRepository {
    var stored: VaultContent? = null

    override fun observeAccount() = MutableStateFlow<Account?>(Account("a@b.c", "A"))

    override suspend fun login(
        email: String,
        password: String,
    ) = Result.success(Account(email, ""))

    override suspend fun logout() = Unit

    override suspend fun upload(content: VaultContent): Result<String> {
        stored = content
        return Result.success("now")
    }

    override suspend fun download(): Result<VaultContent> =
        stored?.let { Result.success(it) } ?: Result.failure(IllegalStateException("none"))

    override suspend fun shares() = Result.success(emptyList<ShareLink>())

    override suspend fun createShare(
        scope: ShareScope,
        title: String,
        hidePrices: Boolean,
    ) = Result.success(ShareLink("t", scope, title, hidePrices, "u/t"))

    override suspend fun revokeShare(token: String) = Result.success(Unit)
}
