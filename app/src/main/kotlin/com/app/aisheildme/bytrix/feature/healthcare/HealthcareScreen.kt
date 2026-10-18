package com.app.aisheildme.bytrix.feature.healthcare

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.aisheildme.bytrix.core.designsystem.AppColors
import com.app.aisheildme.bytrix.core.designsystem.component.PagedList
import com.app.aisheildme.bytrix.core.designsystem.component.RecordRow
import com.app.aisheildme.bytrix.core.designsystem.component.SettingsRow
import com.app.aisheildme.bytrix.core.designsystem.component.SimpleAppBar
import com.app.aisheildme.bytrix.domain.model.HealthAppointment

@Composable
fun HealthcareScreen(
    onBack: () -> Unit,
    onOpenAppointment: (HealthAppointment) -> Unit,
    onCheckIn: () -> Unit,
    onCheckInHistory: () -> Unit,
    viewModel: HealthcareViewModel = hiltViewModel(),
) {
    val paging by viewModel.paging.collectAsStateWithLifecycle()
    val savedCount by viewModel.savedCheckInCount.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { SimpleAppBar(title = "Healthcare", onBack = onBack) },
        containerColor = AppColors.LightBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                SettingsRow(
                    title = "Patient Check-in",
                    subtitle = "Scan a form and let on-device AI fill it in",
                    onClick = onCheckIn,
                )
                SettingsRow(
                    title = "Saved Check-ins",
                    subtitle = if (savedCount == 0) {
                        "Nothing saved yet"
                    } else {
                        "$savedCount saved on this device"
                    },
                    onClick = onCheckInHistory,
                )
            }
            Spacer(Modifier.height(8.dp))

            Box(modifier = Modifier.weight(1f)) {
                PagedList(
                    state = paging,
                    onRetry = viewModel::loadFirstPage,
                    onLoadMore = viewModel::loadNextPage,
                    key = { it.id },
                    emptyText = "No appointments yet",
                ) { appointment ->
                    RecordRow(
                        title = appointment.doctorName,
                        subtitle = listOf(appointment.specialty, appointment.date, appointment.time)
                            .filter { it.isNotEmpty() }
                            .joinToString(" · "),
                        imageUrl = appointment.imageUrl.takeIf { it.isNotEmpty() },
                        onClick = { onOpenAppointment(appointment) },
                    )
                }
            }
        }
    }
}
