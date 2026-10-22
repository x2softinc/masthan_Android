package com.x2softinc.masthan.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.x2softinc.masthan.ASAR
import com.x2softinc.masthan.CITY
import com.x2softinc.masthan.DHUHAR
import com.x2softinc.masthan.FAJR
import com.x2softinc.masthan.ISHA
import com.x2softinc.masthan.JURISDICTION
import com.x2softinc.masthan.JUMMA
import com.x2softinc.masthan.MAGHRIB
import com.x2softinc.masthan.SUNRISE
import com.x2softinc.masthan.data.MasjidService
import com.x2softinc.masthan.data.UserFriendlyError
import com.x2softinc.masthan.util.dateSelectionText
import com.x2softinc.masthan.util.formatTime
import com.x2softinc.masthan.util.gregorianDateText
import com.x2softinc.masthan.util.hijriDateText
import com.x2softinc.masthan.util.isFriday
import com.x2softinc.masthan.util.parseSettingTime
import com.x2softinc.masthan.util.today
import com.x2softinc.masthan.viewmodel.CurrentPrayer
import com.x2softinc.masthan.viewmodel.TimesViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

private const val MAX_DAY_OFFSET = 15 // 0 is today; 15 is fifteen days from today

private fun dateFor(offset: Int): ZonedDateTime = today().plusDays(offset.toLong())

/** Prayer times for today and the next 15 days (port of Views/DashboardView.swift). */
@Composable
fun DashboardScreen(timesVM: TimesViewModel = viewModel()) {
    var currentError by remember { mutableStateOf<UserFriendlyError?>(null) }
    var selectedDayOffset by remember { mutableIntStateOf(0) }
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
    val scope = rememberCoroutineScope()

    suspend fun loadMasjidInfo() {
        try {
            MasjidService().fetchMasjidInfo()
        } catch (e: CancellationException) {
            throw e
        } catch (e: UserFriendlyError) {
            currentError = e
        } catch (e: Exception) {
            currentError = UserFriendlyError.dataLoading("Masjid Information") { loadMasjidInfo() }
        }
    }

    LaunchedEffect(Unit) { loadMasjidInfo() }
    LaunchedEffect(selectedDayOffset) { timesVM.load(selectedDayOffset) }

    if (isTablet) {
        TabletLayout(timesVM, selectedDayOffset) { selectedDayOffset = it }
    } else {
        PhoneLayout(timesVM, selectedDayOffset) { selectedDayOffset = it }
    }

    UserFriendlyErrorDialog(currentError) { currentError = null }
}

// region Layouts

@Composable
private fun PhoneLayout(timesVM: TimesViewModel, selectedDayOffset: Int, onSelect: (Int) -> Unit) {
    val pagerState = rememberPagerState(initialPage = selectedDayOffset, pageCount = { MAX_DAY_OFFSET + 1 })

    LaunchedEffect(pagerState.settledPage) { onSelect(pagerState.settledPage) }

    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { offset ->
        DayPrayerPage(offset, dateFor(offset), timesVM, isTabletFullScreen = false)
    }
}

@Composable
private fun TabletLayout(timesVM: TimesViewModel, selectedDayOffset: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxSize()) {
        // Sidebar with date picker
        Column(
            Modifier
                .width(280.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        ) {
            Text("Select Date", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            HorizontalDivider()
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items((0..MAX_DAY_OFFSET).toList()) { offset ->
                    DateSelectionRow(
                        offset = offset,
                        date = dateFor(offset),
                        isSelected = selectedDayOffset == offset,
                        modifier = Modifier.clickable { onSelect(offset) },
                    )
                }
            }
        }

        HorizontalDivider(Modifier.fillMaxHeight().width(1.dp))

        // Main content area - use full space
        Box(Modifier.weight(1f).fillMaxHeight().padding(16.dp)) {
            DayPrayerPage(selectedDayOffset, dateFor(selectedDayOffset), timesVM, isTabletFullScreen = true)
        }
    }
}

// endregion

// region Date selection row

