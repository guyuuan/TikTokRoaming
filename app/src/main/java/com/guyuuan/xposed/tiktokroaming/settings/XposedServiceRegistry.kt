package com.guyuuan.xposed.tiktokroaming.settings

import io.github.libxposed.service.XposedService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object XposedServiceRegistry {
  private val mutableService = MutableStateFlow<XposedService?>(null)

  val service: StateFlow<XposedService?> = mutableService.asStateFlow()

  fun attach(service: XposedService) {
    mutableService.value = service
  }

  fun detach(service: XposedService) {
    if (mutableService.value === service) mutableService.value = null
  }
}
