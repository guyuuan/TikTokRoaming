package com.guyuuan.xposed.tiktokroaming.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guyuuan.xposed.tiktokroaming.model.CountryProfile
import com.guyuuan.xposed.tiktokroaming.model.CountryProfiles
import com.guyuuan.xposed.tiktokroaming.settings.ModuleSettings
import com.guyuuan.xposed.tiktokroaming.settings.PackageManagerLauncherIconController
import com.guyuuan.xposed.tiktokroaming.settings.RemoteSettingsRepository
import com.guyuuan.xposed.tiktokroaming.settings.TikTokTargets
import com.guyuuan.xposed.tiktokroaming.settings.XposedServiceRegistry
import com.guyuuan.xposed.tiktokroaming.theme.TikTokRoamingTheme

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val repository = remember { RemoteSettingsRepository(XposedServiceRegistry.service) }
  val launcherIconController =
    remember(context) { PackageManagerLauncherIconController(context.applicationContext) }
  val viewModel: MainScreenViewModel =
    viewModel { MainScreenViewModel(repository, launcherIconController) }
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  LifecycleResumeEffect(viewModel) {
    viewModel.refreshLauncherIconState()
    onPauseOrDispose {}
  }

  when (val current = state) {
    MainScreenUiState.Loading -> Unit
    is MainScreenUiState.Error ->
      ErrorScreen(message = current.throwable.message ?: "未知错误", modifier = modifier)
    is MainScreenUiState.Ready ->
      SettingsScreen(
        state = current,
        onSelectProfile = viewModel::selectProfile,
        onSetEnabled = viewModel::setEnabled,
        onSetHideVpn = viewModel::setHideVpn,
        onSetLauncherIconHidden = viewModel::setLauncherIconHidden,
        modifier = modifier,
      )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
  state: MainScreenUiState.Ready,
  onSelectProfile: (CountryProfile) -> Unit,
  onSetEnabled: (Boolean) -> Unit,
  onSetHideVpn: (Boolean) -> Unit,
  onSetLauncherIconHidden: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showCountryPicker by rememberSaveable { mutableStateOf(false) }

  Scaffold(
    modifier = modifier,
    topBar = { TopAppBar(title = { Text("TikTok Roaming") }) },
  ) { contentPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item { ServiceStatusCard(state) }
      item {
        SettingsCard(
          state = state,
          onSetEnabled = onSetEnabled,
          onSetHideVpn = onSetHideVpn,
          onSetLauncherIconHidden = onSetLauncherIconHidden,
        )
      }
      item {
        CountryCard(
          profile = state.selectedProfile,
          enabled = state.serviceConnected,
          onChoose = { showCountryPicker = true },
        )
      }
      item { ScopeCard() }
      item {
        Text(
          text = "切换档案后请彻底结束并重新启动 TikTok。国家、语言和时区只会影响上述作用域内的进程。",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(bottom = 24.dp),
        )
      }
    }
  }

  if (showCountryPicker) {
    CountryPickerDialog(
      profiles = state.profiles,
      selected = state.selectedProfile,
      onDismiss = { showCountryPicker = false },
      onSelect = { profile ->
        onSelectProfile(profile)
        showCountryPicker = false
      },
    )
  }
}

@Composable
private fun ServiceStatusCard(state: MainScreenUiState.Ready) {
  val colors =
    if (state.serviceConnected) {
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else {
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    }

  Card(colors = colors, modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        text = if (state.serviceConnected) "模块服务已连接" else "模块服务未连接",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
      )
      Text(state.frameworkLabel, style = MaterialTheme.typography.bodyMedium)
      if (!state.serviceConnected) {
        Text(
          "请先在兼容 libxposed API 102 的管理器中启用模块，并勾选两个 TikTok 作用域。",
          style = MaterialTheme.typography.bodySmall,
        )
      }
    }
  }
}

