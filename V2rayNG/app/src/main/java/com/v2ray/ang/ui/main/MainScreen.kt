package com.v2ray.ang.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.R
import com.v2ray.ang.handler.MmkvManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    smartConnecting: Boolean,
    smartCountdownSeconds: Int,
    smartConnectionFailed: Boolean,
    onSmartConnect: () -> Unit,
    onAction: (MainAction) -> Unit,
    onNavigate: (MainDestination) -> Unit,
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val groups = uiState.groups
    val isLoading by mainViewModel.isLoading.collectAsStateWithLifecycle()
    val isRunning = uiState.isRunning
    val selectedGuid = uiState.selectedGuid
    val doubleColumnDisplay = uiState.doubleColumnDisplay
    val confirmRemove = uiState.confirmRemove

    val selectedProfile = selectedGuid?.let { MmkvManager.decodeServerConfig(it) }
    val selectedPing = selectedGuid
        ?.let { MmkvManager.decodeServerAffiliationInfo(it)?.testDelayMillis }
        ?: 0L

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showDelAllConfirm by remember { mutableStateOf(false) }
    var showDelDuplicateConfirm by remember { mutableStateOf(false) }
    var showDelInvalidConfirm by remember { mutableStateOf(false) }
    var showRemoveConfirm by remember { mutableStateOf<String?>(null) }

    val removeServer: (String) -> Unit = { guid ->
        if (confirmRemove) showRemoveConfirm = guid else onAction(MainAction.RemoveServer(guid))
    }

    // Page 0 = Automatic mode, Page 1 = Manual mode.
    val modePagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })

    // Subscription pages live inside Manual mode. Horizontal gestures are disabled on this inner
    // pager so left/right swipes are reserved for switching Automatic <-> Manual.
    val groupPagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { groups.size.coerceAtLeast(1) }
    )

    val lazyListStates = remember { mutableStateMapOf<String, LazyListState>() }
    val lazyGridStates = remember { mutableStateMapOf<String, LazyGridState>() }

    var locateInProgress by remember { mutableStateOf(false) }

    LaunchedEffect(groups) {
        val validGroupIds = groups.map { it.id }.toSet()
        lazyListStates.keys.retainAll(validGroupIds)
        lazyGridStates.keys.retainAll(validGroupIds)
    }

    val latestDoubleColumnDisplay by rememberUpdatedState(doubleColumnDisplay)

    LaunchedEffect(groups, uiState.selectedGroupId) {
        if (groups.isEmpty()) return@LaunchedEffect
        val selectedIndex = groups.indexOfFirst { it.id == uiState.selectedGroupId }
            .takeIf { it >= 0 } ?: 0
        if (!groupPagerState.isScrollInProgress && groupPagerState.settledPage != selectedIndex) {
            groupPagerState.scrollToPage(selectedIndex)
        }
    }

    val latestGroups by rememberUpdatedState(groups)
    val latestLocateInProgress by rememberUpdatedState(locateInProgress)

    LaunchedEffect(groupPagerState) {
        snapshotFlow { groupPagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                val currentGroups = latestGroups
                if (!latestLocateInProgress && page in currentGroups.indices) {
                    onAction(MainAction.SelectGroup(currentGroups[page].id))
                }
            }
    }

    LaunchedEffect(uiState.locateTarget) {
        val target = uiState.locateTarget ?: return@LaunchedEffect
        if (target.groupIndex !in 0 until groupPagerState.pageCount) {
            mainViewModel.onAction(MainAction.LocateHandled(target))
            return@LaunchedEffect
        }

        locateInProgress = true
        try {
            if (modePagerState.settledPage != 1) {
                modePagerState.animateScrollToPage(1)
            }
            if (groupPagerState.settledPage != target.groupIndex) {
                groupPagerState.navigateToPageOptimized(
                    targetPage = target.groupIndex,
                    animateAdjacentPage = false
                )
            }
            onAction(MainAction.SelectGroup(target.groupId))

            repeat(10) {
                val ready = if (latestDoubleColumnDisplay) {
                    lazyGridStates[target.groupId] != null
                } else {
                    lazyListStates[target.groupId] != null
                }
                if (ready) return@repeat
                delay(16L)
            }

            if (latestDoubleColumnDisplay) {
                lazyGridStates[target.groupId]?.let { gridState ->
                    gridState.scrollToItem(
                        index = target.itemPosition,
                        scrollOffset = -gridState.layoutInfo.viewportSize.height / 3
                    )
                }
            } else {
                lazyListStates[target.groupId]?.let { listState ->
                    listState.scrollToItem(
                        index = target.itemPosition,
                        scrollOffset = -listState.layoutInfo.viewportSize.height / 3
                    )
                }
            }
        } finally {
            delay(32L)
            locateInProgress = false
            mainViewModel.onAction(MainAction.LocateHandled(target))
        }
    }

    MainDialogs(
        showDelAllConfirm = showDelAllConfirm,
        onDismissDelAll = { showDelAllConfirm = false },
        onConfirmDelAll = { showDelAllConfirm = false; onAction(MainAction.RemoveAllServers) },
        showDelDuplicateConfirm = showDelDuplicateConfirm,
        onDismissDelDuplicate = { showDelDuplicateConfirm = false },
        onConfirmDelDuplicate = { showDelDuplicateConfirm = false; onAction(MainAction.RemoveDuplicateServers) },
        showDelInvalidConfirm = showDelInvalidConfirm,
        onDismissDelInvalid = { showDelInvalidConfirm = false },
        onConfirmDelInvalid = { showDelInvalidConfirm = false; onAction(MainAction.RemoveInvalidServers) },
        showRemoveConfirm = showRemoveConfirm,
        onDismissRemove = { showRemoveConfirm = null },
        onConfirmRemove = { guid -> showRemoveConfirm = null; onAction(MainAction.RemoveServer(guid)) }
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MainDrawerContent(
                drawerState = drawerState,
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    onNavigate(route)
                }
            )
        }
    ) {
        Scaffold(
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
            topBar = {
                MainTopBar(
                    isLoading = isLoading,
                    showSearch = showSearch,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { query: String ->
                        searchQuery = query
                        onAction(MainAction.Search(query))
                    },
                    onSearchClose = {
                        searchQuery = ""
                        onAction(MainAction.Search(""))
                        showSearch = false
                    },
                    onSearchToggle = { show: Boolean -> showSearch = show },
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onAction = onAction,
                    onMoreMenuAction = { action ->
                        when (action) {
                            MainMoreMenuAction.RestartService -> onAction(MainAction.RestartService)
                            MainMoreMenuAction.DeleteAll -> showDelAllConfirm = true
                            MainMoreMenuAction.DeleteDuplicate -> showDelDuplicateConfirm = true
                            MainMoreMenuAction.DeleteInvalid -> showDelInvalidConfirm = true
                            MainMoreMenuAction.LocateSelected -> onAction(MainAction.LocateSelectedServer)
                            MainMoreMenuAction.SortByTestResults -> onAction(MainAction.SortByTestResults)
                            MainMoreMenuAction.TestAll -> onAction(MainAction.TestAllServers)
                            MainMoreMenuAction.TestAllRealPing -> onAction(MainAction.TestRealAllServers)
                            MainMoreMenuAction.UpdateSubscriptions -> onAction(MainAction.UpdateSubscriptions)
                        }
                    }
                )
            },
            bottomBar = {
                if (modePagerState.currentPage == 1) {
                    MainBottomBar(
                        displayText = uiState.statusText,
                        isRunning = isRunning,
                        isDarkTheme = isSystemInDarkTheme(),
                        onAction = onAction,
                        onSmartConnect = onSmartConnect
                    )
                }
            },
            floatingActionButton = {},
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                MobileTinaModeTabs(
                    selectedPage = modePagerState.currentPage,
                    onAutomaticClick = {
                        scope.launch { modePagerState.animateScrollToPage(0) }
                    },
                    onManualClick = {
                        scope.launch { modePagerState.animateScrollToPage(1) }
                    }
                )

                HorizontalPager(
                    state = modePagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = true,
                    beyondViewportPageCount = 1
                ) { modePage ->
                    when (modePage) {
                        0 -> {
                            PullToRefreshBox(
                                isRefreshing = isLoading,
                                onRefresh = { onAction(MainAction.UpdateSubscriptions) },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    MobileTinaDashboard(
                                        isRunning = isRunning,
                                        smartConnecting = smartConnecting,
                                        smartCountdownSeconds = smartCountdownSeconds,
                                        smartConnectionFailed = smartConnectionFailed,
                                        selectedSubscriptionId = uiState.selectedGroupId,
                                        selectedServerName = selectedProfile?.remarks.orEmpty(),
                                        selectedServerDetails = selectedProfile?.server.orEmpty(),
                                        selectedPingMillis = selectedPing,
                                        onToggle = onSmartConnect,
                                        onTestPing = { onAction(MainAction.TestCurrentServer) }
                                    )
                                }
                            }
                        }

                        1 -> {
                            PullToRefreshBox(
                                isRefreshing = isLoading,
                                onRefresh = { onAction(MainAction.UpdateSubscriptions) },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    if (groups.isNotEmpty()) {
                                        GroupTabBar(
                                            groups = groups,
                                            selectedTabIndex = groupPagerState.currentPage.coerceIn(0, groups.lastIndex),
                                            mainViewModel = mainViewModel,
                                            onTabClick = { targetIndex ->
                                                scope.launch {
                                                    groupPagerState.navigateToPageOptimized(
                                                        targetPage = targetIndex,
                                                        animateAdjacentPage = true
                                                    )
                                                }
                                            }
                                        )
                                    }

                                    if (groups.isNotEmpty()) {
                                        HorizontalPager(
                                            state = groupPagerState,
                                            modifier = Modifier.fillMaxSize(),
                                            userScrollEnabled = false,
                                            beyondViewportPageCount = 1,
                                            key = { page -> groups.getOrNull(page)?.id ?: "group-page-$page" }
                                        ) { page ->
                                            val group = groups.getOrNull(page) ?: return@HorizontalPager

                                            GroupPagerPage(
                                                groupId = group.id,
                                                mainViewModel = mainViewModel,
                                                selectedGuid = selectedGuid,
                                                doubleColumnDisplay = doubleColumnDisplay,
                                                confirmRemove = confirmRemove,
                                                searchQuery = searchQuery,
                                                lazyListStates = lazyListStates,
                                                lazyGridStates = lazyGridStates,
                                                onSelectServer = { guid -> onAction(MainAction.SelectServer(guid)) },
                                                onRemoveServer = removeServer,
                                                contentPadding = PaddingValues(
                                                    start = 0.dp,
                                                    top = 4.dp,
                                                    end = 0.dp,
                                                    bottom = 16.dp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MobileTinaModeTabs(
    selectedPage: Int,
    onAutomaticClick: () -> Unit,
    onManualClick: () -> Unit
) {
    // Force the visual order requested by the product design: Manual on the left, Auto on the right.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            ModeTab(
                text = stringResource(R.string.mobiletina_mode_manual),
                selected = selectedPage == 1,
                onClick = onManualClick,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            ModeTab(
                text = stringResource(R.string.mobiletina_mode_auto),
                selected = selectedPage == 0,
                onClick = onAutomaticClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ModeTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        tonalElevation = if (selected) 2.dp else 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 11.dp, horizontal = 8.dp),
            textAlign = TextAlign.Center,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
