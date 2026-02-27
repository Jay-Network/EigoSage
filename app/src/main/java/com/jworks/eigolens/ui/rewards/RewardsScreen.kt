package com.jworks.eigolens.ui.rewards

import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jworks.eigolens.R

@Composable
fun RewardsScreen(
    onBackClick: () -> Unit,
    viewModel: RewardsViewModel = hiltViewModel()
) {
    val coinBalance by viewModel.coinBalance.collectAsState()
    val tier by viewModel.tier.collectAsState()
    val freeAiRemaining by viewModel.freeAiRemaining.collectAsState()
    val streakFreezesOwned by viewModel.streakFreezesOwned.collectAsState()
    val unlockedThemes by viewModel.unlockedThemes.collectAsState()
    val isPurchasing by viewModel.isPurchasing.collectAsState()
    val streakDays by viewModel.streakDays.collectAsState()
    val totalScans by viewModel.totalScans.collectAsState()
    val totalCefrWords by viewModel.totalCefrWords.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.purchaseResult.collect { result ->
            when (result) {
                is PurchaseResult.Success -> Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                is PurchaseResult.Error -> Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = "Coin Shop",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Balance Card
        BalanceCard(balance = coinBalance, tier = tier, streakDays = streakDays, totalScans = totalScans, totalCefrWords = totalCefrWords)

        Spacer(modifier = Modifier.height(24.dp))

        // Consumables Section
        Text(
            text = "Consumables",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))

        RewardsViewModel.CONSUMABLE_ITEMS.forEach { item ->
            val subtitle = when (item.id) {
                "ai_deep_analysis" -> if (freeAiRemaining > 0) "$freeAiRemaining free today" else "Free quota used"
                "streak_freeze" -> "Owned: $streakFreezesOwned"
                else -> null
            }
            ShopItemCard(
                item = item,
                coinBalance = coinBalance,
                isPurchasing = isPurchasing,
                subtitle = subtitle,
                onPurchase = { viewModel.purchaseItem(item) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(16.dp))

        // Permanent Unlocks Section
        Text(
            text = "Permanent Unlocks",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))

        RewardsViewModel.THEME_ITEMS.forEach { item ->
            val themeId = item.id.removePrefix("theme_")
            val isUnlocked = themeId in unlockedThemes
            ShopItemCard(
                item = item,
                coinBalance = coinBalance,
                isPurchasing = isPurchasing,
                isOwned = isUnlocked,
                onPurchase = { viewModel.purchaseItem(item) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun BalanceCard(
    balance: Int,
    tier: String,
    streakDays: Int,
    totalScans: Int,
    totalCefrWords: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFFFFD700), Color(0xFFFFA000))
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "\uD83E\uDE99", fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "$balance",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "J Coins \u2022 ${tier.replaceFirstChar { it.uppercase() }} Tier",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black.copy(alpha = 0.7f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatChip(label = "Streak", value = "${streakDays}d")
                    StatChip(label = "Scans", value = "$totalScans")
                    StatChip(label = "Words", value = "$totalCefrWords")
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Black.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun ShopItemCard(
    item: SpendItem,
    coinBalance: Int,
    isPurchasing: Boolean,
    subtitle: String? = null,
    isOwned: Boolean = false,
    onPurchase: () -> Unit
) {
    val canAfford = coinBalance >= item.cost

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Text(
                text = item.icon,
                fontSize = 28.sp,
                modifier = Modifier.padding(end = 12.dp)
            )

            // Name + description
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Price + buy button
            if (isOwned) {
                Text(
                    text = "Owned",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            } else {
                Button(
                    onClick = onPurchase,
                    enabled = canAfford && !isPurchasing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD700),
                        contentColor = Color.Black,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f),
                        disabledContentColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isPurchasing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.Black
                        )
                    } else {
                        Text(
                            text = "\uD83E\uDE99 ${item.cost}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
