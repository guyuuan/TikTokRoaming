package com.guyuuan.xposed.tiktokroaming.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guyuuan.xposed.tiktokroaming.model.CountryProfile
import com.guyuuan.xposed.tiktokroaming.model.CountryProfiles
import com.guyuuan.xposed.tiktokroaming.settings.LauncherIconController
import com.guyuuan.xposed.tiktokroaming.settings.ModuleSettings
import com.guyuuan.xposed.tiktokroaming.settings.SettingsConnection
import com.guyuuan.xposed.tiktokroaming.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class MainScreenViewModel(
  private val repository: SettingsRepository,
  private val launcherIconController: LauncherIconController,
) : ViewModel() {
  val uiState: StateFlow<MainScreenUiState> =
    combine(repository.state, launcherIconController.isHidden) { connection, launcherIconHidden ->
        val uiState: MainScreenUiState =
          when (connection) {
            SettingsConnection.Disconnected ->
              MainScreenUiState.Ready(
                serviceConnected = false,
                frameworkLabel = "LSPosed 服务未连接",
                settings = ModuleSettings(),
                selectedProfile = CountryProfiles.default,
                launcherIconHidden = launcherIconHidden,
              )
            is SettingsConnection.Connected ->
              MainScreenUiState.Ready(
                serviceConnected = true,
                frameworkLabel = "${connection.frameworkName} · API ${connection.apiVersion}",
                settings = connection.settings,
                selectedProfile = connection.settings.profile,
                launcherIconHidden = launcherIconHidden,
              )
          }
        uiState
      }
      .catch { emit(MainScreenUiState.Error(it)) }
      .stateIn(viewModelScope, SharingStarted.Eagerly, MainScreenUiState.Loading)

  fun selectProfile(profile: CountryProfile) = repository.selectProfile(profile.id)

  fun setEnabled(enabled: Boolean) = repository.setEnabled(enabled)

  fun setHideVpn(hideVpn: Boolean) = repository.setHideVpn(hideVpn)

  fun setLauncherIconHidden(hidden: Boolean) = launcherIconController.setHidden(hidden)

  fun refreshLauncherIconState() = launcherIconController.refresh()
}

sealed interface MainScreenUiState {
  data object Loading : MainScreenUiState

  data class Error(val throwable: Throwable) : MainScreenUiState

  data class Ready(
    val serviceConnected: Boolean,
    val frameworkLabel: String,
    val settings: ModuleSettings,
    val selectedProfile: CountryProfile,
    val launcherIconHidden: Boolean,
    val profiles: List<CountryProfile> = CountryProfiles.all,
  ) : MainScreenUiState
}
