package com.shinku.reader.ui.library

import com.shinku.reader.domain.library.model.LibraryManga
import com.shinku.reader.domain.manga.model.Manga
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LibraryDuplicateScannerTest {

    private fun libManga(id: Long, source: Long, title: String, chapters: Long): LibraryManga {
        val manga = Manga.create().copy(
            id = id,
            source = source,
            url = "/m/$id",
            ogTitle = title,
            favorite = false,
        )
        return LibraryManga(
            manga = manga,
            categories = listOf(0L),
            totalChapters = chapters,
            readCount = 0L,
            bookmarkCount = 0L,
            latestUpload = 0L,
            chapterFetchedAt = 0L,
            lastRead = 0L,
        )
    }

    @Test
    fun `groups duplicate titles ignoring case and whitespace and excludes fully grouped sets`() {
        val a1 = libManga(1L, 10L, "Solo Leveling", 180L)
        val a2 = libManga(2L, 20L, "  solo leveling ", 200L)
        val b1 = libManga(3L, 10L, "Already Grouped", 50L)
        val b2 = libManga(4L, 20L, "already grouped", 40L)
        val unique = libManga(5L, 30L, "Unique Title", 12L)

        val memberships = mapOf(
            3L to 99L,
            4L to 99L,
        )

        val duplicates = LibraryDuplicateScanner.findDuplicateGroups(
            libraryManga = listOf(a1, a2, b1, b2, unique),
            memberships = memberships,
        )

        assertEquals(1, duplicates.size)
        val group = duplicates.first()
        assertEquals("solo leveling", group.normalizedTitle)
        assertEquals(listOf(2L, 1L), group.entries.map { it.id })
        assertTrue(group.mergeGroupIds.isEmpty())
    }
}
