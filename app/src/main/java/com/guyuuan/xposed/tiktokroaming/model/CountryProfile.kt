package com.guyuuan.xposed.tiktokroaming.model

import java.util.Locale

data class CountryProfile(
  val id: String,
  val displayName: String,
  val countryIso: String,
  val operatorNumeric: String,
  val languageTag: String,
  val timeZoneId: String,
) {
  val locale: Locale
    get() = Locale.forLanguageTag(languageTag)
}

object CountryProfiles {
  val all: List<CountryProfile> =
    listOf(
      CountryProfile("US", "美国", "US", "310260", "en-US", "America/New_York"),
      CountryProfile("CA", "加拿大", "CA", "302720", "en-CA", "America/Toronto"),
      CountryProfile("MX", "墨西哥", "MX", "334020", "es-MX", "America/Mexico_City"),
      CountryProfile("BR", "巴西", "BR", "72405", "pt-BR", "America/Sao_Paulo"),
      CountryProfile("GB", "英国", "GB", "23415", "en-GB", "Europe/London"),
      CountryProfile("DE", "德国", "DE", "26202", "de-DE", "Europe/Berlin"),
      CountryProfile("FR", "法国", "FR", "20801", "fr-FR", "Europe/Paris"),
      CountryProfile("ES", "西班牙", "ES", "21407", "es-ES", "Europe/Madrid"),
      CountryProfile("IT", "意大利", "IT", "22210", "it-IT", "Europe/Rome"),
      CountryProfile("NL", "荷兰", "NL", "20404", "nl-NL", "Europe/Amsterdam"),
      CountryProfile("PL", "波兰", "PL", "26002", "pl-PL", "Europe/Warsaw"),
      CountryProfile("TR", "土耳其", "TR", "28601", "tr-TR", "Europe/Istanbul"),
      CountryProfile("JP", "日本", "JP", "44010", "ja-JP", "Asia/Tokyo"),
      CountryProfile("KR", "韩国", "KR", "45008", "ko-KR", "Asia/Seoul"),
      CountryProfile("SG", "新加坡", "SG", "52501", "en-SG", "Asia/Singapore"),
      CountryProfile("MY", "马来西亚", "MY", "50212", "ms-MY", "Asia/Kuala_Lumpur"),
      CountryProfile("ID", "印度尼西亚", "ID", "51010", "id-ID", "Asia/Jakarta"),
      CountryProfile("TH", "泰国", "TH", "52001", "th-TH", "Asia/Bangkok"),
      CountryProfile("VN", "越南", "VN", "45204", "vi-VN", "Asia/Ho_Chi_Minh"),
      CountryProfile("PH", "菲律宾", "PH", "51502", "en-PH", "Asia/Manila"),
      CountryProfile("TW", "中国台湾", "TW", "46692", "zh-TW", "Asia/Taipei"),
      CountryProfile("HK", "中国香港", "HK", "45412", "zh-HK", "Asia/Hong_Kong"),
      CountryProfile("IN", "印度", "IN", "40445", "en-IN", "Asia/Kolkata"),
      CountryProfile("AU", "澳大利亚", "AU", "50501", "en-AU", "Australia/Sydney"),
      CountryProfile("AE", "阿联酋", "AE", "42402", "ar-AE", "Asia/Dubai"),
      CountryProfile("SA", "沙特阿拉伯", "SA", "42001", "ar-SA", "Asia/Riyadh"),
    )

  val default: CountryProfile = all.first()

  fun byId(id: String?): CountryProfile =
    all.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: default
}
