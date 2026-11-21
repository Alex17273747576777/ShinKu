package com.shinku.reader.ui.browse

import androidx.compose.runtime.Immutable
import com.shinku.reader.domain.manga.model.Manga

@Immutable
data class BrowseEntryKey(
    val sourceId: Long,
    val url: String,
) {
    companion object {
        fun of(manga: Manga): BrowseEntryKey = BrowseEntryKey(
            sourceId = manga.source,
            url = manga.url,
        )
    }
}

@Immutable
data class BrowseSelectionState(
    val selectionMode: Boolean = false,
    val selectedEntries: Map<BrowseEntryKey, Manga> = emptyMap(),
) {
    val selectedCount: Int
        get() = selectedEntries.size

    val selectedManga: List<Manga>
        get() = selectedEntries.values.toList()

    fun isSelected(manga: Manga): Boolean = BrowseEntryKey.of(manga) in selectedEntries

    fun enterSelectionMode(): BrowseSelectionState = copy(selectionMode = true)

    fun exitSelectionMode(): BrowseSelectionState = BrowseSelectionState()

    fun toggle(manga: Manga): BrowseSelectionState {
        val key = BrowseEntryKey.of(manga)
        val updated = if (key in selectedEntries) {
            selectedEntries - key
        } else {
            selectedEntries + (key to manga)
        }
        return copy(selectionMode = true, selectedEntries = updated)
    }

    fun selectAllLoaded(loaded: List<Manga>): BrowseSelectionState {
        if (loaded.isEmpty()) return copy(selectionMode = true)
        val updated = selectedEntries.toMutableMap()
        for (manga in loaded) {
            updated[BrowseEntryKey.of(manga)] = manga
        }
        return copy(selectionMode = true, selectedEntries = updated)
    }

    fun invertLoaded(loaded: List<Manga>): BrowseSelectionState {
        if (loaded.isEmpty()) return copy(selectionMode = true)
        val updated = selectedEntries.toMutableMap()
        for (manga in loaded) {
            val key = BrowseEntryKey.of(manga)
            if (key in updated) {
                updated.remove(key)
            } else {
                updated[key] = manga
            }
        }
        return copy(selectionMode = true, selectedEntries = updated)
    }

    fun clear(): BrowseSelectionState = copy(selectedEntries = emptyMap())
}

@Immutable
data class BulkFavoriteSummary(
    val added: Int,
    val skippedAlreadyFavorite: Int,
    val skippedDuplicates: Int,
    val failed: Int,
) {
    val hasFailures: Boolean
        get() = failed > 0

    fun formatMessage(): String {
        val parts = buildList {
            add("Added $added")
            if (skippedAlreadyFavorite > 0) add("$skippedAlreadyFavorite already in library")
            if (skippedDuplicates > 0) add("$skippedDuplicates duplicates skipped")
            if (failed > 0) add("$failed failed")
        }
        return parts.joinToString(" · ")
    }
}
