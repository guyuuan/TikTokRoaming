package com.guyuuan.xposed.tiktokroaming.settings

import com.guyuuan.xposed.tiktokroaming.model.CountryProfile
import com.guyuuan.xposed.tiktokroaming.model.CountryProfiles

data class ModuleSettings(
  val profileId: String = CountryProfiles.default.id,
  val enabled: Boolean = true,
  val hideVpn: Boolean = true,
) {
  val profile: CountryProfile
    get() = CountryProfiles.byId(profileId)
}

object ModulePreferenceKeys {
  const val GROUP = "tiktok_roaming"
  const val PROFILE_ID = "profile_id"
  const val ENABLED = "enabled"
  const val HIDE_VPN = "hide_vpn"
}

object TikTokTargets {
  val packages = setOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")
}
