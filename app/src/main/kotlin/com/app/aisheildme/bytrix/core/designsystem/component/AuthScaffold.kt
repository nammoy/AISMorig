package com.app.aisheildme.bytrix.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.aisheildme.bytrix.core.designsystem.AppColors

@Composable
fun AuthHeader(
    titleTop: String,
    titleBottom: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = titleTop,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.BlackText,
        )
        Text(
            text = titleBottom,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.BrandBlue,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextHint,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
fun AuthBottomLink(
    prompt: String,
    action: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = prompt,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.BlackText,
        )
        Text(
            text = " $action",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.BrandBlue,
            modifier = Modifier.clickable(onClick = onClick),
        )
    }
}
