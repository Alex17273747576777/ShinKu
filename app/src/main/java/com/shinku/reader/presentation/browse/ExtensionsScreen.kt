package com.shinku.reader.presentation.browse

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.shinku.reader.extension.model.Extension
import com.shinku.reader.extension.model.InstallStep
import com.shinku.reader.i18n.MR
import com.shinku.reader.i18n.sy.SYMR
import com.shinku.reader.presentation.browse.components.BaseBrowseItem
import com.shinku.reader.presentation.browse.components.ExtensionIcon
import com.shinku.reader.presentation.components.WarningBanner
import com.shinku.reader.presentation.core.components.FastScrollLazyColumn
import com.shinku.reader.presentation.core.components.material.PullRefresh
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.components.material.topSmallPaddingValues
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.screens.EmptyScreen
import com.shinku.reader.presentation.core.screens.EmptyScreenAction
import com.shinku.reader.presentation.core.screens.LoadingScreen
import com.shinku.reader.presentation.core.theme.header
import com.shinku.reader.presentation.core.util.plus
import com.shinku.reader.presentation.core.util.secondaryItemAlpha
import com.shinku.reader.presentation.manga.components.DotSeparatorNoSpaceText
import com.shinku.reader.presentation.more.settings.screen.browse.ExtensionReposScreen
import com.shinku.reader.presentation.util.animateItemFastScroll
import com.shinku.reader.presentation.util.rememberRequestPackageInstallsPermissionState
import com.shinku.reader.ui.browse.extension.ExtensionUiModel
import com.shinku.reader.ui.browse.extension.ExtensionsScreenModel
import com.shinku.reader.util.system.LocaleHelper
import com.shinku.reader.util.system.launchRequestPackageInstallsPermission
import dev.icerock.moko.resources.StringResource
import kotlinx.collections.immutable.persistentListOf

@Composable
fun ExtensionScreen(
    state: ExtensionsScreenModel.State,
    contentPadding: PaddingValues,
    searchQuery: String?,
    onLongClickItem: (Extension) -> Unit,
    onClickItemCancel: (Extension) -> Unit,
    onOpenWebView: (Extension.Available) -> Unit,
    onInstallExtension: (Extension.Available) -> Unit,
    onUninstallExtension: (Extension) -> Unit,
    onUpdateExtension: (Extension.Installed) -> Unit,
    onTrustExtension: (Extension.Untrusted) -> Unit,
    onOpenExtension: (Extension.Installed) -> Unit,
    onClickUpdateAll: () -> Unit,
    onRefresh: () -> Unit,
    onToggleSelectExtension: (String) -> Unit = {},
    onCancelSelectionMode: () -> Unit = {},
    onInstallSelected: () -> Unit = {},
) {
    val navigator = LocalNavigator.currentOrThrow

    PullRefresh(
        refreshing = state.isRefreshing,
        onRefresh = onRefresh,
        enabled = !state.isLoading,
    ) {
        when {
            state.isLoading -> LoadingScreen(Modifier.padding(contentPadding))
            state.isEmpty -> {
                val msg = if (!searchQuery.isNullOrEmpty()) {
                    MR.strings.no_results_found
                } else {
                    MR.strings.empty_screen
                }
                EmptyScreen(
                    msg,
                    modifier = Modifier.padding(contentPadding),
                    actions = persistentListOf(
                        EmptyScreenAction(
                            stringRes = MR.strings.label_extension_repos,
                            icon = Icons.Outlined.Settings,
                            onClick = { navigator.push(ExtensionReposScreen()) },
                        ),
                    ),
                )
            }
            else -> {
                ExtensionContent(
                    state = state,
                    contentPadding = contentPadding,
                    onLongClickItem = onLongClickItem,
                    onClickItemCancel = onClickItemCancel,
                    onOpenWebView = onOpenWebView,
                    onInstallExtension = onInstallExtension,
                    onUninstallExtension = onUninstallExtension,
                    onUpdateExtension = onUpdateExtension,
                    onTrustExtension = onTrustExtension,
                    onOpenExtension = onOpenExtension,
                    onClickUpdateAll = onClickUpdateAll,
                    onToggleSelectExtension = onToggleSelectExtension,
                    onCancelSelectionMode = onCancelSelectionMode,
                    onInstallSelected = onInstallSelected,
                )
            }
        }
    }
}

