package com.guyuuan.xposed.tiktokroaming

import android.app.Application
import com.guyuuan.xposed.tiktokroaming.settings.XposedServiceRegistry
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

class TikTokRoamingApp : Application(), XposedServiceHelper.OnServiceListener {
  override fun onCreate() {
    super.onCreate()
    XposedServiceHelper.registerListener(this)
  }

  override fun onServiceBind(service: XposedService) {
    XposedServiceRegistry.attach(service)
  }

  override fun onServiceDied(service: XposedService) {
    XposedServiceRegistry.detach(service)
  }
}
