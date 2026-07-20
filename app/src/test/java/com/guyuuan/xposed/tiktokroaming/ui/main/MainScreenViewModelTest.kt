package com.guyuuan.xposed.tiktokroaming.ui.main

import com.guyuuan.xposed.tiktokroaming.model.CountryProfiles
import com.guyuuan.xposed.tiktokroaming.settings.ModuleSettings
import com.guyuuan.xposed.tiktokroaming.settings.SettingsConnection
import com.guyuuan.xposed.tiktokroaming.settings.SettingsRepository
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MainScreenViewModelTest {
  @Test
  fun connectedRepository_isShownWithSelectedCountry() = runTest {
    val repository = FakeSettingsRepository()
    repository.connection.value =
      SettingsConnection.Connected(
        settings = ModuleSettings(profileId = "JP", enabled = true, hideVpn = true),
        frameworkName = "LSPosed",
        apiVersion = 102,
      )
    val viewModel = MainScreenViewModel(repository)

    val state = viewModel.uiState.filterIsInstance<MainScreenUiState.Ready>().first()
    assertEquals("JP", state.selectedProfile.id)
    assertEquals("LSPosed · API 102", state.frameworkLabel)
  }

  @Test
  fun selectingCountry_updatesRepository() = runTest {
    val repository = FakeSettingsRepository()
    val viewModel = MainScreenViewModel(repository)

    viewModel.selectProfile(CountryProfiles.byId("SG"))

    assertEquals("SG", repository.selectedProfileId)
  }
}

private class FakeSettingsRepository : SettingsRepository {
  val connection = MutableStateFlow<SettingsConnection>(SettingsConnection.Disconnected)
  override val state: StateFlow<SettingsConnection> = connection
  var selectedProfileId: String? = null

  override fun setEnabled(enabled: Boolean) = Unit

  override fun setHideVpn(hideVpn: Boolean) = Unit

  override fun selectProfile(profileId: String) {
    selectedProfileId = profileId
  }
}
