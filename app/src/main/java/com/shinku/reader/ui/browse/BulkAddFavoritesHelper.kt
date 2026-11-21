package com.shinku.reader.ui.browse

import com.shinku.reader.domain.category.interactor.GetCategories
import com.shinku.reader.domain.category.interactor.SetMangaCategories
import com.shinku.reader.domain.category.model.Category
import com.shinku.reader.domain.chapter.interactor.SetMangaDefaultChapterFlags
import com.shinku.reader.domain.library.service.LibraryPreferences
import com.shinku.reader.domain.manga.interactor.GetDuplicateLibraryManga
import com.shinku.reader.domain.manga.interactor.UpdateManga
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.domain.manga.model.toMangaUpdate
import com.shinku.reader.domain.track.interactor.AddTracks
import com.shinku.reader.exh.source.ShinKuPreferences
import eu.kanade.tachiyomi.source.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.firstOrNull
import java.time.Instant

sealed interface BulkAddPreparation {
    data class Ready(val categoryIds: List<Long>) : BulkAddPreparation
    data class NeedsCategoryChoice(val categories: List<Category>) : BulkAddPreparation
}

class BulkAddFavoritesHelper(
    private val libraryPreferences: LibraryPreferences,
    private val getCategories: GetCategories,
    private val setMangaCategories: SetMangaCategories,
    private val setMangaDefaultChapterFlags: SetMangaDefaultChapterFlags,
    private val addTracks: AddTracks,
    private val updateManga: UpdateManga,
    private val getDuplicateLibraryManga: GetDuplicateLibraryManga,
    private val shinkuPreferences: ShinKuPreferences,
) {
    suspend fun prepareCategories(): BulkAddPreparation {
        val categories = getCategories.subscribe()
            .firstOrNull()
            ?.filterNot { it.isSystemCategory }
            .orEmpty()
        val defaultCategoryId = libraryPreferences.defaultCategory().get()
        val defaultCategory = categories.find { it.id == defaultCategoryId.toLong() }

        return when {
            defaultCategory != null -> BulkAddPreparation.Ready(listOf(defaultCategory.id))
            defaultCategoryId == 0 || categories.isEmpty() -> BulkAddPreparation.Ready(emptyList())
            else -> BulkAddPreparation.NeedsCategoryChoice(categories)
        }
    }

    suspend fun addFavorites(
        mangas: List<Manga>,
        categoryIds: List<Long>,
        sourceProvider: (Long) -> Source?,
    ): BulkFavoriteSummary {
        var added = 0
        var skippedAlreadyFavorite = 0
        var skippedDuplicates = 0
        var failed = 0
        val mergeGroupsEnabled = shinkuPreferences.enableLibraryMergeGroups().get()
        val filteredCategoryIds = categoryIds.filter { it != 0L }

        for (manga in mangas) {
            if (manga.favorite) {
                skippedAlreadyFavorite++
                continue
            }

            try {
                val duplicates = getDuplicateLibraryManga.invoke(manga)
                val activeDuplicates = if (mergeGroupsEnabled) {
                    duplicates.filterNot { shinkuPreferences.isManualUnmerge(manga.id, it.manga.id) }
                } else {
                    duplicates
                }
                if (activeDuplicates.isNotEmpty()) {
                    skippedDuplicates++
                    continue
                }

                setMangaCategories.await(
                    mangaId = manga.id,
                    categoryIds = filteredCategoryIds,
                )
                setMangaDefaultChapterFlags.await(manga)
                sourceProvider(manga.source)?.let { source ->
                    addTracks.bindEnhancedTrackers(manga, source)
                }
                val updated = manga.copy(
                    favorite = true,
                    dateAdded = Instant.now().toEpochMilli(),
                )
                val saved = updateManga.await(updated.toMangaUpdate())
                if (saved) {
                    added++
                } else {
                    failed++
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failed++
            }
        }

        return BulkFavoriteSummary(
            added = added,
            skippedAlreadyFavorite = skippedAlreadyFavorite,
            skippedDuplicates = skippedDuplicates,
            failed = failed,
        )
    }
}
