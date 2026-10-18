package com.app.aisheildme.bytrix.feature.healthcare

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.aisheildme.bytrix.core.designsystem.AppColors
import com.app.aisheildme.bytrix.core.designsystem.component.DetailCard
import com.app.aisheildme.bytrix.core.designsystem.component.DetailLine
import com.app.aisheildme.bytrix.core.designsystem.component.EmptyState
import com.app.aisheildme.bytrix.core.designsystem.component.SimpleAppBar
import com.app.aisheildme.bytrix.core.designsystem.component.UserAvatar
import com.app.aisheildme.bytrix.domain.model.HealthAppointment

@Composable
fun AppointmentDetailScreen(
    appointment: HealthAppointment?,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = { SimpleAppBar(title = "Appointment", onBack = onBack) },
        containerColor = AppColors.LightBackground,
    ) { padding ->
        if (appointment == null) {
            EmptyState(
                text = "This appointment is no longer available.",
                modifier = Modifier.padding(padding),
                actionText = "Go back",
                onAction = onBack,
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                UserAvatar(
                    imageUrl = appointment.imageUrl.takeIf { it.isNotEmpty() },
                    size = 96.dp,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = appointment.doctorName,
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColors.BrandText,
                )
                Text(
                    text = appointment.specialty,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextHint,
                )
            }

            Spacer(Modifier.height(20.dp))

            DetailCard {
                DetailLine("Date", appointment.date.ifEmpty { "—" })
                DetailLine("Time", appointment.time.ifEmpty { "—" })
                DetailLine("Location", appointment.location)
                if (appointment.experience.isNotEmpty()) {
                    DetailLine("Experience", appointment.experience)
                }
                if (appointment.treated.isNotEmpty()) {
                    DetailLine("Patients treated", appointment.treated)
                }
                if (appointment.hourlyRate.isNotEmpty()) {
                    DetailLine("Rate", appointment.hourlyRate)
                }
            }

            if (appointment.about.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                DetailCard {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.BrandText,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = appointment.about,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextHint,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