@Composable
private fun DateSelectionRow(offset: Int, date: ZonedDateTime, isSelected: Boolean, modifier: Modifier = Modifier) {
    val theme = MaterialTheme.colorScheme.primary
    val onTheme = MaterialTheme.colorScheme.onPrimary
    val shape = RoundedCornerShape(10.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isSelected) theme else Color.Transparent)
            .border(1.dp, if (isSelected) Color.Transparent else Color.Gray.copy(alpha = 0.2f), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                dateSelectionText(date, offset == 0),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) onTheme else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                hijriDateText(date, shortMonth = true),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) onTheme.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (offset == 0) {
            Icon(
                Icons.Filled.Star, contentDescription = "Today",
                tint = if (isSelected) onTheme else theme,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

// endregion

// region Day prayer page

@Composable
private fun DayPrayerPage(dayOffset: Int, pageDate: ZonedDateTime, timesVM: TimesViewModel, isTabletFullScreen: Boolean) {
    // Re-evaluate the highlighted prayer once a minute.
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = ZonedDateTime.now()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Date header
        Column(Modifier.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(gregorianDateText(pageDate), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                hijriDateText(pageDate),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        if (timesVM.isLoading || !timesVM.hasLoaded || timesVM.displayedDayOffset != dayOffset) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator()
                    Text("Loading prayer times…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Box(
                Modifier.fillMaxWidth().weight(1f).padding(8.dp),
                contentAlignment = if (isTabletFullScreen) Alignment.Center else Alignment.TopCenter,
            ) {
                PrayerTable(dayOffset, pageDate, timesVM, now)
            }
        }
    }
}

@Composable
private fun PrayerTable(dayOffset: Int, pageDate: ZonedDateTime, timesVM: TimesViewModel, now: ZonedDateTime) {
    val jurisdiction by rememberIntPref(JURISDICTION)
    val theme = MaterialTheme.colorScheme.primary
    val current = if (dayOffset == 0) timesVM.currentPrayerInfo(now) else null
    val times = timesVM.adhanTimes
    val shape = RoundedCornerShape(16.dp)

    Column(
        Modifier
            .shadow(8.dp, shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                BorderStroke(1.5.dp, Brush.linearGradient(listOf(theme.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.2f)))),
                shape,
            ),
    ) {
        // Header row
        Row(
            Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(theme.copy(alpha = 0.25f), theme.copy(alpha = 0.15f))))
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(1f).padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.NightsStay, null, tint = theme, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Prayer", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Text("Adhan", Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("Iqama", Modifier.weight(1f).padding(end = 16.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        HorizontalDivider(color = Color.Gray.copy(alpha = 0.4f))

        PrayerTableRow(FAJR, times.fajr, timesVM.masjidAdhanTimes?.fajr, timesVM.iqamaTimes?.fajr, isCurrent(FAJR, current), Icons.Filled.Bedtime)
        RowDivider()
        PrayerTableRow(SUNRISE, times.sunrise, null, null, isCurrent(SUNRISE, current), Icons.Filled.WbTwilight, isSpecial = true)
        RowDivider()
        PrayerTableRow(DHUHAR, times.dhuhar, timesVM.masjidAdhanTimes?.dhuhar, timesVM.iqamaTimes?.dhuhar, isCurrent(DHUHAR, current), Icons.Filled.WbSunny)
        RowDivider()
        PrayerTableRow(
            ASAR,
            if (jurisdiction != 0) times.asarHanafi else times.asar,
            timesVM.masjidAdhanTimes?.asar, timesVM.iqamaTimes?.asar,
            isCurrent(ASAR, current), Icons.Filled.WbCloudy,
        )
        RowDivider()
        PrayerTableRow(MAGHRIB, times.maghrib, timesVM.masjidAdhanTimes?.maghrib, timesVM.iqamaTimes?.maghrib, isCurrent(MAGHRIB, current), Icons.Filled.Brightness4)
        RowDivider()
        PrayerTableRow(ISHA, times.isha, timesVM.masjidAdhanTimes?.isha, timesVM.iqamaTimes?.isha, isCurrent(ISHA, current), Icons.Filled.NightsStay)

        // Jumma section if applicable
        if (shouldShowJumma(pageDate, timesVM)) {
            HorizontalDivider(color = theme.copy(alpha = 0.4f))
            PrayerTableRow(
                JUMMA, times.dhuhar, timesVM.masjidAdhanTimes?.dhuhar,
                parseSettingTime(timesVM.settings?.jumma, pageDate),
                isCurrent = false, icon = Icons.Filled.Groups, isJumma = true,
            )
        }
    }
}

@Composable
private fun RowDivider() = HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

private fun shouldShowJumma(pageDate: ZonedDateTime, timesVM: TimesViewModel): Boolean =
    isFriday(pageDate) && !timesVM.settings?.jumma.isNullOrBlank()

private fun isCurrent(displayName: String, current: CurrentPrayer?): Boolean {
    current ?: return false
    return when (displayName) {
        DHUHAR -> current.name == "Dhuhar" || current.name == "Dhuhr"
        ASAR -> current.name == "Asar" || current.name == "Asr"
        else -> current.name == displayName
    }
}

@Composable
private fun PrayerTableRow(
    name: String,
    enters: ZonedDateTime?,
    adhan: ZonedDateTime?,
    iqama: ZonedDateTime?,
    isCurrent: Boolean,
    icon: ImageVector,
    isJumma: Boolean = false,
    isSpecial: Boolean = false,
) {
    val theme = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    val rowBackground = when {
        isCurrent -> Brush.horizontalGradient(listOf(theme.copy(alpha = 0.15f), theme.copy(alpha = 0.08f)))
        isJumma -> Brush.horizontalGradient(listOf(theme.copy(alpha = 0.05f), theme.copy(alpha = 0.05f)))
        else -> Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
    }
    val iconTint by animateColorAsState(if (isCurrent) theme else secondary.copy(alpha = 0.7f), label = "iconTint")

    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min).background(rowBackground),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isCurrent) Box(Modifier.width(4.dp).fillMaxHeight().background(theme))

        // Prayer name with icon and "enters" time
        Row(
            Modifier.weight(1f).padding(start = 12.dp, top = 18.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isCurrent) theme.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(16.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isCurrent) onSurface else onSurface.copy(alpha = 0.85f),
                )
                if (enters != null && !isSpecial) {
                    Text(
                        formatTime(enters),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        color = secondary.copy(alpha = 0.8f),
                    )
                }
            }
        }

        // Adhan time column
        Box(Modifier.weight(1f).padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
            when {
                adhan != null -> Text(
                    formatTime(adhan), fontSize = 16.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                )
                isSpecial && enters != null -> Text(
                    formatTime(enters), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Red,
                )
                else -> Text("—", fontSize = 16.sp, color = secondary)
            }
        }

        // Iqama time column
        Box(Modifier.weight(1f).padding(top = 18.dp, bottom = 18.dp, end = 12.dp), contentAlignment = Alignment.Center) {
            if (iqama != null) {
                Text(
                    formatTime(iqama), fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = if (isCurrent) theme else theme.copy(alpha = 0.8f),
                )
            } else {
                Text("—", fontSize = 16.sp, color = secondary)
            }
        }
    }
}

// endregion
