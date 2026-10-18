package com.app.aisheildme.bytrix.feature.banking

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.aisheildme.bytrix.core.designsystem.AppColors
import com.app.aisheildme.bytrix.core.designsystem.component.AppSnackbarHost
import com.app.aisheildme.bytrix.core.designsystem.component.CustomButton
import com.app.aisheildme.bytrix.core.designsystem.component.DetailCard
import com.app.aisheildme.bytrix.core.designsystem.component.DetailLine
import com.app.aisheildme.bytrix.core.designsystem.component.EmptyState
import com.app.aisheildme.bytrix.core.designsystem.component.FullScreenLoader
import com.app.aisheildme.bytrix.core.designsystem.component.PagedList
import com.app.aisheildme.bytrix.core.designsystem.component.RecordRow
import com.app.aisheildme.bytrix.core.designsystem.component.SimpleAppBar
import com.app.aisheildme.bytrix.core.util.FileOpener
import com.app.aisheildme.bytrix.domain.model.BankStatement
import kotlinx.coroutines.launch

@Composable
fun BankingScreen(
    onBack: () -> Unit,
    onOpenStatement: (BankStatement) -> Unit,
    viewModel: BankingViewModel = hiltViewModel(),
) {
    val paging by viewModel.paging.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { SimpleAppBar(title = "Banking", onBack = onBack) },
        containerColor = AppColors.LightBackground,
    ) { padding ->
        PagedList(
            state = paging,
            onRetry = viewModel::loadFirstPage,
            onLoadMore = viewModel::loadNextPage,
            key = { it.id },
            emptyText = "No statements yet",
            modifier = Modifier.padding(padding),
        ) { statement ->
            RecordRow(
                title = statement.name,
                subtitle = statement.shortStatementId,
                trailingText = statement.amountText,
                onClick = { onOpenStatement(statement) },
            )
        }
    }
}

@Composable
fun BankStatementDetailScreen(
    statement: BankStatement?,
    fileOpener: FileOpener,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isDownloading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { SimpleAppBar(title = "Statement", onBack = onBack) },
        snackbarHost = { AppSnackbarHost(snackbarHostState, isError = true) },
        containerColor = AppColors.LightBackground,
    ) { padding ->
        if (statement == null) {
            EmptyState(
                text = "This statement is no longer available.",
                modifier = Modifier.padding(padding),
                actionText = "Go back",
                onAction = onBack,
            )
            return@Scaffold
        }

        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
            ) {
                DetailCard {
                    DetailLine("Statement", statement.name)
                    DetailLine("Reference", statement.shortStatementId)
                    if (statement.dateTime.isNotEmpty()) DetailLine("Issued", statement.dateTime)
                    if (statement.amountText.isNotEmpty()) {
                        DetailLine("Amount", statement.amountText, emphasised = true)
                    }
                }

                if (statement.transactions.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    DetailCard {
                        Text(
                            text = "Transactions",
                            style = MaterialTheme.typography.titleMedium,
                            color = AppColors.BrandText,
                        )
                        Spacer(Modifier.height(8.dp))
                        statement.transactions.forEach { transaction ->
                            DetailLine(transaction.description, transaction.amount)
                        }
                    }
                }

                if (statement.hasPdf) {
                    Spacer(Modifier.height(24.dp))
                    CustomButton(
                        title = "Open PDF",
                        isLoading = isDownloading,
                        height = 44.dp,
                        onClick = {
                            scope.launch {
                                isDownloading = true
                                val error = fileOpener.downloadAndOpen(
                                    context = context,
                                    url = statement.pdfUrl.orEmpty(),
                                    fileName = "${statement.name.ifEmpty { "statement" }}.pdf",
                                    mimeType = "application/pdf",
                                )
                                isDownloading = false
                                error?.let { snackbarHostState.showSnackbar(it) }
                            }
                        },
                    )
                }

                Spacer(Modifier.height(24.dp))
            }

            if (isDownloading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    FullScreenLoader()
                }
            }
        }
    }
}
