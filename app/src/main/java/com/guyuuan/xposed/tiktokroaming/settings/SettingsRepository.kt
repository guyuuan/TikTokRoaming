package com.guyuuan.xposed.tiktokroaming.settings

import kotlinx.coroutines.flow.Flow

sealed interface SettingsConnection {
  data object Disconnected : SettingsConnection

  data class Connected(
    val settings: ModuleSettings,
    val frameworkName: String,
    val apiVersion: Int,
  ) : SettingsConnection
}

interface SettingsRepository {
  val state: Flow<SettingsConnection>

  fun selectProfile(profileId: String)

  fun setEnabled(enabled: Boolean)

  fun setHideVpn(hideVpn: Boolean)
}
