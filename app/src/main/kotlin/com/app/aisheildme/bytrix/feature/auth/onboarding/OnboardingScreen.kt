package com.app.aisheildme.bytrix.feature.auth.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import com.app.aisheildme.bytrix.R
import com.app.aisheildme.bytrix.core.designsystem.AppColors
import com.app.aisheildme.bytrix.core.designsystem.component.CustomButton
import kotlinx.coroutines.launch

private data class OnboardingPage(
    @param:DrawableRes val image: Int,
    val title: String,
    val body: String,
)

private val PAGES = listOf(
    OnboardingPage(
        image = R.drawable.onb1,
        title = "Your documents, secured",
        body = "Store records, IDs and statements in a vault only you can open.",
    ),
    OnboardingPage(
        image = R.drawable.onb2,
        title = "Check in without the clipboard",
        body = "Scan a paper form and let on-device AI fill in the rest.",
    ),
    OnboardingPage(
        image = R.drawable.onb3,
        title = "Share on your terms",
        body = "Grant access to a doctor or family member, and revoke it any time.",
    ),
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { PAGES.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == PAGES.lastIndex

    Scaffold(containerColor = AppColors.LightBackground) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onFinished) {
                    Text("Skip", color = AppColors.TextHint)
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                val item = PAGES[page]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Image(
                        painter = painterResource(item.image),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(24.dp)),
                    )
                    Spacer(Modifier.height(40.dp))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = AppColors.BlackText,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = item.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextHint,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(PAGES.size) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(width = if (selected) 20.dp else 8.dp, height = 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) AppColors.BrandBlue else AppColors.CheckboxBorder,
                            ),
                    )
                }
            }

            Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                CustomButton(
                    title = if (isLastPage) "Get Started" else "Next",
                    onClick = {
                        if (isLastPage) {
                            onFinished()
                        } else {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    },
                    height = 44.dp,
                )
            }
        }
    }
}
