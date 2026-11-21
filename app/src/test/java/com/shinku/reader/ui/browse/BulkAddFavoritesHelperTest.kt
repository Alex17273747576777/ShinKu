package com.shinku.reader.ui.browse

import com.shinku.reader.core.common.preference.Preference
import com.shinku.reader.domain.category.interactor.GetCategories
import com.shinku.reader.domain.category.interactor.SetMangaCategories
import com.shinku.reader.domain.chapter.interactor.SetMangaDefaultChapterFlags
import com.shinku.reader.domain.library.service.LibraryPreferences
import com.shinku.reader.domain.manga.interactor.GetDuplicateLibraryManga
import com.shinku.reader.domain.manga.interactor.UpdateManga
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.domain.manga.model.MangaWithChapterCount
import com.shinku.reader.domain.track.interactor.AddTracks
import com.shinku.reader.exh.source.ShinKuPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BulkAddFavoritesHelperTest {

    private fun testManga(id: Long, source: Long, title: String): Manga {
        return Manga.create().copy(
            id = id,
            source = source,
            url = "/manga/$id",
            ogTitle = title,
            favorite = false,
        )
    }

    @Test
    fun `addFavorites separates added, duplicates, and failed items honestly`() = runTest {
        val libraryPreferences = mockk<LibraryPreferences>(relaxed = true)
        val getCategories = mockk<GetCategories>(relaxed = true)
        val setMangaCategories = mockk<SetMangaCategories>(relaxed = true)
        val setMangaDefaultChapterFlags = mockk<SetMangaDefaultChapterFlags>(relaxed = true)
        val addTracks = mockk<AddTracks>(relaxed = true)
        val updateManga = mockk<UpdateManga>()
        val getDuplicateLibraryManga = mockk<GetDuplicateLibraryManga>()
        val shinkuPreferences = mockk<ShinKuPreferences>()

        val mergePref = mockk<Preference<Boolean>>()
        every { mergePref.get() } returns false
        every { shinkuPreferences.enableLibraryMergeGroups() } returns mergePref

        val m1 = testManga(1L, 10L, "First")
        val m2Duplicate = testManga(2L, 10L, "Duplicate")
        val m3Fail = testManga(3L, 10L, "Fail")

        coEvery { getDuplicateLibraryManga.invoke(m1) } returns emptyList()
        coEvery { getDuplicateLibraryManga.invoke(m2Duplicate) } returns listOf(mockk<MangaWithChapterCount>(relaxed = true))
        coEvery { getDuplicateLibraryManga.invoke(m3Fail) } returns emptyList()

        coEvery { updateManga.await(match { it.id == 1L }) } returns true
        coEvery { updateManga.await(match { it.id == 3L }) } returns false

        val helper = BulkAddFavoritesHelper(
            libraryPreferences = libraryPreferences,
            getCategories = getCategories,
            setMangaCategories = setMangaCategories,
            setMangaDefaultChapterFlags = setMangaDefaultChapterFlags,
            addTracks = addTracks,
            updateManga = updateManga,
            getDuplicateLibraryManga = getDuplicateLibraryManga,
            shinkuPreferences = shinkuPreferences,
        )

        val summary = helper.addFavorites(
            mangas = listOf(m1, m2Duplicate, m3Fail),
            categoryIds = listOf(5L),
            sourceProvider = { null },
        )

        assertEquals(1, summary.added)
        assertEquals(0, summary.skippedAlreadyFavorite)
        assertEquals(1, summary.skippedDuplicates)
        assertEquals(1, summary.failed)
        assertTrue(summary.hasFailures)

        coVerify(exactly = 1) { setMangaCategories.await(1L, listOf(5L)) }
        coVerify(exactly = 0) { setMangaCategories.await(2L, any()) }
    }
}
