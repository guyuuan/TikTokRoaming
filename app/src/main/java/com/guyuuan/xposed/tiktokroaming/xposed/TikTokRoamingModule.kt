package com.guyuuan.xposed.tiktokroaming.xposed

import android.content.SharedPreferences
import android.util.Log
import com.guyuuan.xposed.tiktokroaming.settings.ModulePreferenceKeys
import com.guyuuan.xposed.tiktokroaming.settings.ModuleSettings
import com.guyuuan.xposed.tiktokroaming.settings.TikTokTargets
import com.guyuuan.xposed.tiktokroaming.settings.readModuleSettings
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class TikTokRoamingModule : XposedModule() {
  private val settings = AtomicReference(ModuleSettings())
  private val hooksInstalled = AtomicBoolean(false)
  private var preferences: SharedPreferences? = null
  private var preferenceListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

  override fun onModuleLoaded(param: ModuleLoadedParam) {
    log(Log.INFO, TAG, "Loaded in ${param.processName} with $frameworkName API $apiVersion")
  }

  override fun onPackageReady(param: PackageReadyParam) {
    if (!param.isFirstPackage || param.packageName !in TikTokTargets.packages) return
    if (!hooksInstalled.compareAndSet(false, true)) return

    val remotePreferences = getRemotePreferences(ModulePreferenceKeys.GROUP)
    preferences = remotePreferences
    refreshSettings(remotePreferences)

    val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, _ ->
      refreshSettings(sharedPreferences)
    }
    preferenceListener = listener
    remotePreferences.registerOnSharedPreferenceChangeListener(listener)

    TikTokHookInstaller(this, settings::get).install()
    log(Log.INFO, TAG, "Hooks installed for ${param.packageName}")
  }

  private fun refreshSettings(preferences: SharedPreferences) {
    val updated = preferences.readModuleSettings()
    settings.set(updated)
    log(
      Log.INFO,
      TAG,
      "Settings updated: enabled=${updated.enabled}, profile=${updated.profile.id}, hideVpn=${updated.hideVpn}",
    )
  }

  private companion object {
    const val TAG = "TikTokRoaming"
  }
}
