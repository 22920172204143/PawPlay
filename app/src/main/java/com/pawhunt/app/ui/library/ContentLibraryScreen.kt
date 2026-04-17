package com.pawhunt.app.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pawhunt.app.model.BackgroundLibrary
import com.pawhunt.app.model.GameBackground
import com.pawhunt.app.model.Toy
import com.pawhunt.app.model.ToyLibrary
import com.pawhunt.app.theme.PawFreeGreen
import com.pawhunt.app.theme.PawLockGray
import com.pawhunt.app.theme.PawPrimary
import com.pawhunt.app.theme.PawProGold
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContentLibraryScreen(
    isPro: Boolean,
    selectedToyId: Int,
    selectedBgId: Int,
    onSelectToy: (Int) -> Unit,
    onSelectBg: (Int) -> Unit,
    onBack: () -> Unit,
    onPaywall: () -> Unit
) {
    val tabs = listOf("Toys", "Backgrounds")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Content Library", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(title) }
                    )
                }
            }

            HorizontalPager(state = pagerState) { page ->
                when (page) {
                    0 -> ToyGrid(
                        toys = ToyLibrary.allToys,
                        isPro = isPro,
                        selectedId = selectedToyId,
                        onSelect = { toy ->
                            if (toy.isFree || isPro) {
                                onSelectToy(toy.id)
                            } else {
                                onPaywall()
                            }
                        }
                    )
                    1 -> BackgroundGrid(
                        backgrounds = BackgroundLibrary.allBackgrounds,
                        isPro = isPro,
                        selectedId = selectedBgId,
                        onSelect = { bg ->
                            if (bg.isFree || isPro) {
                                onSelectBg(bg.id)
                            } else {
                                onPaywall()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ToyGrid(
    toys: List<Toy>,
    isPro: Boolean,
    selectedId: Int,
    onSelect: (Toy) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(120.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(toys) { toy ->
            val isLocked = !toy.isFree && !isPro
            val isSelected = toy.id == selectedId

            ToyCard(
                toy = toy,
                isLocked = isLocked,
                isSelected = isSelected,
                onClick = { onSelect(toy) }
            )
        }
    }
}

@Composable
private fun ToyCard(
    toy: Toy,
    isLocked: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(0.85f)
            .then(
                if (isSelected) Modifier.border(3.dp, PawPrimary, RoundedCornerShape(16.dp))
                else Modifier
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = toy.emoji, fontSize = 40.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = toy.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusBadge(isFree = toy.isFree, isLocked = isLocked, isSelected = isSelected)
            }
        }
    }
}

@Composable
private fun BackgroundGrid(
    backgrounds: List<GameBackground>,
    isPro: Boolean,
    selectedId: Int,
    onSelect: (GameBackground) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(140.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(backgrounds) { bg ->
            val isLocked = !bg.isFree && !isPro
            val isSelected = bg.id == selectedId

            BackgroundCard(
                bg = bg,
                isLocked = isLocked,
                isSelected = isSelected,
                onClick = { onSelect(bg) }
            )
        }
    }
}

@Composable
private fun BackgroundCard(
    bg: GameBackground,
    isLocked: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1.4f)
            .then(
                if (isSelected) Modifier.border(3.dp, PawPrimary, RoundedCornerShape(16.dp))
                else Modifier
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color(
                        bg.baseColor.red,
                        bg.baseColor.green,
                        bg.baseColor.blue,
                        if (isLocked) 0.4f else 1f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = bg.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusBadge(isFree = bg.isFree, isLocked = isLocked, isSelected = isSelected)
            }

            if (isLocked) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(isFree: Boolean, isLocked: Boolean, isSelected: Boolean) {
    val (text, color) = when {
        isSelected -> "✓ Selected" to PawPrimary
        isLocked -> "PRO" to PawProGold
        isFree -> "FREE" to PawFreeGreen
        else -> "Unlocked" to PawPrimary
    }

    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}
