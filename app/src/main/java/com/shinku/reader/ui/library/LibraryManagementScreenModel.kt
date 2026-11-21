package com.shinku.reader.ui.library

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.shinku.reader.core.common.util.lang.launchIO
import com.shinku.reader.domain.library.model.LibraryManga
import com.shinku.reader.domain.manga.interactor.DeleteMergeGroupMember
import com.shinku.reader.domain.manga.interactor.GetLibraryManga
import com.shinku.reader.domain.manga.interactor.GetMergeGroupMemberships
import com.shinku.reader.domain.manga.interactor.GroupManga
import com.shinku.reader.domain.source.service.SourceManager
import com.shinku.reader.exh.source.ShinKuPreferences
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Immutable
data class ActiveMergeGroupUi(
    val groupId: Long,
    val members: ImmutableList<LibraryManga>,
)

class LibraryManagementScreenModel(
    private val getLibraryManga: GetLibraryManga = Injekt.get(),
    private val getMergeGroupMemberships: GetMergeGroupMemberships = Injekt.get(),
    private val groupManga: GroupManga = Injekt.get(),
    private val deleteMergeGroupMember: DeleteMergeGroupMember = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val shinkuPreferences: ShinKuPreferences = Injekt.get(),
) : StateScreenModel<LibraryManagementScreenModel.State>(State()) {

    init {
        combine(
            getLibraryManga.subscribe(),
            getMergeGroupMemberships.subscribe(),
        ) { libraryList, memberships ->
            val uniqueLibrary = libraryList.distinctBy { it.id }
            val libraryById = uniqueLibrary.associateBy { it.id }

            val duplicateGroups = LibraryDuplicateScanner
                .findDuplicateGroups(uniqueLibrary, memberships)
                .filter { group ->
                    val primary = group.entries.first()
                    group.entries.drop(1).any { other ->
                        !shinkuPreferences.isManualUnmerge(primary.id, other.id)
                    }
                }
                .toImmutableList()

            val activeGroups = memberships.entries
                .groupBy({ it.value }, { it.key })
                .mapNotNull { (groupId, memberIds) ->
                    val members = memberIds.mapNotNull { libraryById[it] }
                        .sortedByDescending { it.totalChapters }
                        .toImmutableList()
                    if (members.size < 2) null else ActiveMergeGroupUi(groupId, members)
                }
                .sortedBy { it.members.firstOrNull()?.manga?.title?.lowercase().orEmpty() }
                .toImmutableList()

            val unavailableSourceEntries = uniqueLibrary
                .filter { sourceManager.get(it.manga.source) == null }
                .sortedBy { it.manga.title.lowercase() }
                .toImmutableList()

            State(
                isLoading = false,
                duplicateGroups = duplicateGroups,
                activeMergeGroups = activeGroups,
                unavailableSourceEntries = unavailableSourceEntries,
            )
        }
            .onEach { next -> mutableState.value = next }
            .launchIn(screenModelScope)
    }

    fun getSourceName(sourceId: Long): String {
        return sourceManager.getOrStub(sourceId).name
    }

    fun groupDuplicates(group: DuplicateMangaGroup) {
        val entries = group.entries
        if (entries.size < 2) return
        val primary = entries.first()
        screenModelScope.launchIO {
            entries.drop(1).forEach { candidate ->
                groupManga.await(primary.id, candidate.id)
            }
        }
    }

    fun ignoreDuplicateGroup(group: DuplicateMangaGroup) {
        val entries = group.entries
        if (entries.size < 2) return
        val primary = entries.first()
        entries.drop(1).forEach { candidate ->
            shinkuPreferences.addManualUnmerge(primary.id, candidate.id)
        }
        mutableState.update { current ->
            current.copy(
                duplicateGroups = current.duplicateGroups
                    .filterNot { it.normalizedTitle == group.normalizedTitle }
                    .toImmutableList(),
            )
        }
    }

    fun removeFromMergeGroup(mangaId: Long) {
        screenModelScope.launchIO {
            deleteMergeGroupMember.await(mangaId)
        }
    }

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val duplicateGroups: ImmutableList<DuplicateMangaGroup> = persistentListOf(),
        val activeMergeGroups: ImmutableList<ActiveMergeGroupUi> = persistentListOf(),
        val unavailableSourceEntries: ImmutableList<LibraryManga> = persistentListOf(),
    )
}
