package com.app.aisheildme.bytrix.feature.retail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.aisheildme.bytrix.core.designsystem.AppColors
import com.app.aisheildme.bytrix.core.designsystem.component.DetailCard
import com.app.aisheildme.bytrix.core.designsystem.component.DetailLine
import com.app.aisheildme.bytrix.core.designsystem.component.EmptyState
import com.app.aisheildme.bytrix.core.designsystem.component.PagedList
import com.app.aisheildme.bytrix.core.designsystem.component.RecordRow
import com.app.aisheildme.bytrix.core.designsystem.component.SimpleAppBar
import com.app.aisheildme.bytrix.data.remote.dto.Money
import com.app.aisheildme.bytrix.domain.model.RetailOrder

@Composable
fun RetailScreen(
    onBack: () -> Unit,
    onOpenOrder: (RetailOrder) -> Unit,
    viewModel: RetailViewModel = hiltViewModel(),
) {
    val paging by viewModel.paging.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { SimpleAppBar(title = "Retail", onBack = onBack) },
        containerColor = AppColors.LightBackground,
    ) { padding ->
        PagedList(
            state = paging,
            onRetry = viewModel::loadFirstPage,
            onLoadMore = viewModel::loadNextPage,
            key = { it.id },
            emptyText = "No orders yet",
            modifier = Modifier.padding(padding),
        ) { order ->
            RecordRow(
                title = order.brandName,
                subtitle = order.shortOrderId,
                trailingText = Money.dollars(order.total),
                imageUrl = order.logoUrl.takeIf { it.isNotEmpty() },
                onClick = { onOpenOrder(order) },
            )
        }
    }
}

@Composable
fun RetailDetailScreen(
    order: RetailOrder?,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = { SimpleAppBar(title = "Order Details", onBack = onBack) },
        containerColor = AppColors.LightBackground,
    ) { padding ->
        if (order == null) {
            EmptyState(
                text = "This order is no longer available.",
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
            DetailCard {
                DetailLine("Merchant", order.brandName)
                DetailLine("Order ID", order.shortOrderId)
                if (order.orderTime.isNotEmpty()) DetailLine("Placed", order.orderTime)
                DetailLine("Payment", order.paymentMethod)
            }

            if (order.items.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                DetailCard {
                    Text(
                        text = "Items",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.BrandText,
                    )
                    Spacer(Modifier.height(8.dp))
                    order.items.forEach { item ->
                        DetailLine(
                            label = "${item.qty}× ${item.name}",
                            value = Money.dollars(item.price),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            DetailCard {
                DetailLine("Subtotal", Money.dollars(order.subtotal))
                if (order.promoVoucher != 0.0) {
                    DetailLine("Promo voucher", "-${Money.dollars(order.promoVoucher)}")
                }
                if (order.coinsReward != 0.0) {
                    DetailLine("Coins reward", "-${Money.dollars(order.coinsReward)}")
                }
                if (order.deliveryFee != 0.0) {
                    DetailLine("Delivery fee", Money.dollars(order.deliveryFee))
                }
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = AppColors.LightBorder)
                Spacer(Modifier.height(4.dp))
                DetailLine("Total", Money.dollars(order.total), emphasised = true)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
