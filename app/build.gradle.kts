import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

fun gitOutput(vararg arguments: String): String =
  providers
    .exec {
      commandLine("git", *arguments)
      isIgnoreExitValue = true
    }
    .standardOutput
    .asText
    .get()
    .trim()

val defaultVersionTag = "v1.0.0"
val headVersionTag =
  gitOutput("tag", "--points-at", "HEAD", "--sort=-version:refname")
    .lineSequence()
    .firstOrNull(String::isNotBlank)
val latestVersionTag =
  gitOutput("tag", "--list", "--sort=-version:refname")
    .lineSequence()
    .firstOrNull(String::isNotBlank)
val requestedReleaseVersionTag =
  providers.gradleProperty("releaseVersionTag").orNull?.trim()?.takeIf(String::isNotBlank)
val versionTag = requestedReleaseVersionTag ?: headVersionTag ?: latestVersionTag ?: defaultVersionTag
val commitId = gitOutput("rev-parse", "--short=7", "HEAD").ifBlank { "unknown" }
val commitCount = gitOutput("rev-list", "--count", "HEAD").toIntOrNull() ?: 1
val computedVersionName = "$versionTag-$commitId"
val isDailyBuild =
  providers.gradleProperty("dailyBuild").orNull?.equals("true", ignoreCase = true) == true
val apkVersion = if (isDailyBuild) computedVersionName else versionTag
val apkFileName =
  "${rootProject.name}-${apkVersion.replace(Regex("[^A-Za-z0-9._-]"), "-")}.apk"

val releaseSigningPropertiesFile = rootProject.file("keystore.properties")
val releaseSigningEnvironmentVariables =
  mapOf(
    "storeFile" to "TIKTOKROAMING_STORE_FILE",
    "storePassword" to "TIKTOKROAMING_STORE_PASSWORD",
    "keyAlias" to "TIKTOKROAMING_KEY_ALIAS",
    "keyPassword" to "TIKTOKROAMING_KEY_PASSWORD",
  )
val releaseSigningProperties =
  Properties()
    .apply {
      if (releaseSigningPropertiesFile.isFile) {
        releaseSigningPropertiesFile.reader(Charsets.UTF_8).use { reader -> load(reader) }
      }
      releaseSigningEnvironmentVariables.forEach { (propertyName, environmentVariable) ->
        providers.environmentVariable(environmentVariable).orNull
          ?.takeIf(String::isNotBlank)
          ?.let { setProperty(propertyName, it) }
      }
    }
    .takeUnless { it.isEmpty }

releaseSigningProperties?.let { properties ->
  val requiredProperties = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
  val missingProperties = requiredProperties.filter { properties.getProperty(it).isNullOrBlank() }
  check(missingProperties.isEmpty()) {
    "Missing release signing properties in ${releaseSigningPropertiesFile.name}: ${missingProperties.joinToString()}"
  }
}

android {
    namespace = "com.guyuuan.xposed.tiktokroaming"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.guyuuan.xposed.tiktokroaming"
        minSdk = 26
        targetSdk = 37
        versionCode = commitCount
        versionName = computedVersionName
    }

    signingConfigs {
        releaseSigningProperties?.let { properties ->
            create("release") {
                storeFile = rootProject.file(properties.getProperty("storeFile"))
                storePassword = properties.getProperty("storePassword")
                keyAlias = properties.getProperty("keyAlias")
                keyPassword = properties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
      shaders = false
    }

    packaging {
      resources {
        merges += "META-INF/xposed/*"
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

androidComponents {
  onVariants(selector().withBuildType("release")) { variant ->
    variant.outputs.forEach { output -> output.outputFileName.set(apkFileName) }
  }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Xposed Module API (provided by the framework at runtime)
  compileOnly(libs.libxposed.api)
  implementation(libs.libxposed.service)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
}
