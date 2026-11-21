package com.shinku.reader.ui.browse.source.globalsearch

import cafe.adriel.voyager.core.model.screenModelScope
import com.shinku.reader.core.common.preference.mapAsCheckboxState
import com.shinku.reader.core.common.util.lang.launchIO
import com.shinku.reader.domain.category.interactor.GetCategories
import com.shinku.reader.domain.category.interactor.SetMangaCategories
import com.shinku.reader.domain.chapter.interactor.SetMangaDefaultChapterFlags
import com.shinku.reader.domain.library.service.LibraryPreferences
import com.shinku.reader.domain.manga.interactor.GetDuplicateLibraryManga
import com.shinku.reader.domain.manga.interactor.UpdateManga
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.domain.source.service.SourceManager
import com.shinku.reader.domain.track.interactor.AddTracks
import com.shinku.reader.exh.source.ShinKuPreferences
import com.shinku.reader.ui.browse.BulkAddFavoritesHelper
import com.shinku.reader.ui.browse.BulkAddPreparation
import com.shinku.reader.ui.browse.BulkFavoriteSummary
import eu.kanade.tachiyomi.source.CatalogueSource
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class GlobalSearchScreenModel(
    initialQuery: String = "",
    initialExtensionFilter: String? = null,
    private val sourceManager: SourceManager = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val setMangaCategories: SetMangaCategories = Injekt.get(),
    private val setMangaDefaultChapterFlags: SetMangaDefaultChapterFlags = Injekt.get(),
    private val addTracks: AddTracks = Injekt.get(),
    private val updateManga: UpdateManga = Injekt.get(),
    private val getDuplicateLibraryManga: GetDuplicateLibraryManga = Injekt.get(),
    private val shinkuPreferences: ShinKuPreferences = Injekt.get(),
) : SearchScreenModel(State(searchQuery = initialQuery)) {

    private val _bulkFavoriteEvents = Channel<BulkFavoriteSummary>(Channel.BUFFERED)
    val bulkFavoriteEvents = _bulkFavoriteEvents.receiveAsFlow()

    private val bulkAddHelper by lazy {
        BulkAddFavoritesHelper(
            libraryPreferences = libraryPreferences,
            getCategories = getCategories,
            setMangaCategories = setMangaCategories,
            setMangaDefaultChapterFlags = setMangaDefaultChapterFlags,
            addTracks = addTracks,
            updateManga = updateManga,
            getDuplicateLibraryManga = getDuplicateLibraryManga,
            shinkuPreferences = shinkuPreferences,
        )
    }

    init {
        extensionFilter = initialExtensionFilter
        if (initialQuery.isNotBlank() || !initialExtensionFilter.isNullOrBlank()) {
            if (extensionFilter != null) {
                // we're going to use custom extension filter instead
                setSourceFilter(SourceFilter.All)
            }
            search()
        }
    }

    override fun getEnabledSources(): List<CatalogueSource> {
        return super.getEnabledSources()
            .filter { state.value.sourceFilter != SourceFilter.PinnedOnly || "${it.id}" in pinnedSources }
    }

    fun enterSelectionMode() {
        mutableState.update { it.copy(selection = it.selection.enterSelectionMode()) }
    }

    fun exitSelectionMode() {
        mutableState.update { it.copy(selection = it.selection.exitSelectionMode()) }
    }

    fun toggleSelection(manga: Manga) {
        mutableState.update { it.copy(selection = it.selection.toggle(manga)) }
    }

    private fun loadedResults(): List<Manga> {
        return state.value.filteredItems.values
            .filterIsInstance<SearchItemResult.Success>()
            .flatMap { it.result }
    }

    fun selectAllLoaded() {
        val loaded = loadedResults()
        mutableState.update { it.copy(selection = it.selection.selectAllLoaded(loaded)) }
    }

    fun invertLoaded() {
        val loaded = loadedResults()
        mutableState.update { it.copy(selection = it.selection.invertLoaded(loaded)) }
    }

    fun requestBulkAddToLibrary() {
        val selected = state.value.selection.selectedManga
        if (selected.isEmpty()) return
        screenModelScope.launchIO {
            when (val prep = bulkAddHelper.prepareCategories()) {
                is BulkAddPreparation.Ready -> {
                    executeBulkAdd(selected, prep.categoryIds)
                }
                is BulkAddPreparation.NeedsCategoryChoice -> {
                    mutableState.update {
                        it.copy(
                            dialog = Dialog.BulkChangeMangaCategory(
                                mangas = selected,
                                initialSelection = prep.categories.mapAsCheckboxState { false }.toImmutableList(),
                            ),
                        )
                    }
                }
            }
        }
    }

    fun confirmBulkAddToLibrary(mangas: List<Manga>, categoryIds: List<Long>) {
        screenModelScope.launchIO {
            executeBulkAdd(mangas, categoryIds)
        }
    }

    private suspend fun executeBulkAdd(mangas: List<Manga>, categoryIds: List<Long>) {
        val summary = bulkAddHelper.addFavorites(
            mangas = mangas,
            categoryIds = categoryIds,
            sourceProvider = { sourceManager.getOrStub(it) },
        )
        exitSelectionMode()
        _bulkFavoriteEvents.send(summary)
    }
}
