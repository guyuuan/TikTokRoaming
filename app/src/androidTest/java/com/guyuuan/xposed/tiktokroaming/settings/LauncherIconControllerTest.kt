package com.guyuuan.xposed.tiktokroaming.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.guyuuan.xposed.tiktokroaming.MainActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherIconControllerTest {
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val packageManager = context.packageManager
  private val launcherComponent =
    ComponentName(context.packageName, "${context.packageName}.LauncherActivity")

  @After
  fun restoreLauncherIcon() {
    packageManager.setComponentEnabledSetting(
      launcherComponent,
      PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
      PackageManager.DONT_KILL_APP,
    )
  }

  @Test
  fun hidingLauncherAlias_persistsAndKeepsInfoEntryAvailable() {
    val controller = PackageManagerLauncherIconController(context)

    controller.setHidden(true)

    assertTrue(controller.isHidden.value)
    assertEquals(
      PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
      packageManager.getComponentEnabledSetting(launcherComponent),
    )
    assertTrue(PackageManagerLauncherIconController(context).isHidden.value)

    val packageLaunchIntent = packageManager.getLaunchIntentForPackage(context.packageName)
    assertNotNull(packageLaunchIntent)
    assertEquals(ComponentName(context, MainActivity::class.java), packageLaunchIntent?.component)

    controller.setHidden(false)

    assertFalse(controller.isHidden.value)
    assertEquals(
      PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
      packageManager.getComponentEnabledSetting(launcherComponent),
    )
  }
}
