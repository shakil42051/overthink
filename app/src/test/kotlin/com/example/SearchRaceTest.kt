package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.ui.OverthinkViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SearchRaceTest {
    @Test
    fun resetCancelsPendingSearch() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val vm = OverthinkViewModel(ApplicationProvider.getApplicationContext<Application>())
        try {
            vm.searchGateway =
                object : SearchGateway {
                    override suspend fun search(
                        query: String,
                        kind: ItemKind,
                        page: Int,
                        filter: SearchFilter,
                    ): SearchPage {
                        withContext(NonCancellable) { delay(1000) }
                        return SearchPage(
                            listOf(ResearchItem("old", "Old result")),
                            emptyList(),
                            false,
                        )
                    }
                }
            vm.query("pending")
            vm.search()
            runCurrent()
            vm.resetCustomization()
            advanceUntilIdle()
            vm.awaitSearchIdle()
            assertTrue(vm.discovery.value.items.isEmpty())
            assertEquals("", vm.discovery.value.query)
            assertFalse(vm.discovery.value.loading)
        } finally {
            vm.store.close()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun historyOptOutWorksDuringOtherOperations() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val vm = OverthinkViewModel(ApplicationProvider.getApplicationContext<Application>())
        try {
            vm.store.update { it.copy(history = listOf("private query")) }
            vm.busy.value = true
            vm.customize { it.copy(rememberHistory = false) }
            vm.clearHistory().join()
            assertTrue(vm.workspace.value.history.isEmpty())
            vm.searchGateway =
                object : SearchGateway {
                    override suspend fun search(
                        query: String,
                        kind: ItemKind,
                        page: Int,
                        filter: SearchFilter,
                    ) = SearchPage(emptyList(), emptyList(), false)
                }
            vm.query("not remembered")
            vm.search()
            advanceUntilIdle()
            vm.awaitSearchIdle()
            assertTrue(vm.workspace.value.history.isEmpty())
        } finally {
            vm.store.close()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun cancelledOlderSearchNeverReplacesANewerQuery() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val vm = OverthinkViewModel(ApplicationProvider.getApplicationContext<Application>())
        try {
            vm.searchGateway =
                object : SearchGateway {
                    override suspend fun search(
                        query: String,
                        kind: ItemKind,
                        page: Int,
                        filter: SearchFilter,
                    ): SearchPage {
                        if (query == "slow") withContext(NonCancellable) { delay(1000) }
                        else delay(10)
                        return SearchPage(listOf(ResearchItem(query, query)), emptyList(), false)
                    }
                }
            vm.query("slow")
            vm.search()
            runCurrent()
            vm.query("new")
            vm.search()
            advanceUntilIdle()
            vm.awaitSearchIdle()
            assertEquals("new", vm.discovery.value.query)
            assertEquals(listOf("new"), vm.discovery.value.items.map { it.id })
            assertFalse(vm.discovery.value.loading)
        } finally {
            vm.store.close()
            Dispatchers.resetMain()
        }
    }
}
