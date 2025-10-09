package com.phoenixcorp.founderfinder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.phoenixcorp.founderfinder.data.Activity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

@Composable
fun CalendarSection(
    currentMonth: Calendar = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) },
    activities: List<Activity> = emptyList(),
    timeSlots: List<String> = listOf(
        "00:00", "00:30", "01:00", "01:30", "02:00", "02:30", "03:00", "03:30",
        "04:00", "04:30", "05:00", "05:30", "06:00", "06:30", "07:00", "07:30",
        "08:00", "08:30", "09:00", "09:30", "10:00", "10:30", "11:00", "11:30",
        "12:00", "12:30", "13:00", "13:30", "14:00", "14:30", "15:00", "15:30",
        "16:00", "16:30", "17:00", "17:30", "18:00", "18:30", "19:00", "19:30",
        "20:00", "20:30", "21:00", "21:30", "22:00", "22:30", "23:00", "23:30"
    ),
    calendarTitle: String = "Calendar",
    onDayTap: (Int) -> Unit = {},
    onDayLongPress: (Int) -> Unit = {},
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = calendarTitle,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Calendar Grid
        val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = currentMonth.apply { set(Calendar.DAY_OF_MONTH, 1) }.get(Calendar.DAY_OF_WEEK)
        val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val paddingDaysCount = max(0, firstDayOfWeek - 1)
        val totalCells = 35
        val daysAfterMonth = max(0, totalCells - (paddingDaysCount + daysInMonth))

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            contentPadding = PaddingValues(4.dp)
        ) {
            // Day names
            items(dayNames) { dayName ->
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(4.dp)
                )
            }

            // Padding days before first day
            items(paddingDaysCount) {
                Box(modifier = Modifier.size(40.dp))
            }

            // Actual days
            items((1..daysInMonth).toList()) { day ->
                val dayActivities = activities.filter {
                    val activityDate = Calendar.getInstance().apply { timeInMillis = it.date }
                    activityDate.get(Calendar.DAY_OF_MONTH) == day &&
                            activityDate.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH) &&
                            activityDate.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR)
                }
                val activityCount = dayActivities.size
                val maxSlots = timeSlots.size
                val busynessColor = when {
                    activityCount == 0 -> null
                    activityCount <= 2 || activityCount.toFloat() / maxSlots < 0.5f -> MaterialTheme.colorScheme.primary
                    activityCount <= 4 || activityCount.toFloat() / maxSlots < 0.75f -> Color.Yellow
                    else -> MaterialTheme.colorScheme.error
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { onDayTap(day) },
                                onLongPress = { onDayLongPress(day) }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    busynessColor?.let { color ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .align(Alignment.TopCenter)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                    Text(
                        text = day.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (dayActivities.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Padding days after last day
            items(daysAfterMonth) {
                Box(modifier = Modifier.size(40.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Navigation Arrows
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onPreviousMonth) { Text("<") }
            Text(
                text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentMonth.time),
                style = MaterialTheme.typography.bodyLarge
            )
            TextButton(onClick = onNextMonth) { Text(">") }
        }
    }
}