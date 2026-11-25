package com.vidrierapp.presentation.detail

import com.vidrierapp.domain.model.Shop
import com.vidrierapp.domain.network.FakeNetworkMonitor
import com.vidrierapp.domain.repository.ShopRepository
import com.vidrierapp.domain.model.PASS_DURATION_MILLIS
import com.vidrierapp.domain.model.PassState
import com.vidrierapp.domain.usecase.FakePassRepository
import com.vidrierapp.domain.usecase.FakeReportedShopsRepository
import com.vidrierapp.domain.usecase.ObserveReportedShopsUseCase
import com.vidrierapp.domain.usecase.ReportRentedUseCase
import com.vidrierapp.domain.usecase.GetShopDetailUseCase
import com.vidrierapp.domain.usecase.ObservePassAccessUseCase
import com.vidrierapp.domain.usecase.PurchasePassUseCase
import com.vidrierapp.domain.usecase.StartTrialUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShopDetailViewModelTest {

    private class FakeShopRepository : ShopRepository {
        var result: Result<Shop> = Result.failure(IllegalStateException())
        val requestedIds = mutableListOf<String>()
        val flowsById = mutableMapOf<String, MutableSharedFlow<Result<Shop>>>()
        var reportResult: Result<Unit> = Result.success(Unit)
        val reportedIds = mutableListOf<String>()

        override suspend fun reportRented(shopId: String): Result<Unit> {
            reportedIds.add(shopId)
            return reportResult
        }

        override fun getShops(categoryFilter: String?): Flow<Result<List<Shop>>> =
            flowOf(Result.failure(UnsupportedOperationException()))

        override fun getShopDetail(shopId: String): Flow<Result<Shop>> {
            requestedIds.add(shopId)
            return flowsById[shopId] ?: flowOf(result)
        }
    }

    private var now = 1_000L
    private val passRepository = FakePassRepository(PassState(trialStartedAt = 0L))

    private val reportedShops = FakeReportedShopsRepository()

    private val networkMonitor = FakeNetworkMonitor()

    private fun viewModel(repository: ShopRepository) = ShopDetailViewModel(
        getShopDetailUseCase = GetShopDetailUseCase(repository),
        observePassAccessUseCase = ObservePassAccessUseCase(passRepository) { now },
        startTrialUseCase = StartTrialUseCase(passRepository) { now },
        purchasePassUseCase = PurchasePassUseCase(passRepository) { now },
        reportRentedUseCase = ReportRentedUseCase(repository, reportedShops),
        observeReportedShopsUseCase = ObserveReportedShopsUseCase(reportedShops),
        networkMonitor = networkMonitor
    )

    private fun shop(id: String, votes: Int = 20) = Shop(
        id = id,
        address = "address $id",
        neighborhood = "neighborhood",
        latitude = -34.65,
        longitude = -58.50,
        surfaceAreaM2 = 50,
        rentalPrice = "100",
        topCategory = null,
        totalVotes = votes
    )

    private fun loadedShop(votes: Int): Shop {
        val repository = FakeShopRepository().apply { result = Result.success(shop("1", votes)) }
        val viewModel = viewModel(repository)
        viewModel.loadShop("1")
        return (viewModel.uiState.value as ShopDetailUiState.Success).shop
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        collectors.forEach { it.cancel() }
        Dispatchers.resetMain()
    }

    @Test
    fun `loadShop emits Success with active pass`() {
        val repository = FakeShopRepository().apply { result = Result.success(shop("1")) }
        val viewModel = viewModel(repository)

        viewModel.loadShop("1")

        assertEquals(
            ShopDetailUiState.Success(shop = shop("1"), hasActivePass = true, trialAvailable = false),
            viewModel.uiState.value
        )
    }

    @Test
    fun `loaded shop flags insufficient sample with 0 votes`() {
        assertFalse(loadedShop(0).isSampleSufficient)
    }

    @Test
    fun `loaded shop flags insufficient sample with 14 votes`() {
        assertFalse(loadedShop(14).isSampleSufficient)
    }

    @Test
    fun `loaded shop has sufficient sample with 15 votes`() {
        assertTrue(loadedShop(15).isSampleSufficient)
    }

    @Test
    fun `loaded shop has sufficient sample with 16 votes`() {
        assertTrue(loadedShop(16).isSampleSufficient)
    }

    @Test
    fun `loadShop emits Error when loading fails`() {
        val repository = FakeShopRepository().apply {
            result = Result.failure(IllegalStateException("boom"))
        }
        val viewModel = viewModel(repository)

        viewModel.loadShop("1")

        assertEquals(ShopDetailUiState.Error("boom"), viewModel.uiState.value)
    }

    @Test
    fun `retry reloads the last shop after a failure`() {
        val repository = FakeShopRepository().apply {
            result = Result.failure(IllegalStateException("boom"))
        }
        val viewModel = viewModel(repository)
        viewModel.loadShop("1")

        repository.result = Result.success(shop("1"))
        viewModel.retry()

        assertTrue(viewModel.uiState.value is ShopDetailUiState.Success)
        assertEquals(listOf("1", "1"), repository.requestedIds)
    }

    @Test
    fun `retry without a previous load does nothing`() {
        val repository = FakeShopRepository()
        val viewModel = viewModel(repository)

        viewModel.retry()

        assertEquals(ShopDetailUiState.Loading, viewModel.uiState.value)
        assertTrue(repository.requestedIds.isEmpty())
    }

    @Test
    fun `loading another shop cancels the previous collection`() {
        val first = MutableSharedFlow<Result<Shop>>(extraBufferCapacity = 1)
        val second = MutableSharedFlow<Result<Shop>>(extraBufferCapacity = 1)
        val repository = FakeShopRepository().apply {
            flowsById["1"] = first
            flowsById["2"] = second
        }
        val viewModel = viewModel(repository)

        viewModel.loadShop("1")
        viewModel.loadShop("2")

        assertEquals(0, first.subscriptionCount.value)
        assertEquals(1, second.subscriptionCount.value)

        second.tryEmit(Result.success(shop("2")))
        first.tryEmit(Result.success(shop("1")))

        assertEquals(
            ShopDetailUiState.Success(shop = shop("2"), hasActivePass = true, trialAvailable = false),
            viewModel.uiState.value
        )
    }

    @Test
    fun `loadShop is locked on a fresh install`() {
        passRepository.state.value = PassState()
        val repository = FakeShopRepository().apply { result = Result.success(shop("1")) }
        val viewModel = viewModel(repository)

        viewModel.loadShop("1")

        assertEquals(
            ShopDetailUiState.Success(shop("1"), hasActivePass = false, trialAvailable = true),
            viewModel.uiState.value
        )
    }

    @Test
    fun `loadShop is locked when the trial has expired`() {
        now = 4L * 24 * 60 * 60 * 1000
        val repository = FakeShopRepository().apply { result = Result.success(shop("1")) }
        val viewModel = viewModel(repository)

        viewModel.loadShop("1")

        assertEquals(
            ShopDetailUiState.Success(shop("1"), hasActivePass = false, trialAvailable = false),
            viewModel.uiState.value
        )
    }

    @Test
    fun `startTrial unlocks the loaded shop`() {
        passRepository.state.value = PassState()
        val repository = FakeShopRepository().apply { result = Result.success(shop("1")) }
        val viewModel = viewModel(repository)
        viewModel.loadShop("1")

        viewModel.startTrial()

        assertEquals(
            ShopDetailUiState.Success(shop("1"), hasActivePass = true, trialAvailable = false),
            viewModel.uiState.value
        )
        assertEquals(1_000L, passRepository.state.value.trialStartedAt)
    }

    @Test
    fun `purchasePass unlocks the loaded shop after the trial expired`() {
        now = 4L * 24 * 60 * 60 * 1000
        val repository = FakeShopRepository().apply { result = Result.success(shop("1")) }
        val viewModel = viewModel(repository)
        viewModel.loadShop("1")

        viewModel.purchasePass()

        assertEquals(
            ShopDetailUiState.Success(shop("1"), hasActivePass = true, trialAvailable = false),
            viewModel.uiState.value
        )
        assertEquals(now + PASS_DURATION_MILLIS, passRepository.state.value.passExpiresAt)
    }

    private val collectors = mutableListOf<kotlinx.coroutines.Job>()

    private fun collectFeedback(viewModel: ShopDetailViewModel): List<ReportRentedFeedback> {
        val events = mutableListOf<ReportRentedFeedback>()
        collectors += CoroutineScope(Dispatchers.Main).launch {
            viewModel.feedback.collect { events.add(it) }
        }
        return events
    }

    @Test
    fun `report click opens the dialog and dismiss closes it`() {
        val viewModel = viewModel(FakeShopRepository())
        viewModel.loadShop("1")

        viewModel.onReportRentedClick()
        assertTrue(viewModel.reportState.value.isDialogVisible)

        viewModel.dismissReportDialog()
        assertEquals(ReportRentedUiState(), viewModel.reportState.value)
    }

    @Test
    fun `confirming a report closes the dialog emits success and disables the button`() {
        val repository = FakeShopRepository()
        val viewModel = viewModel(repository)
        val events = collectFeedback(viewModel)
        viewModel.loadShop("1")
        viewModel.onReportRentedClick()

        viewModel.confirmReportRented()

        assertEquals(listOf("1"), repository.reportedIds)
        assertEquals(listOf(ReportRentedFeedback.Success), events)
        assertEquals(
            ReportRentedUiState(isDialogVisible = false, isSubmitting = false, alreadyReported = true),
            viewModel.reportState.value
        )
    }

    @Test
    fun `failed report emits failure keeps the button enabled and can be retried`() {
        val repository = FakeShopRepository().apply {
            reportResult = Result.failure(IllegalStateException("boom"))
        }
        val viewModel = viewModel(repository)
        val events = collectFeedback(viewModel)
        viewModel.loadShop("1")

        viewModel.confirmReportRented()
        assertEquals(listOf(ReportRentedFeedback.Failure), events)
        assertEquals(false, viewModel.reportState.value.alreadyReported)

        repository.reportResult = Result.success(Unit)
        viewModel.confirmReportRented()

        assertEquals(listOf(ReportRentedFeedback.Failure, ReportRentedFeedback.Success), events)
        assertEquals(listOf("1", "1"), repository.reportedIds)
        assertTrue(viewModel.reportState.value.alreadyReported)
    }

    @Test
    fun `report click on an already reported shop emits already reported and no dialog`() {
        runBlocking { reportedShops.markReported("1") }
        val viewModel = viewModel(FakeShopRepository())
        val events = collectFeedback(viewModel)
        viewModel.loadShop("1")

        viewModel.onReportRentedClick()

        assertEquals(listOf(ReportRentedFeedback.AlreadyReported), events)
        assertEquals(false, viewModel.reportState.value.isDialogVisible)
        assertTrue(viewModel.reportState.value.alreadyReported)
    }

    @Test
    fun `a report of another shop does not disable this one`() {
        reportedShops.state.value = setOf("2")
        val viewModel = viewModel(FakeShopRepository())
        viewModel.loadShop("1")

        assertEquals(false, viewModel.reportState.value.alreadyReported)
    }

    @Test
    fun `confirm without a loaded shop does nothing`() {
        val repository = FakeShopRepository()
        val viewModel = viewModel(repository)

        viewModel.confirmReportRented()

        assertTrue(repository.reportedIds.isEmpty())
    }

    @Test
    fun `failure while online is not flagged offline`() {
        val repository = FakeShopRepository()
        val vm = viewModel(repository)

        vm.loadShop("a")

        assertEquals(false, (vm.uiState.value as ShopDetailUiState.Error).isOffline)
    }

    @Test
    fun `failure while offline is flagged offline and retry re-requests the same shop`() {
        val repository = FakeShopRepository()
        networkMonitor.online.value = false
        val vm = viewModel(repository)

        vm.loadShop("a")
        assertEquals(true, (vm.uiState.value as ShopDetailUiState.Error).isOffline)

        networkMonitor.online.value = true
        repository.result = Result.success(shop("a"))
        vm.retry()

        assertEquals(listOf("a", "a"), repository.requestedIds)
        assertTrue(vm.uiState.value is ShopDetailUiState.Success)
    }
}
