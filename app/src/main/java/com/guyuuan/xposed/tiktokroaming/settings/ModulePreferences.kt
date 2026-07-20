package com.guyuuan.xposed.tiktokroaming.settings

import android.content.SharedPreferences
import com.guyuuan.xposed.tiktokroaming.model.CountryProfiles

fun SharedPreferences.readModuleSettings(): ModuleSettings =
  ModuleSettings(
    profileId = getString(ModulePreferenceKeys.PROFILE_ID, CountryProfiles.default.id) ?: CountryProfiles.default.id,
    enabled = getBoolean(ModulePreferenceKeys.ENABLED, true),
    hideVpn = getBoolean(ModulePreferenceKeys.HIDE_VPN, true),
  )
