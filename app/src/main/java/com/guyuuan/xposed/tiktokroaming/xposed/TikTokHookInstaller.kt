@file:Suppress("DEPRECATION")

package com.guyuuan.xposed.tiktokroaming.xposed

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.content.res.Resources
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.LocaleList
import android.telephony.SubscriptionInfo
import android.telephony.TelephonyManager
import android.util.Log
import com.guyuuan.xposed.tiktokroaming.settings.ModuleSettings
import io.github.libxposed.api.XposedInterface.Chain
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.net.NetworkInterface
import java.time.ZoneId
import java.util.Collections
import java.util.Locale
import java.util.TimeZone

internal class TikTokHookInstaller(
  private val module: XposedModule,
  private val currentSettings: () -> ModuleSettings,
) {
  fun install() {
    installTelephonyHooks()
    installLocaleHooks()
    installTimeZoneHooks()
    installSystemPropertyHooks()
    installVpnHooks()
  }

  private fun installTelephonyHooks() = safely("telephony") {
    val countryIso: (Chain, ModuleSettings) -> Any? = { _, settings ->
      settings.profile.countryIso.lowercase(Locale.ROOT)
    }
    hookMethods(TelephonyManager::class.java, "getSimCountryIso", interceptor = countryIso)
    hookMethods(TelephonyManager::class.java, "getNetworkCountryIso", interceptor = countryIso)
    hookMethods(SubscriptionInfo::class.java, "getCountryIso", interceptor = countryIso)

    val operatorNumeric: (Chain, ModuleSettings) -> Any? = { _, settings ->
      settings.profile.operatorNumeric
    }
    hookMethods(TelephonyManager::class.java, "getSimOperator", interceptor = operatorNumeric)
    hookMethods(TelephonyManager::class.java, "getNetworkOperator", interceptor = operatorNumeric)

    hookMethods(SubscriptionInfo::class.java, "getMcc") { _, settings ->
      settings.profile.operatorNumeric.take(3).toInt()
    }
    hookMethods(SubscriptionInfo::class.java, "getMccString") { _, settings ->
      settings.profile.operatorNumeric.take(3)
    }
    hookMethods(SubscriptionInfo::class.java, "getMnc") { _, settings ->
      settings.profile.operatorNumeric.drop(3).toInt()
    }
    hookMethods(SubscriptionInfo::class.java, "getMncString") { _, settings ->
      settings.profile.operatorNumeric.drop(3)
    }
  }

  @SuppressLint("AppBundleLocaleChanges")
  private fun installLocaleHooks() = safely("locale") {
    hookMethods(
      type = Locale::class.java,
      name = "getDefault",
      predicate = { Modifier.isStatic(it.modifiers) && it.returnType == Locale::class.java },
    ) { _, settings -> settings.profile.locale }

    hookMethods(LocaleList::class.java, "getDefault") { _, settings ->
      LocaleList(settings.profile.locale)
    }
    hookMethods(LocaleList::class.java, "getAdjustedDefault") { _, settings ->
      LocaleList(settings.profile.locale)
    }
    hookMethods(Configuration::class.java, "getLocales") { _, settings ->
      LocaleList(settings.profile.locale)
    }
    hookMethods(Resources::class.java, "getConfiguration") { chain, settings ->
      val original = chain.proceed() as? Configuration ?: return@hookMethods null
      Configuration(original).apply {
        setLocale(settings.profile.locale)
        setLocales(LocaleList(settings.profile.locale))
      }
    }
  }

  private fun installTimeZoneHooks() = safely("time zone") {
    hookMethods(
      type = TimeZone::class.java,
      name = "getDefault",
      predicate = { Modifier.isStatic(it.modifiers) && it.parameterCount == 0 },
    ) { _, settings -> TimeZone.getTimeZone(settings.profile.timeZoneId) }
    hookMethods(
      type = ZoneId::class.java,
      name = "systemDefault",
      predicate = { Modifier.isStatic(it.modifiers) && it.parameterCount == 0 },
    ) { _, settings -> ZoneId.of(settings.profile.timeZoneId) }
    hookMethods(
      type = android.icu.util.TimeZone::class.java,
      name = "getDefault",
      predicate = { Modifier.isStatic(it.modifiers) && it.parameterCount == 0 },
    ) { _, settings -> android.icu.util.TimeZone.getTimeZone(settings.profile.timeZoneId) }
  }

  @SuppressLint("PrivateApi")
  private fun installSystemPropertyHooks() = safely("system properties") {
    val systemProperties = Class.forName("android.os.SystemProperties")
    hookMethods(
      type = systemProperties,
      name = "get",
      predicate = {
        Modifier.isStatic(it.modifiers) &&
          it.returnType == String::class.java &&
          it.parameterTypes.firstOrNull() == String::class.java
      },
    ) { chain, settings ->
      when (chain.getArg(0) as String) {
        "persist.sys.locale", "ro.product.locale" -> settings.profile.languageTag
        "ro.product.locale.language" -> settings.profile.locale.language
        "ro.product.locale.region" -> settings.profile.countryIso
        "persist.sys.timezone" -> settings.profile.timeZoneId
        else -> chain.proceed()
      }
    }
  }

  private fun installVpnHooks() = safely("VPN") {
    hookMethods(NetworkCapabilities::class.java, "hasTransport") { chain, settings ->
      if (settings.hideVpn && chain.getArg(0) == NetworkCapabilities.TRANSPORT_VPN) false
      else chain.proceed()
    }
    hookMethods(NetworkCapabilities::class.java, "hasCapability") { chain, settings ->
      if (settings.hideVpn && chain.getArg(0) == NetworkCapabilities.NET_CAPABILITY_NOT_VPN) true
      else chain.proceed()
    }
    hookMethods(NetworkInfo::class.java, "getType") { chain, settings ->
      val type = chain.proceed() as Int
      if (settings.hideVpn && type == ConnectivityManager.TYPE_VPN) ConnectivityManager.TYPE_WIFI else type
    }
    hookMethods(NetworkInfo::class.java, "getTypeName") { chain, settings ->
      val typeName = chain.proceed() as? String
      if (settings.hideVpn && typeName.equals("VPN", ignoreCase = true)) "WIFI" else typeName
    }
    hookMethods(
      type = NetworkInterface::class.java,
      name = "getByName",
      predicate = { Modifier.isStatic(it.modifiers) && it.parameterTypes.contentEquals(arrayOf(String::class.java)) },
    ) { chain, settings ->
      val name = chain.getArg(0) as String
      if (settings.hideVpn && isVpnInterface(name)) null else chain.proceed()
    }
    hookMethods(
      type = NetworkInterface::class.java,
      name = "getNetworkInterfaces",
      predicate = { Modifier.isStatic(it.modifiers) && it.parameterCount == 0 },
    ) { chain, settings ->
      val interfaces = chain.proceed() as? java.util.Enumeration<*> ?: return@hookMethods null
      if (!settings.hideVpn) return@hookMethods interfaces

      val visible = mutableListOf<NetworkInterface>()
      while (interfaces.hasMoreElements()) {
        val networkInterface = interfaces.nextElement() as? NetworkInterface ?: continue
        if (!isVpnInterface(networkInterface.name)) visible += networkInterface
      }
      Collections.enumeration(visible)
    }
  }

  private fun hookMethods(
    type: Class<*>,
    name: String,
    predicate: (Method) -> Boolean = { true },
    interceptor: (Chain, ModuleSettings) -> Any?,
  ) {
    val methods =
      (type.methods.asSequence() + type.declaredMethods.asSequence())
        .filter { it.name == name && predicate(it) }
        .distinctBy(Method::toGenericString)
        .toList()

    methods.forEach { method ->
      runCatching {
          val signature = method.parameterTypes.joinToString(",") { it.name }
          module
            .hook(method)
            .setId("TikTokRoaming:${type.name}#$name($signature)")
            .intercept { chain ->
              val settings = currentSettings()
              if (!settings.enabled) chain.proceed() else interceptor(chain, settings)
            }
        }
        .onFailure { error -> module.log(Log.WARN, TAG, "Unable to hook ${method.toGenericString()}", error) }
    }
  }

  private fun safely(group: String, block: () -> Unit) {
    runCatching(block).onFailure { error ->
      module.log(Log.ERROR, TAG, "Failed to install $group hooks", error)
    }
  }

  private fun isVpnInterface(name: String): Boolean {
    val normalized = name.lowercase(Locale.ROOT)
    return VPN_INTERFACE_PREFIXES.any(normalized::startsWith)
  }

  private companion object {
    const val TAG = "TikTokRoaming"
    val VPN_INTERFACE_PREFIXES = listOf("tun", "tap", "ppp", "wg", "tailscale", "nordlynx")
  }
}
