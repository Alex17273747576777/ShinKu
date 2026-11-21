package com.shinku.reader.ui.library

import com.shinku.reader.domain.library.model.LibraryManga

data class DuplicateMangaGroup(
    val normalizedTitle: String,
    val displayTitle: String,
    val entries: List<LibraryManga>,
    val mergeGroupIds: Set<Long>,
) {
    val isFullyGrouped: Boolean
        get() = mergeGroupIds.size == 1 && entries.all { it.id in groupedMemberIds }

    var groupedMemberIds: Set<Long> = emptySet()
        internal set
}

object LibraryDuplicateScanner {
    fun findDuplicateGroups(
        libraryManga: List<LibraryManga>,
        memberships: Map<Long, Long>,
    ): List<DuplicateMangaGroup> {
        val grouped = libraryManga
            .distinctBy { it.id }
            .groupBy { it.manga.title.trim().lowercase() }
            .filter { (key, entries) -> key.isNotEmpty() && entries.size > 1 }

        return grouped.mapNotNull { (normalizedTitle, entries) ->
            val sortedEntries = entries.sortedWith(
                compareByDescending<LibraryManga> { it.totalChapters }
                    .thenBy { it.manga.source }
                    .thenBy { it.id },
            )
            val entryGroupIds = sortedEntries.mapNotNull { memberships[it.id] }.toSet()
            val membersWithGroup = sortedEntries.filter { it.id in memberships }.map { it.id }.toSet()
            val allInSameGroup = entryGroupIds.size == 1 && membersWithGroup.size == sortedEntries.size
            if (allInSameGroup) return@mapNotNull null

            DuplicateMangaGroup(
                normalizedTitle = normalizedTitle,
                displayTitle = sortedEntries.first().manga.title,
                entries = sortedEntries,
                mergeGroupIds = entryGroupIds,
            ).apply {
                groupedMemberIds = membersWithGroup
            }
        }.sortedBy { it.displayTitle.lowercase() }
    }
}
