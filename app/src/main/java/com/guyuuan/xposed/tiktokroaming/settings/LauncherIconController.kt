package com.guyuuan.xposed.tiktokroaming.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface LauncherIconController {
  val isHidden: StateFlow<Boolean>

  fun setHidden(hidden: Boolean)

  fun refresh()
}

class PackageManagerLauncherIconController(context: Context) : LauncherIconController {
  private val packageManager = context.packageManager
  private val launcherComponent =
    ComponentName(context.packageName, "${context.packageName}.LauncherActivity")
  private val mutableIsHidden = MutableStateFlow(readIsHidden())

  override val isHidden: StateFlow<Boolean> = mutableIsHidden.asStateFlow()

  override fun setHidden(hidden: Boolean) {
    packageManager.setComponentEnabledSetting(
      launcherComponent,
      if (hidden) {
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
      } else {
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
      },
      PackageManager.DONT_KILL_APP,
    )
    refresh()
  }

  override fun refresh() {
    mutableIsHidden.value = readIsHidden()
  }

  private fun readIsHidden(): Boolean =
    when (packageManager.getComponentEnabledSetting(launcherComponent)) {
      PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
      PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
      PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED -> true
      else -> false
    }
}
