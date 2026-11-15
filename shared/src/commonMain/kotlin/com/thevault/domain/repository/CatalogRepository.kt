package com.thevault.domain.repository

import com.thevault.domain.model.CatalogSheet

/** Shared catalogue of the backend. */
interface CatalogRepository {
    /**
     * Looks up the catalogue sheets of [externalIds].
     *
     * @return Sheets by external id; items unknown to the backend are absent.
     */
    suspend fun lookup(externalIds: List<String>): Map<String, CatalogSheet>
}
