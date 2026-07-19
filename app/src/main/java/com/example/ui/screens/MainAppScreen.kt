package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.ui.theme.*
import com.example.ui.translation.AppTranslation
import com.example.data.model.BatterItem
import com.example.data.model.Order
import com.example.ui.viewmodel.CartDisplayItem
import com.example.ui.viewmodel.CartSummary
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val activeTrackOrderId by viewModel.activeTrackOrderId.collectAsState()
    val showApplePay by viewModel.showApplePaySheet.collectAsState()
    val isProcessingPayment by viewModel.isProcessingPayment.collectAsState()
    val paymentSuccess by viewModel.paymentSuccess.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    var showTranslateDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    if (showTranslateDialog) {
        GoogleTranslateDialog(
            viewModel = viewModel,
            currentLanguage = currentLanguage,
            onDismiss = { showTranslateDialog = false }
        )
    }

    Scaffold(
        topBar = {
            GoogleTranslateTopBar(
                viewModel = viewModel,
                currentLanguage = currentLanguage,
                onTranslateClick = { showTranslateDialog = true }
            )
        },
        bottomBar = {
            AppleBottomNavigation(
                currentScreen = currentScreen,
                onTabSelected = { viewModel.navigateTo(it) },
                translate = { viewModel.translate(it) },
                currentLanguage = currentLanguage
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            val isCartDrawerOpen by viewModel.isCartDrawerOpen.collectAsState()
            val cart by viewModel.cartItems.collectAsState()

            Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                when (screen) {
                    is Screen.Shop -> ShopTabScreen(viewModel, currentLanguage)
                    is Screen.Cart -> CartTabScreen(viewModel, currentLanguage)
                    is Screen.Orders -> OrdersTabScreen(viewModel, activeTrackOrderId, currentLanguage)
                    is Screen.Profile -> ProfileTabScreen(currentLanguage, translate = { viewModel.translate(it) })
                }
            }

            // Floating Cart Button with Badge (Visible on Shop, Orders, Profile screens when items are in cart)
            val cartItemsCount = cart.sumOf { it.quantity }
            if (cartItemsCount > 0 && currentScreen != Screen.Cart && !isCartDrawerOpen) {
                FloatingActionButton(
                    onClick = { viewModel.setCartDrawerOpen(true) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp, end = 16.dp)
                        .testTag("floating_cart_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.ShoppingCart,
                            contentDescription = "View Cart Drawer",
                            modifier = Modifier.size(24.dp)
                        )
                        // Badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 6.dp, y = (-6).dp)
                                .background(Color.Red, CircleShape)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = cartItemsCount.toString(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // Drawer Backdrop Scrim Overlay
            if (isCartDrawerOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { viewModel.setCartDrawerOpen(false) }
                )
            }

            // Animated Sliding Cart Drawer
            AnimatedVisibility(
                visible = isCartDrawerOpen,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it }),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                CartDrawer(
                    viewModel = viewModel,
                    currentLanguage = currentLanguage,
                    onDismiss = { viewModel.setCartDrawerOpen(false) }
                )
            }

            // Apple Pay Bottom Sheet
            if (showApplePay) {
                ApplePayBottomSheet(
                    viewModel = viewModel,
                    isProcessing = isProcessingPayment,
                    currentLanguage = currentLanguage,
                    onDismiss = { viewModel.setApplePaySheetVisible(false) },
                    onConfirm = { viewModel.checkout("Apple Pay", "8824") }
                )
            }

            // Payment success dialog/alert overlay
            if (paymentSuccess) {
                Dialog(onDismissRequest = { viewModel.resetPaymentSuccess() }) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(Color(0xFF34C759).copy(alpha = 0.15f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Success",
                                    tint = Color(0xFF34C759),
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Payment Secured",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your order has been placed successfully and has entered preparation mode.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { viewModel.resetPaymentSuccess() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Track Live Delivery", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppleBottomNavigation(
    currentScreen: Screen,
    onTabSelected: (Screen) -> Unit,
    translate: (String) -> String,
    currentLanguage: String
) {
    Surface(
        color = GourmetSlightGray,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .border(
                width = 1.dp,
                color = GourmetClayLight.copy(alpha = 0.4f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomTabItem(
                label = translate("Shop"),
                iconSelected = Icons.Rounded.Storefront,
                iconUnselected = Icons.Outlined.Storefront,
                isSelected = currentScreen is Screen.Shop,
                onClick = { onTabSelected(Screen.Shop) },
                tag = "tab_shop"
            )
            BottomTabItem(
                label = translate("Cart"),
                iconSelected = Icons.Rounded.ShoppingCart,
                iconUnselected = Icons.Outlined.ShoppingCart,
                isSelected = currentScreen is Screen.Cart,
                onClick = { onTabSelected(Screen.Cart) },
                tag = "tab_cart"
            )
            BottomTabItem(
                label = translate("Orders"),
                iconSelected = Icons.Rounded.DirectionsRun,
                iconUnselected = Icons.Outlined.DirectionsRun,
                isSelected = currentScreen is Screen.Orders,
                onClick = { onTabSelected(Screen.Orders) },
                tag = "tab_orders"
            )
            BottomTabItem(
                label = translate("About"),
                iconSelected = Icons.Rounded.Info,
                iconUnselected = Icons.Outlined.Info,
                isSelected = currentScreen is Screen.Profile,
                onClick = { onTabSelected(Screen.Profile) },
                tag = "tab_profile"
            )
        }
    }
}

@Composable
fun BottomTabItem(
    label: String,
    iconSelected: ImageVector,
    iconUnselected: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    val scaleAnim by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "tabScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag(tag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(64.dp)
                .height(32.dp)
                .background(
                    color = if (isSelected) GourmetPeachLight else Color.Transparent,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Icon(
                imageVector = if (isSelected) iconSelected else iconUnselected,
                contentDescription = label,
                tint = if (isSelected) GourmetPrimaryLight else GourmetSubtextLight.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = if (isSelected) GourmetPrimaryLight else GourmetSubtextLight.copy(alpha = 0.7f)
        )
    }
}

// ---------------- SHOP TAB ----------------

@Composable
fun ShopTabScreen(viewModel: MainViewModel, currentLanguage: String) {
    val items by viewModel.allBatterItems.collectAsState()
    val cart by viewModel.cartItems.collectAsState()
    val reviews by viewModel.allReviews.collectAsState()
    var selectedCategory by remember { mutableStateOf("All") }
    var showReviewsDialog by remember { mutableStateOf(false) }

    if (showReviewsDialog) {
        AppleReviewsDialog(
            viewModel = viewModel,
            onDismiss = { showReviewsDialog = false }
        )
    }

    val categories = listOf("All", "Traditional Batter")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("shop_screen"),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Hero Header Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_vn_hero_1784440500402),
                    contentDescription = viewModel.translate("V&N Foods") + " Hero",
                    modifier = Modifier.fillMaxSize(),
                    alignment = Alignment.Center,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                // Linear gradient scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                                startY = 100f
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = viewModel.translate("ESTD 2026"),
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = viewModel.translate("V&N Foods"),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = viewModel.translate("Freshly fermented, stone-ground gourmet idli & dosa batters delivered to your doorstep daily."),
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Category Row
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = viewModel.translate("Select Category"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { category ->
                    val isSelected = category == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(20.dp)
                              )
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = viewModel.translate(category),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Section Title
        item {
            Text(
                text = viewModel.translate("Our Fresh Batters & Sides"),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Grid List of Batter Items
        val filteredItems = if (selectedCategory == "All") items else items.filter { it.category == selectedCategory }

        if (filteredItems.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = viewModel.translate("No items available in this category."),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            items(filteredItems) { batterItem ->
                val cartQty = cart.find { it.itemId == batterItem.id }?.quantity ?: 0
                BatterCard(
                    item = batterItem,
                    quantityInCart = cartQty,
                    onAdd = { viewModel.addToCart(batterItem.id, 1) },
                    onSubtract = { viewModel.addToCart(batterItem.id, -1) },
                    reviewCount = reviews.size,
                    onReviewsClick = { showReviewsDialog = true },
                    translate = { viewModel.translate(it) }
                )
            }
        }
    }
}

@Composable
fun BatterCard(
    item: BatterItem,
    quantityInCart: Int,
    onAdd: () -> Unit,
    onSubtract: () -> Unit,
    reviewCount: Int,
    onReviewsClick: () -> Unit,
    translate: (String) -> String
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                RoundedCornerShape(18.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.isBestSeller) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFF9500), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    translate("Bestseller").uppercase(),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                item.size,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = translate(item.name),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "₹${String.format(Locale.US, "%.2f", item.price)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = translate(item.description),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onReviewsClick() }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                            .testTag("batter_card_reviews_row_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFFCC00),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${item.rating} • ($reviewCount ${translate("Reviews").lowercase()})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GourmetPrimaryLight,
                            textDecoration = TextDecoration.Underline
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Rounded.AccessTime,
                        contentDescription = "Time",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = translate(item.prepTime),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                if (quantityInCart > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        IconButton(
                            onClick = onSubtract,
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color.White, CircleShape)
                        ) {
                            Icon(Icons.Rounded.Remove, "Remove", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            text = quantityInCart.toString(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        IconButton(
                            onClick = onAdd,
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color.White, CircleShape)
                        ) {
                            Icon(Icons.Rounded.Add, "Add", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else {
                    Button(
                        onClick = onAdd,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(100.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Rounded.Add, "Add", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(translate("Add to Cart"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ---------------- CART TAB ----------------

@Composable
fun CartTabScreen(viewModel: MainViewModel, currentLanguage: String) {
    val cartDisplayList by viewModel.cartDisplayItems.collectAsState()
    val summary by viewModel.cartSummary.collectAsState()

    var selectedPaymentMethod by remember { mutableStateOf("UPI") } // "UPI" or "COD"
    var qrSeed by remember { mutableStateOf(System.currentTimeMillis()) }
    var txnId by remember { mutableStateOf("TXN" + System.currentTimeMillis().toString().takeLast(8)) }
    var remainingSeconds by remember { mutableStateOf(120) }

    val onRefresh = {
        qrSeed = System.currentTimeMillis()
        txnId = "TXN" + System.currentTimeMillis().toString().takeLast(8)
    }

    LaunchedEffect(qrSeed) {
        remainingSeconds = 120
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
        }
        qrSeed = System.currentTimeMillis()
        txnId = "TXN" + System.currentTimeMillis().toString().takeLast(8)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("cart_screen")
    ) {
        // Toolbar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = viewModel.translate("Shopping Cart"),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            if (cartDisplayList.isNotEmpty()) {
                Text(
                    text = viewModel.translate("Clear Cart"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clickable { viewModel.clearCart() }
                )
            }
        }

        if (cartDisplayList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(100.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ShoppingCart,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = viewModel.translate("Your cart is empty"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = viewModel.translate("Browse our shop and pick fresh batters!"),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.navigateTo(Screen.Shop) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(viewModel.translate("Start Shopping"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(cartDisplayList) { displayItem ->
                    CartItemCard(
                        displayItem = displayItem,
                        onAdd = { viewModel.addToCart(displayItem.item.id, 1) },
                        onSubtract = { viewModel.addToCart(displayItem.item.id, -1) },
                        onRemove = { viewModel.removeFromCart(displayItem.item.id) },
                        translate = { viewModel.translate(it) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    OrderSummaryCard(summary = summary, translate = { viewModel.translate(it) })
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    // Secured badge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VerifiedUser,
                            contentDescription = "Secured",
                            tint = Color(0xFF34C759),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            viewModel.translate("Secured and processed via V&N Encrypted Gateway"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF34C759)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    PaymentSelectionToggle(
                        selectedMethod = selectedPaymentMethod,
                        onSelect = { selectedPaymentMethod = it },
                        translate = { viewModel.translate(it) }
                    )
                }

                if (selectedPaymentMethod == "UPI") {
                    item {
                        UpiPaymentSection(
                            viewModel = viewModel,
                            summary = summary,
                            qrSeed = qrSeed,
                            txnId = txnId,
                            remainingSeconds = remainingSeconds,
                            onRefresh = onRefresh
                        )
                    }
                } else {
                    item {
                        CodPaymentSection(
                            viewModel = viewModel,
                            summary = summary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CartItemCard(
    displayItem: CartDisplayItem,
    onAdd: () -> Unit,
    onSubtract: () -> Unit,
    onRemove: () -> Unit,
    translate: (String) -> String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = translate(displayItem.item.name),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = translate(displayItem.item.size),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "₹${String.format(Locale.US, "%.2f", displayItem.item.price)} " + translate("each"),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(100.dp))
                    .padding(4.dp)
            ) {
                IconButton(
                    onClick = onSubtract,
                    modifier = Modifier
                        .size(26.dp)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(Icons.Rounded.Remove, "Remove", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = displayItem.quantity.toString(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
                IconButton(
                    onClick = onAdd,
                    modifier = Modifier
                        .size(26.dp)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(Icons.Rounded.Add, "Add", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete",
                    tint = Color(0xFFFF3B30),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun OrderSummaryCard(summary: CartSummary, translate: (String) -> String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                RoundedCornerShape(18.dp)
            )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = translate("Order Summary"),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(translate("Subtotal"), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Text("₹${String.format(Locale.US, "%.2f", summary.subtotal)}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(translate("Tax") + " (5%)", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Text("₹${String.format(Locale.US, "%.2f", summary.tax)}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(translate("Delivery Fee"), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Text(
                    text = if (summary.deliveryFee == 0.0) translate("FREE") else "₹${String.format(Locale.US, "%.2f", summary.deliveryFee)}",
                    fontSize = 14.sp,
                    fontWeight = if (summary.deliveryFee == 0.0) FontWeight.Bold else FontWeight.Normal,
                    color = if (summary.deliveryFee == 0.0) Color(0xFF34C759) else MaterialTheme.colorScheme.onSurface
                )
            }
            if (summary.subtotal <= 15.0) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        translate("Add") + " ₹${String.format(Locale.US, "%.2f", 15.0 - summary.subtotal)} " + translate("more to qualify for FREE delivery!"),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(translate("Total Amount"), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = "₹${String.format(Locale.US, "%.2f", summary.total)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun PaymentSelectionToggle(
    selectedMethod: String,
    onSelect: (String) -> Unit,
    translate: (String) -> String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (selectedMethod == "UPI") GourmetPrimaryLight else Color.Transparent)
                .clickable { onSelect("UPI") }
                .padding(vertical = 10.dp)
                .testTag("pay_toggle_upi"),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.QrCode,
                    contentDescription = "UPI QR",
                    tint = if (selectedMethod == "UPI") Color.White else GourmetTextDark,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    translate("UPI QR Pay"),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedMethod == "UPI") Color.White else GourmetTextDark
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (selectedMethod == "COD") GourmetPrimaryLight else Color.Transparent)
                .clickable { onSelect("COD") }
                .padding(vertical = 10.dp)
                .testTag("pay_toggle_cod"),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.LocalShipping,
                    contentDescription = "COD",
                    tint = if (selectedMethod == "COD") Color.White else GourmetTextDark,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    translate("Cash on Delivery"),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedMethod == "COD") Color.White else GourmetTextDark
                )
            }
        }
    }
}

fun generateQrMatrix(seed: Long): Array<BooleanArray> {
    val size = 21
    val matrix = Array(size) { BooleanArray(size) }
    val random = java.util.Random(seed)

    // Fill with random noise first
    for (r in 0 until size) {
        for (c in 0 until size) {
            matrix[r][c] = random.nextBoolean()
        }
    }

    // Draw Finder Patterns (7x7)
    fun drawFinder(rowOffset: Int, colOffset: Int) {
        for (r in 0..6) {
            for (c in 0..6) {
                val realR = rowOffset + r
                val realC = colOffset + c
                val isOuterBorder = r == 0 || r == 6 || c == 0 || c == 6
                val isCenter = r in 2..4 && c in 2..4
                matrix[realR][realC] = isOuterBorder || isCenter
            }
        }
    }

    // Top-Left Finder
    drawFinder(0, 0)
    // Top-Right Finder
    drawFinder(0, size - 7)
    // Bottom-Left Finder
    drawFinder(size - 7, 0)

    // Clear a 5x5 area in the center for the UPI badge
    val centerStart = size / 2 - 2
    val centerEnd = size / 2 + 2
    for (r in centerStart..centerEnd) {
        for (c in centerStart..centerEnd) {
            matrix[r][c] = false
        }
    }

    return matrix
}

@Composable
fun GourmetQRCode(
    seed: Long,
    modifier: Modifier = Modifier
) {
    val matrix = remember(seed) { generateQrMatrix(seed) }
    
    Box(
        modifier = modifier
            .size(180.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, GourmetClayLight.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val size = 21
            val cellSize = this.size.width / size
            
            for (r in 0 until size) {
                for (c in 0 until size) {
                    val centerStart = size / 2 - 2
                    val centerEnd = size / 2 + 2
                    if (r in centerStart..centerEnd && c in centerStart..centerEnd) {
                        continue
                    }
                    
                    if (matrix[r][c]) {
                        drawRect(
                            color = GourmetTextDark,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = androidx.compose.ui.geometry.Size(cellSize + 0.5f, cellSize + 0.5f)
                        )
                    }
                }
            }
        }
        
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(Color.White, RoundedCornerShape(4.dp))
                .border(1.5.dp, GourmetPrimaryLight, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "UPI",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = GourmetPrimaryLight
            )
        }
    }
}

@Composable
fun UpiPaymentSection(
    viewModel: MainViewModel,
    summary: CartSummary,
    qrSeed: Long,
    txnId: String,
    remainingSeconds: Int,
    onRefresh: () -> Unit
) {
    val isProcessing by viewModel.isProcessingPayment.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = GourmetSlightGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = viewModel.translate("V&N Foods").uppercase() + " PVT LTD",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GourmetSubtextLight,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "vn.foods@upi",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = GourmetTextDark
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Divider(color = GourmetClayLight.copy(alpha = 0.2f))
                
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = viewModel.translate("Amount to Pay"),
                    fontSize = 12.sp,
                    color = GourmetSubtextLight,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "₹${String.format(Locale.US, "%.2f", summary.total)}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = GourmetPrimaryLight
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GourmetQRCode(seed = qrSeed)

                Spacer(modifier = Modifier.height(12.dp))

                val minutesStr = (remainingSeconds / 60).toString().padStart(2, '0')
                val secondsStr = (remainingSeconds % 60).toString().padStart(2, '0')

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(GourmetPeachLight, RoundedCornerShape(100.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccessTime,
                        contentDescription = "Timer",
                        tint = GourmetPrimaryLight,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = viewModel.translate("QR Code refreshes in") + " $minutesStr:$secondsStr",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GourmetPrimaryLight
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = viewModel.translate("Refresh Now"),
                            tint = GourmetPrimaryLight,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = viewModel.translate("Ref ID") + ": $txnId",
                    fontSize = 11.sp,
                    color = GourmetSubtextLight,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.checkout("UPI", "QR-Pay") },
            enabled = !isProcessing,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GourmetPrimaryLight),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("upi_checkout_button")
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(viewModel.translate("Verifying UPI Payment..."), fontWeight = FontWeight.Bold)
            } else {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = viewModel.translate("I Have Paid • Verify & Order"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = GourmetGreenText,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = viewModel.translate("Secure UPI Auto-Verification Active"),
                fontSize = 11.sp,
                color = GourmetGreenText,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CodPaymentSection(
    viewModel: MainViewModel,
    summary: CartSummary
) {
    val isProcessing by viewModel.isProcessingPayment.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = GourmetSlightGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(GourmetPeachLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalShipping,
                            contentDescription = "COD Icon",
                            tint = GourmetPrimaryLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = viewModel.translate("Cash on Delivery") + " (COD)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = GourmetTextDark
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = GourmetClayLight.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = viewModel.translate("Terms & Instructions:"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GourmetTextDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                val bulletPoints = listOf(
                    "Pay only after receiving your order at your doorstep.",
                    "Our delivery partner accepts cash or instant UPI scan.",
                    "No extra handling/COD processing fee applied."
                )

                bulletPoints.forEach { point ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "• ",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GourmetPrimaryLight
                        )
                        Text(
                            text = viewModel.translate(point),
                            fontSize = 12.sp,
                            color = GourmetSubtextLight,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.checkout("Cash on Delivery", "COD") },
            enabled = !isProcessing,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GourmetPrimaryLight),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("cod_checkout_button")
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(viewModel.translate("Placing Order..."), fontWeight = FontWeight.Bold)
            } else {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = viewModel.translate("Confirm & Place COD Order"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun formatCardNumber(clean: String): String {
    val builder = StringBuilder()
    for (i in clean.indices) {
        builder.append(clean[i])
        if ((i + 1) % 4 == 0 && i < clean.length - 1) {
            builder.append(" ")
        }
    }
    return builder.toString()
}

private fun formatExpiry(clean: String): String {
    if (clean.length >= 2) {
        return clean.substring(0, 2) + "/" + clean.substring(2)
    }
    return clean
}

// ---------------- APPLE PAY SHEET DIALOG ----------------

@Composable
fun ApplePayBottomSheet(
    viewModel: MainViewModel,
    isProcessing: Boolean,
    currentLanguage: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val summary by viewModel.cartSummary.collectAsState()

    Dialog(onDismissRequest = { if (!isProcessing) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Fingerprint,
                            contentDescription = "Apple Pay",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apple Pay", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    }
                    if (!isProcessing) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "Close", tint = Color.White.copy(alpha = 0.5f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("PAY TO", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("V&N Foods Delivery", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("METHOD", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("Apple Card (•••• 8824)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("SHIPPING", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("Instant Home Delivery", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("AMOUNT", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("₹${String.format(Locale.US, "%.2f", summary.total)}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isProcessing) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Verifying Biometrics...", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Rounded.Fingerprint, "Confirm", tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Double-Click / Hold to Pay", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Secure biometric fingerprint or face verification will occur next.",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ---------------- ORDERS TAB ----------------

@Composable
fun OrdersTabScreen(
    viewModel: MainViewModel,
    activeTrackOrderId: String?,
    currentLanguage: String
) {
    val orders by viewModel.allOrders.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("orders_screen")
    ) {
        // Toolbar
        Text(
            text = viewModel.translate("Track Delivery"),
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        val activeOrder = orders.find { it.orderId == activeTrackOrderId }

        if (activeOrder != null) {
            ActiveOrderTrackingCard(
                order = activeOrder,
                translate = { viewModel.translate(it) },
                onDismiss = { viewModel.setTrackingOrderId(null) }
            )
        } else if (orders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(100.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DirectionsRun,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = viewModel.translate("No active orders"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = viewModel.translate("Place an order to see real-time delivery tracking!"),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            // Orders History List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Text(
                        text = viewModel.translate("History & Previous Orders"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
                items(orders) { order ->
                    OrderHistoryItem(
                        order = order,
                        translate = { viewModel.translate(it) },
                        onTrack = { viewModel.setTrackingOrderId(order.orderId) }
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveOrderTrackingCard(
    order: Order,
    translate: (String) -> String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val progressAnim by animateFloatAsState(
        targetValue = order.progress,
        animationSpec = spring(stiffness = Spring.StiffnessVeryLow),
        label = "progress"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Tracker status header - Professional Polish Peach Banner
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = GourmetPeachLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val statusDisplay = when (order.status) {
                                "PENDING" -> "Approval Pending"
                                "PREPARING" -> "Fermenting & Preparing"
                                "DISPATCHED" -> "Dispatched Securely"
                                "OUT_FOR_DELIVERY" -> "Out for delivery"
                                "DELIVERED" -> "Delivered Fresh"
                                else -> order.status
                            }
                            Text(
                                text = translate(statusDisplay),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = GourmetTextDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (order.etaMinutes > 0) translate("Arriving in ${order.etaMinutes} mins") else translate("Handed over safely"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = GourmetSubtextLight
                            )
                        }
                        
                        // Delivery Dining Icon in a beautiful white rounded card
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.White, RoundedCornerShape(16.dp))
                                .shadow(2.dp, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (order.status == "DELIVERED") Icons.Rounded.CheckCircle else Icons.Rounded.DirectionsRun,
                                contentDescription = "Delivery status",
                                tint = GourmetPrimaryLight,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Embedded Gourmet Order Progress Stepper Component
                    Spacer(modifier = Modifier.height(16.dp))
                    GourmetOrderStepper(progress = progressAnim, status = order.status)
                }
            }
        }

        // Live Simulated Tracker Map (Canvas)
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .border(
                        1.dp,
                        GourmetClayLight.copy(alpha = 0.5f),
                        RoundedCornerShape(24.dp)
                    )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    LiveCourierMapCanvas(progress = progressAnim)

                    // Overlay information
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .background(GourmetTextDark.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = translate("LIVE SATELLITE RADAR"),
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        // Delivery Partner Contact Card
        item {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = GourmetTanLight),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(GourmetClayLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = "Partner Avatar",
                            tint = GourmetTextDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            translate("DELIVERY PARTNER"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GourmetSubtextLight,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = order.deliveryPartnerName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = GourmetTextDark
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                val u = Uri.parse("tel:${order.deliveryPartnerPhone}")
                                val intent = Intent(Intent.ACTION_DIAL, u)
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White, CircleShape)
                        ) {
                            Icon(Icons.Rounded.Phone, "Call", modifier = Modifier.size(18.dp), tint = GourmetPrimaryLight)
                        }
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("smsto:${order.deliveryPartnerPhone}")
                                    putExtra("sms_body", "Hi, checking on my fresh batter delivery status.")
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White, CircleShape)
                        ) {
                            Icon(Icons.Rounded.Message, "Sms", modifier = Modifier.size(18.dp), tint = GourmetPrimaryLight)
                        }
                    }
                }
            }
        }

        // Order Summary Card - Styled perfectly matching the HTML
        item {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GourmetClayLight.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = translate("Order Summary"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = GourmetTextDark
                        )
                        // Paid Securely Badge
                        Box(
                            modifier = Modifier
                                .background(GourmetGreenBg, RoundedCornerShape(100.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                translate("PAID SECURELY"),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = GourmetGreenText,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Parsed Order Items List
                    val parsedItems = remember(order.itemsSummary) {
                        order.itemsSummary.split(", ").mapNotNull { itemStr ->
                            if (itemStr.contains(" x ")) {
                                val parts = itemStr.split(" x ")
                                val qty = parts.getOrNull(0) ?: "1"
                                val name = parts.getOrNull(1) ?: itemStr
                                Pair(qty, name)
                            } else {
                                Pair("1", itemStr)
                            }
                        }
                    }

                    parsedItems.forEach { (qty, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(GourmetSlightGray, RoundedCornerShape(12.dp))
                                    .border(1.dp, GourmetClayLight.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (name.contains("Chutney") || name.contains("Podis") || name.contains("Sambar")) Icons.Rounded.Coffee else Icons.Rounded.Restaurant,
                                    contentDescription = "Item Icon",
                                    tint = GourmetPrimaryLight,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = translate(name),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GourmetTextDark
                                )
                                Text(
                                    text = "$qty ${if (qty == "1") translate("each_label") else translate("Packs")}",
                                    fontSize = 11.sp,
                                    color = GourmetSubtextLight
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = GourmetClayLight.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = translate("Delivery Fee"),
                            fontSize = 13.sp,
                            color = GourmetSubtextLight
                        )
                        Text(
                            text = translate("FREE"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GourmetGreenText
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = translate("Total Amount"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = GourmetTextDark
                        )
                        Text(
                            text = "₹${String.format(Locale.US, "%.2f", order.totalAmount)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GourmetPrimaryLight
                        )
                    }
                }
            }
        }

        // Status Logs List
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                translate("Delivery History Logs"),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DeliveryStepItem(
                    title = translate("Order Delivered"),
                    subtitle = translate("Package handed to customer. Enjoy your fresh stone-ground batter!"),
                    isCompleted = order.progress >= 1.0f,
                    isCurrent = order.status == "DELIVERED"
                )
                DeliveryStepItem(
                    title = translate("Out for Delivery"),
                    subtitle = translate("Partner is cruising on eco-scooter. Warm fluffy batter is nearly there!"),
                    isCompleted = order.progress >= 0.85f,
                    isCurrent = order.status == "OUT_FOR_DELIVERY"
                )
                DeliveryStepItem(
                    title = translate("Dispatched from V&N Store"),
                    subtitle = translate("Packed secure in cold-shield bags. Fresh fermentation locked."),
                    isCompleted = order.progress >= 0.55f,
                    isCurrent = order.status == "DISPATCHED"
                )
                DeliveryStepItem(
                    title = translate("Fermenting & Preparing"),
                    subtitle = translate("Our culinary team is selecting and sealing the prime active batter."),
                    isCompleted = order.progress >= 0.25f,
                    isCurrent = order.status == "PREPARING"
                )
                DeliveryStepItem(
                    title = translate("Secure Checkout Approved"),
                    subtitle = translate("Encrypted digital handshake successful. Cooking order initiated."),
                    isCompleted = order.progress >= 0.05f,
                    isCurrent = order.status == "PENDING"
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GourmetPrimaryLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(translate("Back to Order History"), fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun LiveCourierMapCanvas(progress: Float) {
    // Dark mode compatible aesthetic
    val strokeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    val activePathColor = MaterialTheme.colorScheme.primary
    val isDark = isSystemInDarkTheme()

    val mapBgColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF9F9FB)
    val mapLinesColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Draw Map Grid Background (Aesthetic city lines)
        drawRect(color = mapBgColor)

        // Draw horizontal/vertical city streets
        val streetSpacing = 50.dp.toPx()
        var tempX = 0f
        while (tempX < w) {
            drawLine(mapLinesColor, Offset(tempX, 0f), Offset(tempX, h), strokeWidth = 2.dp.toPx())
            tempX += streetSpacing
        }
        var tempY = 0f
        while (tempY < h) {
            drawLine(mapLinesColor, Offset(0f, tempY), Offset(w, tempY), strokeWidth = 2.dp.toPx())
            tempY += streetSpacing
        }

        // Define Start and End Points on Map
        val startPoint = Offset(w * 0.15f, h * 0.75f) // V&N Hub
        val midPoint = Offset(w * 0.5f, h * 0.25f) // Intersection
        val endPoint = Offset(w * 0.8f, h * 0.65f) // User home

        // Draw dashed road from Kitchen to User
        val totalPath = Path().apply {
            moveTo(startPoint.x, startPoint.y)
            lineTo(midPoint.x, midPoint.y)
            lineTo(endPoint.x, endPoint.y)
        }

        drawPath(
            path = totalPath,
            color = strokeColor,
            style = Stroke(
                width = 4.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
            )
        )

        // Draw completed segment in solid color
        val currentPt = when {
            progress <= 0.5f -> {
                val ratio = progress / 0.5f
                Offset(
                    startPoint.x + (midPoint.x - startPoint.x) * ratio,
                    startPoint.y + (midPoint.y - startPoint.y) * ratio
                )
            }
            else -> {
                val ratio = (progress - 0.5f) / 0.5f
                Offset(
                    midPoint.x + (endPoint.x - midPoint.x) * ratio,
                    midPoint.y + (endPoint.y - midPoint.y) * ratio
                )
            }
        }

        val completedPath = Path().apply {
            moveTo(startPoint.x, startPoint.y)
            if (progress <= 0.5f) {
                lineTo(currentPt.x, currentPt.y)
            } else {
                lineTo(midPoint.x, midPoint.y)
                lineTo(currentPt.x, currentPt.y)
            }
        }

        drawPath(
            path = completedPath,
            color = activePathColor,
            style = Stroke(width = 4.dp.toPx())
        )

        // Draw V&N hub circle
        drawCircle(
            color = Color(0xFF5856D6),
            radius = 8.dp.toPx(),
            center = startPoint
        )
        drawCircle(
            color = Color.White,
            radius = 3.dp.toPx(),
            center = startPoint
        )

        // Draw User Home circle
        drawCircle(
            color = Color(0xFF34C759),
            radius = 8.dp.toPx(),
            center = endPoint
        )
        drawCircle(
            color = Color.White,
            radius = 3.dp.toPx(),
            center = endPoint
        )

        // Draw Moving Scooter Indicator
        drawCircle(
            color = activePathColor,
            radius = 12.dp.toPx(),
            center = currentPt
        )
        drawCircle(
            color = Color.White,
            radius = 6.dp.toPx(),
            center = currentPt
        )
    }
}

@Composable
fun DeliveryStepItem(
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    isCurrent: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(18.dp)
                    .background(
                        color = if (isCompleted) Color(0xFF34C759) else Color.Gray.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
            ) {
                if (isCompleted) {
                    Icon(Icons.Default.Check, "Checked", tint = Color.White, modifier = Modifier.size(10.dp))
                }
            }
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(30.dp)
                    .background(
                        color = if (isCompleted) Color(0xFF34C759) else Color.Gray.copy(alpha = 0.2f)
                    )
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun OrderHistoryItem(
    order: Order,
    translate: (String) -> String,
    onTrack: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val dateStr = remember(order.timestamp) { formatter.format(Date(order.timestamp)) }

    val translatedSummary = remember(order.itemsSummary) {
        order.itemsSummary.split(", ").map { itemStr ->
            if (itemStr.contains(" x ")) {
                val parts = itemStr.split(" x ")
                val qty = parts.getOrNull(0) ?: "1"
                val name = parts.getOrNull(1) ?: itemStr
                "$qty x ${translate(name)}"
            } else {
                translate(itemStr)
            }
        }.joinToString(", ")
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = translate("ID: ${order.orderId}"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            if (order.status == "DELIVERED") Color(0xFF34C759).copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            RoundedCornerShape(100.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    val statusDisplay = when (order.status) {
                        "PENDING" -> "Approval Pending"
                        "PREPARING" -> "Fermenting & Preparing"
                        "DISPATCHED" -> "Dispatched Securely"
                        "OUT_FOR_DELIVERY" -> "Out for delivery"
                        "DELIVERED" -> "Delivered Fresh"
                        else -> order.status
                    }
                    Text(
                        text = translate(statusDisplay),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (order.status == "DELIVERED") Color(0xFF34C759) else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = translatedSummary,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = translate("Total Paid") + ": ₹${String.format(Locale.US, "%.2f", order.totalAmount)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Button(
                    onClick = onTrack,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(
                        text = if (order.status == "DELIVERED") translate("View Summary") else translate("Track Live"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ---------------- PROFILE / INFORMATION TAB ----------------

@Composable
fun ProfileTabScreen(currentLanguage: String, translate: (String) -> String) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .testTag("profile_screen")
    ) {
        // Top branding Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 32.dp, horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .shadow(4.dp, RoundedCornerShape(22.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_vn_logo_1784440485106),
                        contentDescription = "V&N Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = translate("V&N Foods"),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = translate("Version 1.0.0 (Secure Build)"),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info details cards
        Text(
            text = translate("Fermentation Craftsmanship"),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = translate("V&N Natural Stone-Ground Method"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = translate("Unlike fast commercial mills that heat the grains and destroy crucial gut-friendly microbes, our stone-grinders spin slowly at ambient temperature. We allow our premium organic black gram and polished short-grain rice to ferment naturally in controlled pristine chambers for exactly 14 hours. No added yeast, no soda, 100% natural."),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = translate("App Features"),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileFeatureRow(
                    icon = Icons.Rounded.VerifiedUser,
                    title = translate("Secure Encrypted Pay"),
                    description = translate("Card data is formatted locally and never stored. Processed through cryptographic hashing.")
                )
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ProfileFeatureRow(
                    icon = Icons.Rounded.DirectionsRun,
                    title = translate("Database Tracking Engine"),
                    description = translate("Card data formatted locally, delivery progress is stored securely in Room DB.")
                )
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ProfileFeatureRow(
                    icon = Icons.Rounded.Eco,
                    title = translate("Cold-Shield Logistics"),
                    description = translate("All products dispatched in specialized temperature containers maintaining exactly 4°C.")
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ProfileFeatureRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun CartDrawer(
    viewModel: MainViewModel,
    currentLanguage: String,
    onDismiss: () -> Unit
) {
    val cartDisplayList by viewModel.cartDisplayItems.collectAsState()
    val summary by viewModel.cartSummary.collectAsState()

    Card(
        shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.85f)
            .widthIn(max = 360.dp)
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
            )
            .testTag("cart_drawer")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 20.dp, horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.ShoppingCart,
                        contentDescription = "Cart Drawer",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cart Drawer",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (cartDisplayList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.ShoppingCart,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Your drawer is empty",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(cartDisplayList) { displayItem ->
                        DrawerCartItemCard(
                            displayItem = displayItem,
                            onWeightChanged = { newWeightId ->
                                viewModel.updateCartItemWeight(displayItem.item.id, newWeightId)
                            },
                            onQtyAdd = { viewModel.addToCart(displayItem.item.id, 1) },
                            onQtySubtract = { viewModel.addToCart(displayItem.item.id, -1) },
                            onRemove = { viewModel.removeFromCart(displayItem.item.id) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(16.dp))

                // Pricing Summary
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text("₹${String.format(Locale.US, "%.2f", summary.subtotal)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tax (5% GST)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text("₹${String.format(Locale.US, "%.2f", summary.tax)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Delivery", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text(
                            text = if (summary.deliveryFee == 0.0) "FREE" else "₹${String.format(Locale.US, "%.2f", summary.deliveryFee)}",
                            fontSize = 13.sp,
                            color = if (summary.deliveryFee == 0.0) Color(0xFF34C759) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Amount", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = "₹${String.format(Locale.US, "%.2f", summary.total)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        onDismiss()
                        viewModel.navigateTo(Screen.Cart)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("drawer_checkout_button")
                ) {
                    Text("Proceed to Checkout", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun DrawerCartItemCard(
    displayItem: CartDisplayItem,
    onWeightChanged: (String) -> Unit,
    onQtyAdd: () -> Unit,
    onQtySubtract: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                RoundedCornerShape(14.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dasa Idly Batter",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "₹${String.format(Locale.US, "%.2f", displayItem.item.price)} each",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Remove",
                        tint = Color(0xFFFF3B30),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Weight Segment selector: "0.5 kg", "1 kg", "2 kg"
            Text(
                text = "Chosen Weight:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "0.5 kg" to "b1",
                    "1 kg" to "b2",
                    "2 kg" to "b3"
                ).forEach { (weightLabel, weightId) ->
                    val isSelected = displayItem.item.id == weightId
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            )
                            .clickable { onWeightChanged(weightId) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = weightLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quantity selector row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subtotal: ₹${String.format(Locale.US, "%.2f", displayItem.item.price * displayItem.quantity)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(100.dp))
                        .padding(2.dp)
                ) {
                    IconButton(
                        onClick = onQtySubtract,
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        Icon(Icons.Rounded.Remove, "Less", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        text = displayItem.quantity.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(
                        onClick = onQtyAdd,
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        Icon(Icons.Rounded.Add, "More", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun GourmetOrderStepper(
    progress: Float,
    status: String
) {
    val stages = listOf(
        Triple("Received", Icons.Rounded.ReceiptLong, "Order Received"),
        Triple("Preparing", Icons.Rounded.Kitchen, "Preparing Batter"),
        Triple("Dispatched", Icons.Rounded.LocalShipping, "Dispatched"),
        Triple("On the Way", Icons.Rounded.DirectionsBike, "Out for Delivery")
    )

    // Determine active index based on status
    val activeIndex = when (status) {
        "PENDING" -> 0
        "PREPARING" -> 1
        "DISPATCHED" -> 2
        "OUT_FOR_DELIVERY" -> 3
        "DELIVERED" -> 4
        else -> 0
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GourmetClayLight.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .testTag("order_stepper_component")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "DELIVERY TIMELINE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GourmetSubtextLight,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // 1. Background progress track (gray line)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(GourmetSlightGray, RoundedCornerShape(2.dp))
                )

                // 2. Animated active progress fill track
                // Normalize progress line to active index
                val fillFraction = when {
                    activeIndex >= 3 -> 1.0f
                    activeIndex == 0 -> 0.05f
                    activeIndex == 1 -> 0.33f
                    activeIndex == 2 -> 0.66f
                    else -> 0.0f
                }
                val animatedFillFraction by animateFloatAsState(
                    targetValue = fillFraction,
                    animationSpec = spring(stiffness = Spring.StiffnessVeryLow),
                    label = "stepper_line_fill"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFillFraction)
                        .height(4.dp)
                        .background(GourmetPrimaryLight, RoundedCornerShape(2.dp))
                )

                // 3. Stage Nodes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    stages.forEachIndexed { index, (shortLabel, icon, fullName) ->
                        val isCompleted = index < activeIndex
                        val isActive = index == activeIndex
                        
                        val nodeBg = when {
                            isCompleted -> GourmetGreenBg
                            isActive -> GourmetPeachLight
                            else -> GourmetSlightGray
                        }
                        
                        val nodeBorderColor = when {
                            isCompleted -> GourmetGreenText
                            isActive -> GourmetPrimaryLight
                            else -> GourmetClayLight.copy(alpha = 0.5f)
                        }
                        
                        val iconTint = when {
                            isCompleted -> GourmetGreenText
                            isActive -> GourmetPrimaryLight
                            else -> GourmetSubtextLight.copy(alpha = 0.4f)
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(nodeBg)
                                .border(2.dp, nodeBorderColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Completed",
                                    tint = GourmetGreenText,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = fullName,
                                    tint = iconTint,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Labels row below nodes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                stages.forEachIndexed { index, (_, _, fullName) ->
                    val isActive = index == activeIndex
                    val isCompleted = index < activeIndex
                    
                    val textColor = when {
                        isActive -> GourmetPrimaryLight
                        isCompleted -> GourmetTextDark
                        else -> GourmetSubtextLight.copy(alpha = 0.4f)
                    }
                    
                    val fontWeight = when {
                        isActive -> FontWeight.ExtraBold
                        isCompleted -> FontWeight.SemiBold
                        else -> FontWeight.Normal
                    }

                    Text(
                        text = fullName,
                        fontSize = 10.sp,
                        fontWeight = fontWeight,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 12.sp,
                        modifier = Modifier.width(68.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleReviewsDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val reviews by viewModel.allReviews.collectAsState()
    val context = LocalContext.current

    // Form fields
    var reviewerName by remember { mutableStateOf("") }
    var reviewText by remember { mutableStateOf("") }
    var reviewRating by remember { mutableStateOf(5) }
    var showForm by remember { mutableStateOf(false) }
    var submissionSuccess by remember { mutableStateOf(false) }

    // Calculate rating details
    val averageRating = if (reviews.isEmpty()) 0.0 else reviews.map { it.rating }.average()
    val formattedAverage = String.format(Locale.US, "%.1f", averageRating)

    // Proportions for stars 1 to 5
    val totalReviews = reviews.size.coerceAtLeast(1)
    val starCounts = IntArray(6) // index 1 to 5
    for (r in reviews) {
        if (r.rating in 1..5) {
            starCounts[r.rating]++
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .border(1.dp, GourmetClayLight.copy(alpha = 0.4f), RoundedCornerShape(28.dp))
                .testTag("apple_reviews_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = viewModel.translate("Customer Reviews"),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GourmetTextDark
                        )
                        Text(
                            text = "Dosa Idly Batter",
                            fontSize = 13.sp,
                            color = GourmetSubtextLight,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(GourmetSlightGray, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = GourmetTextDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable container for Reviews & Form
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Summary section
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = GourmetTanLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left side average rating
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Text(
                                        text = formattedAverage,
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.Black,
                                        color = GourmetTextDark,
                                        lineHeight = 44.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "out of 5",
                                        fontSize = 11.sp,
                                        color = GourmetSubtextLight,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row {
                                        repeat(5) { index ->
                                            val active = index < Math.round(averageRating).toInt()
                                            Icon(
                                                imageVector = Icons.Rounded.Star,
                                                contentDescription = null,
                                                tint = if (active) Color(0xFFFFCC00) else GourmetClayLight.copy(alpha = 0.5f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${reviews.size} Ratings",
                                        fontSize = 11.sp,
                                        color = GourmetSubtextLight
                                    )
                                }

                                // Vertical Divider
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(70.dp)
                                        .background(GourmetClayLight.copy(alpha = 0.3f))
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                // Right side progress bars
                                Column(modifier = Modifier.weight(2f)) {
                                    (5 downTo 1).forEach { starIndex ->
                                        val count = starCounts[starIndex]
                                        val fraction = count.toFloat() / totalReviews.toFloat()

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = starIndex.toString(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GourmetSubtextLight,
                                                modifier = Modifier.width(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(GourmetClayLight.copy(alpha = 0.2f))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(fraction)
                                                        .height(4.dp)
                                                        .background(GourmetPrimaryLight, RoundedCornerShape(2.dp))
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Write Review section
                    item {
                        AnimatedVisibility(
                            visible = !showForm && !submissionSuccess,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Button(
                                onClick = { showForm = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GourmetPeachLight),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("write_review_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.RateReview,
                                    contentDescription = null,
                                    tint = GourmetPrimaryLight,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = viewModel.translate("Write a Customer Review"),
                                    fontWeight = FontWeight.Bold,
                                    color = GourmetPrimaryLight,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = submissionSuccess,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = GourmetGreenBg),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Success",
                                        tint = GourmetGreenText,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = viewModel.translate("Review Submitted!"),
                                            fontWeight = FontWeight.Bold,
                                            color = GourmetGreenText,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = viewModel.translate("Thank you for sharing your feedback with us."),
                                            fontSize = 12.sp,
                                            color = GourmetGreenText.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = showForm,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = GourmetSlightGray),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, GourmetClayLight.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                                    .testTag("write_review_form_card")
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = viewModel.translate("Write Review"),
                                            fontWeight = FontWeight.Bold,
                                            color = GourmetTextDark,
                                            fontSize = 15.sp
                                        )
                                        IconButton(
                                            onClick = { showForm = false },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Cancel",
                                                tint = GourmetSubtextLight,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Name Field
                                    OutlinedTextField(
                                        value = reviewerName,
                                        onValueChange = { reviewerName = it },
                                        placeholder = { Text("Your Name", fontSize = 13.sp) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White,
                                            focusedBorderColor = GourmetPrimaryLight,
                                            unfocusedBorderColor = GourmetClayLight.copy(alpha = 0.5f),
                                            focusedTextColor = GourmetTextDark,
                                            unfocusedTextColor = GourmetTextDark
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("reviewer_name_input")
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Star Selector
                                    Column {
                                        Text(
                                            text = "Select Rating",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GourmetSubtextLight
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            repeat(5) { index ->
                                                val starRatingValue = index + 1
                                                val isSelected = starRatingValue <= reviewRating

                                                val starScale by animateFloatAsState(
                                                    targetValue = if (isSelected) 1.2f else 1.0f,
                                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                                    label = "star_scale"
                                                )

                                                Icon(
                                                    imageVector = Icons.Rounded.Star,
                                                    contentDescription = "$starRatingValue Star",
                                                    tint = if (isSelected) Color(0xFFFFCC00) else GourmetClayLight.copy(alpha = 0.5f),
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .scale(starScale)
                                                        .clickable { reviewRating = starRatingValue }
                                                        .testTag("star_rating_$starRatingValue")
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Feedback Field
                                    OutlinedTextField(
                                        value = reviewText,
                                        onValueChange = { reviewText = it },
                                        placeholder = { Text("Share your experience with our fresh Dosa Idly Batter...", fontSize = 13.sp) },
                                        minLines = 3,
                                        maxLines = 5,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White,
                                            focusedBorderColor = GourmetPrimaryLight,
                                            unfocusedBorderColor = GourmetClayLight.copy(alpha = 0.5f),
                                            focusedTextColor = GourmetTextDark,
                                            unfocusedTextColor = GourmetTextDark
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("reviewer_feedback_input")
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Submit Button
                                    Button(
                                        onClick = {
                                            if (reviewerName.isNotBlank() && reviewText.isNotBlank()) {
                                                viewModel.submitReview(
                                                    customerName = reviewerName,
                                                    rating = reviewRating,
                                                    feedback = reviewText
                                                )
                                                // Show Toast indicating rating has been received successfully
                                                val toastMsg = viewModel.translate("Your rating and review have been received successfully!")
                                                Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show()

                                                reviewerName = ""
                                                reviewText = ""
                                                reviewRating = 5
                                                showForm = false
                                                submissionSuccess = true
                                            }
                                        },
                                        enabled = reviewerName.isNotBlank() && reviewText.isNotBlank(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = GourmetPrimaryLight,
                                            disabledContainerColor = GourmetClayLight.copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("submit_review_form_button")
                                    ) {
                                        Text(
                                            text = viewModel.translate("Post Review"),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Divider title
                    item {
                        Text(
                            text = viewModel.translate("Customer Feedback"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = GourmetTextDark,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // If empty state
                    if (reviews.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No reviews yet. Be the first to share your thoughts!",
                                    fontSize = 13.sp,
                                    color = GourmetSubtextLight,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // Display reviews list
                        items(reviews) { review ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = GourmetSlightGray),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, GourmetClayLight.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                    .testTag("review_card_item")
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = review.customerName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = GourmetTextDark
                                        )
                                        Text(
                                            text = formatReviewDate(review.timestamp),
                                            fontSize = 10.sp,
                                            color = GourmetSubtextLight
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Display stars for this review
                                    Row {
                                        repeat(5) { starIdx ->
                                            Icon(
                                                imageVector = Icons.Rounded.Star,
                                                contentDescription = null,
                                                tint = if (starIdx < review.rating) Color(0xFFFFCC00) else GourmetClayLight.copy(alpha = 0.4f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = review.feedback,
                                        fontSize = 12.sp,
                                        color = GourmetTextDark.copy(alpha = 0.9f),
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatReviewDate(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 0 -> "$days days ago"
        hours > 0 -> "$hours hours ago"
        minutes > 0 -> "$minutes mins ago"
        else -> "Just now"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleTranslateTopBar(
    viewModel: MainViewModel,
    currentLanguage: String,
    onTranslateClick: () -> Unit
) {
    val translatedTitle = viewModel.translate("V&N Foods")
    val activeLang = AppTranslation.LANGUAGES.find { it.code == currentLanguage } ?: AppTranslation.LANGUAGES.first()

    CenterAlignedTopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Restaurant,
                    contentDescription = null,
                    tint = GourmetPrimaryLight,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = translatedTitle,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = GourmetTextDark,
                    letterSpacing = 0.5.sp
                )
            }
        },
        actions = {
            Surface(
                onClick = onTranslateClick,
                color = GourmetPeachLight.copy(alpha = 0.8f),
                shape = RoundedCornerShape(100.dp),
                border = BorderStroke(1.dp, GourmetPrimaryLight.copy(alpha = 0.25f)),
                modifier = Modifier
                    .padding(end = 12.dp)
                    .testTag("google_translate_trigger")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Translate,
                        contentDescription = "Translate icon",
                        tint = GourmetPrimaryLight,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${activeLang.flag} ${activeLang.name}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GourmetPrimaryLight
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Rounded.ArrowDropDown,
                        contentDescription = null,
                        tint = GourmetPrimaryLight,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = GourmetSurfaceLight,
            titleContentColor = GourmetTextDark
        ),
        modifier = Modifier.shadow(2.dp)
    )
}

@Composable
fun GoogleTranslateDialog(
    viewModel: MainViewModel,
    currentLanguage: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.5.dp, GourmetPrimaryLight.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                .testTag("google_translate_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF4285F4), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Translate,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = viewModel.translate("Google Translate"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF4285F4)
                        )
                        Text(
                            text = viewModel.translate("Powered by Google Cloud Translation"),
                            fontSize = 10.sp,
                            color = GourmetSubtextLight.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = GourmetClayLight.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = viewModel.translate("Translate application to:"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GourmetTextDark,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppTranslation.LANGUAGES.forEach { lang ->
                        val isSelected = lang.code == currentLanguage
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) GourmetPeachLight.copy(alpha = 0.5f) else Color.Transparent)
                                .clickable {
                                    viewModel.setLanguage(lang.code)
                                    onDismiss()
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = lang.flag,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = lang.name,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) GourmetPrimaryLight else GourmetTextDark
                                )
                                if (lang.code == "en") {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(${viewModel.translate("English (Original)")})",
                                        fontSize = 11.sp,
                                        color = GourmetSubtextLight.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = GourmetPrimaryLight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = GourmetPrimaryLight)
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