@Composable
private fun ExtensionContent(
    state: ExtensionsScreenModel.State,
    contentPadding: PaddingValues,
    onLongClickItem: (Extension) -> Unit,
    onClickItemCancel: (Extension) -> Unit,
    onOpenWebView: (Extension.Available) -> Unit,
    onInstallExtension: (Extension.Available) -> Unit,
    onUninstallExtension: (Extension) -> Unit,
    onUpdateExtension: (Extension.Installed) -> Unit,
    onTrustExtension: (Extension.Untrusted) -> Unit,
    onOpenExtension: (Extension.Installed) -> Unit,
    onClickUpdateAll: () -> Unit,
    onToggleSelectExtension: (String) -> Unit,
    onCancelSelectionMode: () -> Unit,
    onInstallSelected: () -> Unit,
) {
    val context = LocalContext.current
    var trustState by remember { mutableStateOf<Extension.Untrusted?>(null) }
    var availableInfoState by remember { mutableStateOf<Extension.Available?>(null) }
    val installGranted = rememberRequestPackageInstallsPermissionState(initialValue = true)
    var selectedLang by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }

    val availableLanguages = remember(state.items) {
        state.items.values
            .flatten()
            .mapNotNull { it.extension.lang }
            .filter { it.isNotBlank() && it != "all" }
            .distinct()
    }

    val layoutDirection = LocalLayoutDirection.current
    val innerPadding = if (state.selectionMode) {
        PaddingValues(
            start = contentPadding.calculateStartPadding(layoutDirection),
            end = contentPadding.calculateEndPadding(layoutDirection),
            bottom = contentPadding.calculateBottomPadding(),
            top = 0.dp,
        )
    } else {
        contentPadding
    }

    Column {
        if (state.selectionMode) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = contentPadding.calculateTopPadding()),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onCancelSelectionMode) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(MR.strings.action_cancel),
                            )
                        }
                        Text(
                            text = "${state.selectedPkgNames.size} selected",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                    Button(
                        onClick = onInstallSelected,
                        enabled = state.selectedPkgNames.isNotEmpty(),
                    ) {
                        Text(text = stringResource(MR.strings.ext_install))
                    }
                }
            }
        }

        FastScrollLazyColumn(
            contentPadding = innerPadding + topSmallPaddingValues,
        ) {
            if (!installGranted && state.installer?.requiresSystemPermission == true) {
                item(key = "extension-permissions-warning") {
                    WarningBanner(
                        textRes = MR.strings.ext_permission_install_apps_warning,
                        modifier = Modifier.clickable {
                            context.launchRequestPackageInstallsPermission()
                        },
                    )
                }
            }

            if (availableLanguages.size > 1) {
                item(key = "extensions-language-chips") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            ),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            item(key = "ext-lang-all") {
                                ExtensionLanguageChip(
                                    label = stringResource(MR.strings.all),
                                    selected = selectedLang == null,
                                    onClick = { selectedLang = null },
                                )
                            }
                            items(availableLanguages, key = { "ext-lang-$it" }) { lang ->
                                ExtensionLanguageChip(
                                    label = LocaleHelper.getSourceDisplayName(lang, context),
                                    selected = selectedLang == lang,
                                    onClick = {
                                        selectedLang = if (selectedLang == lang) null else lang
                                    },
                                )
                            }
                        }
                    }
                }
            }

            state.items.forEach { (header, rawItems) ->
                val items = if (selectedLang == null) {
                    rawItems
                } else {
                    rawItems.filter { it.extension.lang.equals(selectedLang, ignoreCase = true) || it.extension.lang == "all" }
                }
                if (items.isEmpty()) return@forEach

                item(
                    contentType = "header",
                    key = "extensionHeader-${header.hashCode()}",
                ) {
                    when (header) {
                        is ExtensionUiModel.Header.Resource -> {
                            val action: @Composable RowScope.() -> Unit =
                                if (header.textRes == MR.strings.ext_updates_pending) {
                                    {
                                        Button(onClick = { onClickUpdateAll() }) {
                                            Text(
                                                text = stringResource(MR.strings.ext_update_all),
                                                style = LocalTextStyle.current.copy(
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                ),
                                            )
                                        }
                                    }
                                } else {
                                    {}
                                }
                            ExtensionHeader(
                                textRes = header.textRes,
                                count = items.size,
                                modifier = Modifier.animateItemFastScroll(),
                                action = action,
                            )
                        }
                        is ExtensionUiModel.Header.Text -> {
                            ExtensionHeader(
                                text = header.text,
                                count = items.size,
                                modifier = Modifier.animateItemFastScroll(),
                            )
                        }
                    }
                }

                items(
                    items = items,
                    contentType = { "item" },
                    key = { item ->
                        when (item.extension) {
                            is Extension.Untrusted -> "extension-untrusted-${item.hashCode()}"
                            is Extension.Installed -> "extension-installed-${item.hashCode()}"
                            is Extension.Available -> "extension-available-${item.hashCode()}"
                        }
                    },
                ) { item ->
                    val isSelectable = item.extension is Extension.Available ||
                        (item.extension is Extension.Installed && item.extension.hasUpdate)
                    val isSelected = state.selectionMode && item.extension.pkgName in state.selectedPkgNames
                    ExtensionItem(
                        modifier = Modifier.animateItemFastScroll(),
                        item = item,
                        selectionMode = state.selectionMode && isSelectable,
                        isSelected = isSelected,
                        onClickItem = {
                            if (state.selectionMode && isSelectable) {
                                onToggleSelectExtension(it.pkgName)
                            } else {
                                when (it) {
                                    is Extension.Available -> onInstallExtension(it)
                                    is Extension.Installed -> onOpenExtension(it)
                                    is Extension.Untrusted -> {
                                        trustState = it
                                    }
                                }
                            }
                        },
                        onLongClickItem = { ext ->
                            if (isSelectable) {
                                onToggleSelectExtension(ext.pkgName)
                            } else {
                                onLongClickItem(ext)
                            }
                        },
                        onClickItemSecondaryAction = {
                            when (it) {
                                is Extension.Available -> onOpenWebView(it)
                                is Extension.Installed -> onOpenExtension(it)
                                else -> {}
                            }
                        },
                        onClickInfo = { ext ->
                            if (ext is Extension.Available) {
                                availableInfoState = ext
                            }
                        },
                        onClickItemCancel = onClickItemCancel,
                        onClickItemAction = {
                            when (it) {
                                is Extension.Available -> onInstallExtension(it)
                                is Extension.Installed -> {
                                    if (it.hasUpdate) {
                                        onUpdateExtension(it)
                                    } else {
                                        onOpenExtension(it)
                                    }
                                }
                                is Extension.Untrusted -> {
                                    trustState = it
                                }
                            }
                        },
                    )
                }
            }
        }
    }
    if (trustState != null) {
        ExtensionTrustDialog(
            onClickConfirm = {
                onTrustExtension(trustState!!)
                trustState = null
            },
            onClickDismiss = {
                onUninstallExtension(trustState!!)
                trustState = null
            },
            onDismissRequest = {
                trustState = null
            },
        )
    }
    availableInfoState?.let { availableExt ->
        AvailableExtensionInfoDialog(
            extension = availableExt,
            onInstall = {
                onInstallExtension(availableExt)
                availableInfoState = null
            },
            onDismissRequest = { availableInfoState = null },
        )
    }
}

