package com.guyuuan.xposed.tiktokroaming.model

import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CountryProfilesTest {
  @Test
  fun profiles_haveUniqueIdsAndCountryCodes() {
    assertEquals(CountryProfiles.all.size, CountryProfiles.all.map { it.id }.toSet().size)
    assertEquals(CountryProfiles.all.size, CountryProfiles.all.map { it.countryIso }.toSet().size)
  }

  @Test
  fun profiles_haveValidLocalesAndTimeZones() {
    CountryProfiles.all.forEach { profile ->
      assertTrue(profile.id, Locale.forLanguageTag(profile.languageTag).language.isNotBlank())
      assertTrue(profile.id, TimeZone.getAvailableIDs().contains(profile.timeZoneId))
      assertTrue(profile.id, profile.operatorNumeric.matches(Regex("\\d{5,6}")))
    }
  }

  @Test
  fun catalog_containsMajorTikTokRegionsAndFallsBackToDefault() {
    listOf("US", "GB", "JP", "KR", "SG", "TW").forEach { countryIso ->
      assertNotNull(CountryProfiles.all.find { it.countryIso == countryIso })
    }

    assertEquals(CountryProfiles.default, CountryProfiles.byId("missing"))
  }
}
