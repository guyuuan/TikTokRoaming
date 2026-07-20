package com.guyuuan.xposed.tiktokroaming.settings

import android.content.SharedPreferences
import io.github.libxposed.service.XposedService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteSettingsRepository(
  private val serviceFlow: StateFlow<XposedService?>,
) : SettingsRepository {
  override val state: Flow<SettingsConnection> =
    serviceFlow
      .flatMapLatest { service ->
        if (service == null) {
          flowOf(SettingsConnection.Disconnected)
        } else {
          observePreferences(service)
        }
      }
      .distinctUntilChanged()

  override fun selectProfile(profileId: String) {
    edit { putString(ModulePreferenceKeys.PROFILE_ID, profileId) }
  }

  override fun setEnabled(enabled: Boolean) {
    edit { putBoolean(ModulePreferenceKeys.ENABLED, enabled) }
  }

  override fun setHideVpn(hideVpn: Boolean) {
    edit { putBoolean(ModulePreferenceKeys.HIDE_VPN, hideVpn) }
  }

  private fun edit(block: SharedPreferences.Editor.() -> Unit) {
    val service = serviceFlow.value ?: return
    val editor = service.getRemotePreferences(ModulePreferenceKeys.GROUP).edit() ?: return
    editor.block()
    editor.apply()
  }

  private fun observePreferences(service: XposedService): Flow<SettingsConnection> = callbackFlow {
    val preferences = service.getRemotePreferences(ModulePreferenceKeys.GROUP)
    fun publish() {
      trySend(
        SettingsConnection.Connected(
          settings = preferences.readModuleSettings(),
          frameworkName = service.frameworkName,
          apiVersion = service.apiVersion,
        )
      )
    }

    val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> publish() }
    preferences.registerOnSharedPreferenceChangeListener(listener)
    publish()
    awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
  }
}