@Composable
private fun ExtensionItem(
    item: ExtensionUiModel.Item,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    onClickItem: (Extension) -> Unit,
    onLongClickItem: (Extension) -> Unit,
    onClickItemCancel: (Extension) -> Unit,
    onClickItemAction: (Extension) -> Unit,
    onClickItemSecondaryAction: (Extension) -> Unit,
    onClickInfo: (Extension) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val (extension, installStep) = item
    BaseBrowseItem(
        modifier = modifier
            .then(
                if (isSelected) {
                    Modifier.background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                } else {
                    Modifier
                },
            )
            .combinedClickable(
                onClick = { onClickItem(extension) },
                onLongClick = { onLongClickItem(extension) },
            ),
        onClickItem = { onClickItem(extension) },
        onLongClickItem = { onLongClickItem(extension) },
        icon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClickItem(extension) },
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val idle = installStep.isCompleted()
                    if (!idle) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 2.dp,
                        )
                    }

                    val padding by animateDpAsState(targetValue = if (idle) 0.dp else 8.dp)
                    ExtensionIcon(
                        extension = extension,
                        modifier = Modifier
                            .matchParentSize()
                            .padding(padding),
                    )
                }
            }
        },
        action = {
            ExtensionItemActions(
                extension = extension,
                installStep = installStep,
                onClickItemCancel = onClickItemCancel,
                onClickItemAction = onClickItemAction,
                onClickItemSecondaryAction = onClickItemSecondaryAction,
                onClickInfo = onClickInfo,
            )
        },
    ) {
        ExtensionItemContent(
            extension = extension,
            installStep = installStep,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ExtensionItemContent(
    extension: Extension,
    installStep: InstallStep,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(start = MaterialTheme.padding.medium),
    ) {
        Text(
            text = extension.name,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
        )
        // Won't look good but it's not like we can ellipsize overflowing content
        FlowRow(
            modifier = Modifier.secondaryItemAlpha(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
        ) {
            ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
                var hasAlreadyShownAnElement by remember { mutableStateOf(false) }
                if (extension.versionName.isNotEmpty()) {
                    hasAlreadyShownAnElement = true
                    Text(
                        text = "v${extension.versionName.removePrefix("v")}",
                    )
                }

                if (!extension.lang.isNullOrEmpty()) {
                    if (hasAlreadyShownAnElement) DotSeparatorNoSpaceText()
                    hasAlreadyShownAnElement = true
                    Text(
                        text = LocaleHelper.getSourceDisplayName(extension.lang, LocalContext.current),
                    )
                }

                val warning = when {
                    extension is Extension.Installed && extension.isObsolete -> MR.strings.ext_obsolete
                    // SY -->
                    extension is Extension.Installed && extension.isRedundant -> SYMR.strings.ext_redundant
                    // SY <--
                    extension.isNsfw -> MR.strings.ext_nsfw_short
                    else -> null
                }
                if (warning != null) {
                    if (hasAlreadyShownAnElement) DotSeparatorNoSpaceText()
                    hasAlreadyShownAnElement = true
                    Text(
                        text = stringResource(warning).uppercase(),
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (extension is Extension.Installed && !extension.isShared) {
                    if (hasAlreadyShownAnElement) DotSeparatorNoSpaceText()
                    Text(
                        text = stringResource(MR.strings.ext_installer_private),
                    )
                }

                if (!installStep.isCompleted()) {
                    DotSeparatorNoSpaceText()
                    Text(
                        text = when (installStep) {
                            InstallStep.Pending -> stringResource(MR.strings.ext_pending)
                            InstallStep.Downloading -> stringResource(MR.strings.ext_downloading)
                            InstallStep.Installing -> stringResource(MR.strings.ext_installing)
                            else -> error("Must not show non-install process text")
                        },
                    )
                }
            }
        }
        if (extension is Extension.Untrusted) {
            Text(
                text = "⚠ This extension is from an unknown repository. Review the source before trusting.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun ExtensionItemActions(
    extension: Extension,
    installStep: InstallStep,
    modifier: Modifier = Modifier,
    onClickItemCancel: (Extension) -> Unit = {},
    onClickItemAction: (Extension) -> Unit = {},
    onClickItemSecondaryAction: (Extension) -> Unit = {},
    onClickInfo: (Extension) -> Unit = {},
) {
    val isIdle = installStep.isCompleted()

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
    ) {
        when {
            !isIdle -> {
                IconButton(
                    onClick = { onClickItemCancel(extension) },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(MR.strings.action_cancel),
                    )
                }
            }
            installStep == InstallStep.Error -> {
                IconButton(
                    onClick = { onClickItemAction(extension) },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = stringResource(MR.strings.action_retry),
                    )
                }
            }
            installStep == InstallStep.Idle -> {
                when (extension) {
                    is Extension.Installed -> {
                        if (extension.hasUpdate) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onClickItemAction(extension) },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.GetApp,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                    )
                                    Text(
                                        text = stringResource(MR.strings.ext_update),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                        ),
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { onClickItemSecondaryAction(extension) },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = stringResource(MR.strings.action_settings),
                            )
                        }
                    }
                    is Extension.Untrusted -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onClickItemAction(extension) },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.VerifiedUser,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    text = stringResource(MR.strings.ext_trust),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    ),
                                )
                            }
                        }
                    }
                    is Extension.Available -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onClickItemAction(extension) },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.GetApp,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    text = stringResource(MR.strings.ext_install),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    ),
                                )
                            }
                        }

                        IconButton(
                            onClick = { onClickInfo(extension) },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "Details",
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        if (extension.sources.isNotEmpty()) {
                            IconButton(
                                onClick = { onClickItemSecondaryAction(extension) },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Public,
                                    contentDescription = stringResource(MR.strings.action_open_in_web_view),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtensionHeader(
    textRes: StringResource,
    count: Int? = null,
    modifier: Modifier = Modifier,
    action: @Composable RowScope.() -> Unit = {},
) {
    ExtensionHeader(
        text = stringResource(textRes),
        count = count,
        modifier = modifier,
        action = action,
    )
}

@Composable
private fun ExtensionHeader(
    text: String,
    count: Int? = null,
    modifier: Modifier = Modifier,
    action: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (count != null) "$text ($count)" else text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier.weight(1f),
        )
        action()
    }
}

@Composable
private fun ExtensionLanguageChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = com.shinku.reader.presentation.core.theme.SanctuaryShapes.pill,
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else {
            com.shinku.reader.presentation.core.theme.SanctuaryBrushes.subtleBorder()
        },
        modifier = Modifier
            .clip(com.shinku.reader.presentation.core.theme.SanctuaryShapes.pill)
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            ),
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun ExtensionTrustDialog(
    onClickConfirm: () -> Unit,
    onClickDismiss: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        title = {
            Text(text = stringResource(MR.strings.untrusted_extension))
        },
        text = {
            Text(text = stringResource(MR.strings.untrusted_extension_message))
        },
        confirmButton = {
            TextButton(onClick = onClickConfirm) {
                Text(text = stringResource(MR.strings.ext_trust))
            }
        },
        dismissButton = {
            TextButton(onClick = onClickDismiss) {
                Text(text = stringResource(MR.strings.ext_uninstall))
            }
        },
        onDismissRequest = onDismissRequest,
    )
}

@Composable
private fun AvailableExtensionInfoDialog(
    extension: Extension.Available,
    onInstall: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    AlertDialog(
        title = {
            Text(
                text = extension.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "Package: ${extension.pkgName}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "Version: v${extension.versionName.removePrefix("v")} (${extension.versionCode})",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "Language: ${LocaleHelper.getSourceDisplayName(extension.lang, context)}",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (extension.isNsfw) {
                    Text(
                        text = stringResource(MR.strings.ext_nsfw_short),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (extension.repoUrl.isNotBlank()) {
                    Text(
                        text = "Repository: ${extension.repoUrl}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (extension.sources.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Included sources (${extension.sources.size}):",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    extension.sources.forEach { source ->
                        Text(
                            text = "• ${source.name} (${source.lang}) — ${source.baseUrl}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onInstall) {
                Text(text = stringResource(MR.strings.ext_install))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        onDismissRequest = onDismissRequest,
    )
}