@Composable
private fun SettingsCard(
  state: MainScreenUiState.Ready,
  onSetEnabled: (Boolean) -> Unit,
  onSetHideVpn: (Boolean) -> Unit,
  onSetLauncherIconHidden: (Boolean) -> Unit,
) {
  Card(modifier = Modifier.fillMaxWidth()) {
    Column {
      SwitchRow(
        title = "启用环境伪装",
        description = "向 TikTok 返回所选国家的 SIM、语言和时区信息",
        checked = state.settings.enabled,
        enabled = state.serviceConnected,
        onCheckedChange = onSetEnabled,
      )
      HorizontalDivider(Modifier.padding(horizontal = 16.dp))
      SwitchRow(
        title = "隐藏 VPN 状态",
        description = "隐藏 Android 标准 VPN 网络标记和常见隧道网卡",
        checked = state.settings.hideVpn,
        enabled = state.serviceConnected && state.settings.enabled,
        onCheckedChange = onSetHideVpn,
      )
      HorizontalDivider(Modifier.padding(horizontal = 16.dp))
      SwitchRow(
        title = "隐藏桌面图标",
        description = "从桌面启动器中隐藏模块入口，不影响当前设置页",
        checked = state.launcherIconHidden,
        enabled = true,
        onCheckedChange = onSetLauncherIconHidden,
      )
    }
  }
}

@Composable
private fun SwitchRow(
  title: String,
  description: String,
  checked: Boolean,
  enabled: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(title, style = MaterialTheme.typography.titleSmall)
      Text(
        description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Spacer(Modifier.width(12.dp))
    Switch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
  }
}

@Composable
private fun CountryCard(profile: CountryProfile, enabled: Boolean, onChoose: () -> Unit) {
  Card(modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text("目标国家", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
      Text("${profile.displayName} · ${profile.countryIso}", style = MaterialTheme.typography.headlineSmall)
      Text(
        "${profile.languageTag}  ·  ${profile.timeZoneId}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Button(onClick = onChoose, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Text("选择国家")
      }
    }
  }
}

@Composable
private fun ScopeCard() {
  Card(modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text("固定作用域", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
      TikTokTargets.packages.forEach { packageName ->
        Text(packageName, style = MaterialTheme.typography.bodyMedium)
      }
      Text(
        "不包含 System Framework；所有 Hook 均限制在 TikTok 进程内。",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun CountryPickerDialog(
  profiles: List<CountryProfile>,
  selected: CountryProfile,
  onDismiss: () -> Unit,
  onSelect: (CountryProfile) -> Unit,
) {
  var query by rememberSaveable { mutableStateOf("") }
  val filtered =
    remember(profiles, query) {
      val keyword = query.trim()
      if (keyword.isEmpty()) profiles
      else
        profiles.filter { profile ->
          profile.displayName.contains(keyword, ignoreCase = true) ||
            profile.countryIso.contains(keyword, ignoreCase = true) ||
            profile.languageTag.contains(keyword, ignoreCase = true)
        }
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("选择目标国家") },
    text = {
      Column(Modifier.heightIn(max = 520.dp)) {
        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          label = { Text("搜索国家或 ISO") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        LazyColumn {
          items(filtered, key = CountryProfile::id) { profile ->
            Row(
              modifier = Modifier.fillMaxWidth().clickable { onSelect(profile) }.padding(vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              RadioButton(selected = profile.id == selected.id, onClick = { onSelect(profile) })
              Column {
                Text("${profile.displayName} · ${profile.countryIso}")
                Text(
                  "${profile.languageTag} · ${profile.timeZoneId}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    },
    confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
  )
}

@Composable
private fun ErrorScreen(message: String, modifier: Modifier = Modifier) {
  Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
    Text("无法读取模块配置", style = MaterialTheme.typography.titleLarge)
    Text(message, color = MaterialTheme.colorScheme.error)
  }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
  TikTokRoamingTheme {
    SettingsScreen(
      state =
        MainScreenUiState.Ready(
          serviceConnected = true,
          frameworkLabel = "LSPosed · API 102",
          settings = ModuleSettings(),
          selectedProfile = CountryProfiles.default,
          launcherIconHidden = false,
        ),
      onSelectProfile = {},
      onSetEnabled = {},
      onSetHideVpn = {},
      onSetLauncherIconHidden = {},
    )
  }
}
