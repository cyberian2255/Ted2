package com.example.actions

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AssistantActionResult(
  val title: String,
  val speechSummary: String,
  val success: Boolean,
  val requiresConfirmation: Boolean = false,
  val pendingIntent: Intent? = null
)

data class InstalledAppInfo(
  val appName: String,
  val packageName: String
)

class TedActionManager(private val context: Context) {

  fun getTimeAndDate(): AssistantActionResult {
    val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
    val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US)
    val now = Date()
    val timeStr = timeFormat.format(now)
    val dateStr = dateFormat.format(now)

    return AssistantActionResult(
      title = "Current Time & Date",
      speechSummary = "It's $timeStr on $dateStr. Time flies when you're staring at a screen, champ.",
      success = true
    )
  }

  fun getBatteryStatus(): AssistantActionResult {
    val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    val batteryStatus = context.registerReceiver(null, ifilter)
    val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else 0
    val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
    val isCharging = plugged != 0

    val chargingText = if (isCharging) "charging right now" else "running on fumes"
    val roast = if (pct < 20 && !isCharging) "Plug your damn phone in before it dies." else "You're good for now."

    return AssistantActionResult(
      title = "Battery Status",
      speechSummary = "Battery's at $pct% and $chargingText. $roast",
      success = true
    )
  }

  fun checkNetworkConnectivity(): AssistantActionResult {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    val activeNet = cm?.activeNetwork
    val caps = cm?.getNetworkCapabilities(activeNet)
    val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    val isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

    val statusText = when {
      hasInternet && isWifi -> "Connected to Wi-Fi with working internet."
      hasInternet && isCellular -> "Connected to cellular mobile data."
      hasInternet -> "Online and connected."
      else -> "Offline. You're disconnected from the digital world."
    }

    return AssistantActionResult(
      title = "Network Status",
      speechSummary = statusText,
      success = hasInternet
    )
  }

  fun openWebsite(url: String): AssistantActionResult {
    val safeUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
      "https://$url"
    } else url

    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    return try {
      context.startActivity(intent)
      AssistantActionResult(
        title = "Opening Website",
        speechSummary = "Opening $url. Don't fall down a rabbit hole.",
        success = true
      )
    } catch (_: Exception) {
      AssistantActionResult(
        title = "Open Failed",
        speechSummary = "Couldn't launch the browser for that link, pal.",
        success = false
      )
    }
  }

  fun performWebSearch(query: String): AssistantActionResult {
    val searchUrl = "https://www.google.com/search?q=${Uri.encode(query)}"
    return openWebsite(searchUrl)
  }

  fun setTimer(seconds: Int, message: String = "Ted's Timer"): AssistantActionResult {
    val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
      putExtra(AlarmClock.EXTRA_LENGTH, seconds)
      putExtra(AlarmClock.EXTRA_MESSAGE, message)
      putExtra(AlarmClock.EXTRA_SKIP_UI, false)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    return try {
      context.startActivity(intent)
      AssistantActionResult(
        title = "Timer Set",
        speechSummary = "Timer set for $seconds seconds. I'll buzz you when time's up.",
        success = true
      )
    } catch (_: Exception) {
      AssistantActionResult(
        title = "Timer Error",
        speechSummary = "No clock or timer app supported on this device.",
        success = false
      )
    }
  }

  fun setAlarm(hour: Int, minute: Int, message: String = "Ted Alarm"): AssistantActionResult {
    val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
      putExtra(AlarmClock.EXTRA_HOUR, hour)
      putExtra(AlarmClock.EXTRA_MINUTES, minute)
      putExtra(AlarmClock.EXTRA_MESSAGE, message)
      putExtra(AlarmClock.EXTRA_SKIP_UI, false)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    return try {
      context.startActivity(intent)
      AssistantActionResult(
        title = "Alarm Scheduled",
        speechSummary = "Alarm set for $hour:${minute.toString().padStart(2, '0')}. Good luck waking up.",
        success = true
      )
    } catch (_: Exception) {
      AssistantActionResult(
        title = "Alarm Error",
        speechSummary = "Couldn't access the system alarm clock.",
        success = false
      )
    }
  }

  /**
   * Dynamically queries all launchable applications installed on the user's device.
   */
  fun getInstalledLaunchableApps(): List<InstalledAppInfo> {
    val pm = context.packageManager
    val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
      addCategory(Intent.CATEGORY_LAUNCHER)
    }

    val resolveInfos = try {
      pm.queryIntentActivities(mainIntent, 0)
    } catch (_: Exception) {
      emptyList()
    }

    val appList = mutableListOf<InstalledAppInfo>()
    for (info in resolveInfos) {
      val pkg = info.activityInfo.packageName
      // Skip self if desired, or keep all
      val name = try {
        info.loadLabel(pm).toString()
      } catch (_: Exception) {
        info.activityInfo.name
      }
      if (name.isNotBlank() && pkg.isNotBlank()) {
        appList.add(InstalledAppInfo(appName = name, packageName = pkg))
      }
    }
    return appList.distinctBy { it.packageName }.sortedBy { it.appName.lowercase() }
  }

  /**
   * Universal launcher: searches installed apps by name or voice command and launches safely.
   */
  fun launchAppByNameOrPackage(appNameOrQuery: String): AssistantActionResult {
    val cleanQuery = appNameOrQuery.trim().lowercase()
      .removePrefix("open ")
      .removePrefix("launch ")
      .removePrefix("start ")
      .trim()

    if (cleanQuery.isBlank()) {
      return AssistantActionResult(
        title = "App Search",
        speechSummary = "Which app do you want me to launch, champ?",
        success = false
      )
    }

    val pm = context.packageManager
    val installed = getInstalledLaunchableApps()

    // 1. Exact match on app name
    var matched = installed.firstOrNull { it.appName.equals(cleanQuery, ignoreCase = true) }

    // 2. Contains match
    if (matched == null) {
      matched = installed.firstOrNull { it.appName.lowercase().contains(cleanQuery) }
    }

    // 3. Package name match
    if (matched == null) {
      matched = installed.firstOrNull { it.packageName.lowercase().contains(cleanQuery) }
    }

    if (matched != null) {
      return launchApp(matched.packageName, matched.appName)
    }

    // Direct package name fallback check
    val directIntent = pm.getLaunchIntentForPackage(cleanQuery)
    if (directIntent != null) {
      return launchApp(cleanQuery, cleanQuery)
    }

    return AssistantActionResult(
      title = "App Not Found",
      speechSummary = "Couldn't find '$cleanQuery' installed on this device, guy. Double check your spelling.",
      success = false
    )
  }

  fun launchApp(packageName: String, appName: String): AssistantActionResult {
    val pm = context.packageManager
    val intent = pm.getLaunchIntentForPackage(packageName)
    if (intent != null) {
      try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        context.startActivity(intent)
        return AssistantActionResult(
          title = "Launched $appName",
          speechSummary = "Fired up $appName for ya. Try not to get distracted.",
          success = true
        )
      } catch (e: Exception) {
        return AssistantActionResult(
          title = "Launch Error",
          speechSummary = "Failed to launch $appName: ${e.localizedMessage}",
          success = false
        )
      }
    }
    return AssistantActionResult(
      title = "App Not Installed",
      speechSummary = "$appName doesn't look like it's installed here, guy.",
      success = false
    )
  }
}
