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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.ui.theme.*
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

    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            AppleBottomNavigation(
                currentScreen = currentScreen,
                onTabSelected = { viewModel.navigateTo(it) }
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
                    is Screen.Shop -> ShopTabScreen(viewModel)
                    is Screen.Cart -> CartTabScreen(viewModel)
                    is Screen.Orders -> OrdersTabScreen(viewModel, activeTrackOrderId)
                    is Screen.Profile -> ProfileTabScreen()
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
                    onDismiss = { viewModel.setCartDrawerOpen(false) }
                )
            }

            // Apple Pay Bottom Sheet
            if (showApplePay) {
                ApplePayBottomSheet(
                    viewModel = viewModel,
                    isProcessing = isProcessingPayment,
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
    onTabSelected: (Screen) -> Unit
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
                label = "Shop",
                iconSelected = Icons.Rounded.Storefront,
                iconUnselected = Icons.Outlined.Storefront,
                isSelected = currentScreen is Screen.Shop,
                onClick = { onTabSelected(Screen.Shop) },
                tag = "tab_shop"
            )
            BottomTabItem(
                label = "Cart",
                iconSelected = Icons.Rounded.ShoppingCart,
                iconUnselected = Icons.Outlined.ShoppingCart,
                isSelected = currentScreen is Screen.Cart,
                onClick = { onTabSelected(Screen.Cart) },
                tag = "tab_cart"
            )
            BottomTabItem(
                label = "Orders",
                iconSelected = Icons.Rounded.DirectionsRun,
                iconUnselected = Icons.Outlined.DirectionsRun,
                isSelected = currentScreen is Screen.Orders,
                onClick = { onTabSelected(Screen.Orders) },
                tag = "tab_orders"
            )
            BottomTabItem(
                label = "About",
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
fun ShopTabScreen(viewModel: MainViewModel) {
    val items by viewModel.allBatterItems.collectAsState()
    val cart by viewModel.cartItems.collectAsState()
    var selectedCategory by remember { mutableStateOf("All") }

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
                    contentDescription = "V&N Foods Hero",
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
                            text = "ESTD 2026",
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "V&N Foods",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Premium stone-ground natural batters & sides",
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
                text = "Select Category",
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
                            text = category,
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
                text = "Our Fresh Batters & Sides",
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
                        text = "No items available in this category.",
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
                    onSubtract = { viewModel.addToCart(batterItem.id, -1) }
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
    onSubtract: () -> Unit
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
                                    "BESTSELLER",
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
                        text = item.name,
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
                text = item.description,
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
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = "Rating",
                        tint = Color(0xFFFFCC00),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.rating.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Rounded.AccessTime,
                        contentDescription = "Time",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.prepTime,
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
                        Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ---------------- CART TAB ----------------

@Composable
fun CartTabScreen(viewModel: MainViewModel) {
    val cartDisplayList by viewModel.cartDisplayItems.collectAsState()
    val summary by viewModel.cartSummary.collectAsState()

    var showCardPayment by remember { mutableStateOf(false) }

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
                text = "Shopping Cart",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            if (cartDisplayList.isNotEmpty()) {
                Text(
                    text = "Clear Cart",
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
                        text = "Your cart is empty",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Browse our shop and pick fresh batters!",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.navigateTo(Screen.Shop) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Start Shopping", fontWeight = FontWeight.Bold)
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
                        onRemove = { viewModel.removeFromCart(displayItem.item.id) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    OrderSummaryCard(summary = summary)
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
                            "Secured and processed via V&N Encrypted Gateway",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF34C759)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    PaymentSelectionToggle(
                        showCardPayment = showCardPayment,
                        onToggle = { showCardPayment = it }
                    )
                }

                if (showCardPayment) {
                    item {
                        CardPaymentSection(viewModel = viewModel, summary = summary)
                    }
                } else {
                    item {
                        ApplePayButton(onClick = { viewModel.setApplePaySheetVisible(true) })
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
    onRemove: () -> Unit
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
                    text = displayItem.item.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayItem.item.size,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "₹${String.format(Locale.US, "%.2f", displayItem.item.price)} each",
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
fun OrderSummaryCard(summary: CartSummary) {
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
                text = "Order Summary",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Subtotal", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Text("₹${String.format(Locale.US, "%.2f", summary.subtotal)}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tax (5%)", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Text("₹${String.format(Locale.US, "%.2f", summary.tax)}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Delivery Fee", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Text(
                    text = if (summary.deliveryFee == 0.0) "FREE" else "₹${String.format(Locale.US, "%.2f", summary.deliveryFee)}",
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
                        "Add ₹${String.format(Locale.US, "%.2f", 15.0 - summary.subtotal)} more to qualify for FREE delivery!",
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
                Text("Total Amount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
    showCardPayment: Boolean,
    onToggle: (Boolean) -> Unit
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
                .background(if (!showCardPayment) MaterialTheme.colorScheme.primary else Color.Transparent)
                .clickable { onToggle(false) }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Fingerprint,
                    contentDescription = "Apple",
                    tint = if (!showCardPayment) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Apple Pay",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (!showCardPayment) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (showCardPayment) MaterialTheme.colorScheme.primary else Color.Transparent)
                .clickable { onToggle(true) }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.CreditCard,
                    contentDescription = "Card",
                    tint = if (showCardPayment) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Credit Card",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (showCardPayment) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun ApplePayButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Fingerprint,
                contentDescription = "Pay",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Pay with Apple Pay",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun CardPaymentSection(
    viewModel: MainViewModel,
    summary: CartSummary
) {
    val cardNumber by viewModel.cardNumber.collectAsState()
    val cardExpiry by viewModel.cardExpiry.collectAsState()
    val cardCvv by viewModel.cardCvv.collectAsState()
    val cardHolder by viewModel.cardHolder.collectAsState()
    val isProcessing by viewModel.isProcessingPayment.collectAsState()

    val isFormValid = cardNumber.length >= 16 && cardExpiry.length >= 4 && cardCvv.length >= 3 && cardHolder.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Virtual Apple Card Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFE5E5EA), Color(0xFFAEAEB2)),
                        start = Offset(0f, 0f),
                        end = Offset(400f, 400f)
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "V&N SecurePay Card",
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Icon(
                        imageVector = Icons.Rounded.VerifiedUser,
                        contentDescription = "Secure",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = if (cardNumber.isBlank()) "•••• •••• •••• ••••" else formatCardNumber(cardNumber),
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black.copy(alpha = 0.85f),
                    letterSpacing = 2.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("CARD HOLDER", fontSize = 8.sp, color = Color.Black.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                        Text(
                            text = if (cardHolder.isBlank()) "YOUR NAME" else cardHolder.uppercase(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("EXPIRES", fontSize = 8.sp, color = Color.Black.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                        Text(
                            text = if (cardExpiry.isBlank()) "MM/YY" else formatExpiry(cardExpiry),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card Fields
        OutlinedTextField(
            value = cardHolder,
            onValueChange = { if (it.length <= 30) viewModel.cardHolder.value = it },
            label = { Text("Cardholder Name") },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            ),
            singleLine = true,
            enabled = !isProcessing
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = cardNumber,
            onValueChange = { input ->
                val clean = input.filter { it.isDigit() }
                if (clean.length <= 16) viewModel.cardNumber.value = clean
            },
            label = { Text("Card Number") },
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            ),
            singleLine = true,
            enabled = !isProcessing
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = cardExpiry,
                onValueChange = { input ->
                    val clean = input.filter { it.isDigit() }
                    if (clean.length <= 4) viewModel.cardExpiry.value = clean
                },
                label = { Text("Expiry (MMYY)") },
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                ),
                singleLine = true,
                enabled = !isProcessing
            )

            OutlinedTextField(
                value = cardCvv,
                onValueChange = { input ->
                    val clean = input.filter { it.isDigit() }
                    if (clean.length <= 4) viewModel.cardCvv.value = clean
                },
                label = { Text("CVV") },
                shape = RoundedCornerShape(10.dp),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                ),
                singleLine = true,
                enabled = !isProcessing
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val last4 = if (cardNumber.length >= 4) cardNumber.substring(cardNumber.length - 4) else "9921"
                viewModel.checkout("V&N SecurePay", last4)
            },
            enabled = isFormValid && !isProcessing,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            if (isProcessing) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Securing Payment...", fontWeight = FontWeight.Bold)
            } else {
                Text("Securely Pay ₹${String.format(Locale.US, "%.2f", summary.total)}", fontWeight = FontWeight.Bold)
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
    activeTrackOrderId: String?
) {
    val orders by viewModel.allOrders.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("orders_screen")
    ) {
        // Toolbar
        Text(
            text = "Track Delivery",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        val activeOrder = orders.find { it.orderId == activeTrackOrderId }

        if (activeOrder != null) {
            ActiveOrderTrackingCard(order = activeOrder, onDismiss = { viewModel.setTrackingOrderId(null) })
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
                        text = "No active orders",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Place an order to see real-time delivery tracking!",
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
                        text = "History & Previous Orders",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
                items(orders) { order ->
                    OrderHistoryItem(order = order, onTrack = { viewModel.setTrackingOrderId(order.orderId) })
                }
            }
        }
    }
}

@Composable
fun ActiveOrderTrackingCard(
    order: Order,
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
                                text = statusDisplay,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = GourmetTextDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (order.etaMinutes > 0) "Arriving in ${order.etaMinutes} mins" else "Handed over safely",
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

                    Spacer(modifier = Modifier.height(20.dp))

                    // Progress timeline with custom dots matching HTML
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Background horizontal white line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(3.dp)
                                .background(Color.White, RoundedCornerShape(2.dp))
                        )

                        // Timeline dots
                        Row(
                            modifier = Modifier.fillMaxWidth(0.9f),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Dot 1 (Pending/Prep) - completed
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(GourmetPrimaryLight, CircleShape)
                                    .border(4.dp, GourmetPeachLight, CircleShape)
                            )
                            // Dot 2 (Dispatched/Out) - completed if progress > 0.5
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(
                                        if (progressAnim >= 0.5f) GourmetPrimaryLight else Color.White,
                                        CircleShape
                                    )
                                    .border(4.dp, GourmetPeachLight, CircleShape)
                            )
                            // Dot 3 (Delivered) - completed if progress >= 1.0
                            Box(
                                modifier = Modifier
                                    .size(if (progressAnim >= 1.0f) 20.dp else 16.dp)
                                    .background(
                                        if (progressAnim >= 1.0f) GourmetPrimaryLight else Color.White,
                                        CircleShape
                                    )
                                    .border(4.dp, GourmetPeachLight, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (progressAnim >= 1.0f) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(Color.White, CircleShape)
                                    )
                                }
                            }
                        }
                    }
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
                            text = "LIVE SATELLITE RADAR",
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
                            "DELIVERY PARTNER",
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
                            text = "Order Summary",
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
                                "PAID SECURELY",
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
                                    text = name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GourmetTextDark
                                )
                                Text(
                                    text = "$qty Pack${if (qty != "1") "s" else ""}",
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
                            text = "Delivery Fee",
                            fontSize = 13.sp,
                            color = GourmetSubtextLight
                        )
                        Text(
                            text = "FREE",
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
                            text = "Total Amount",
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
                "Delivery History Logs",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DeliveryStepItem(
                    title = "Order Delivered",
                    subtitle = "Package handed to customer. Enjoy your fresh stone-ground batter!",
                    isCompleted = order.progress >= 1.0f,
                    isCurrent = order.status == "DELIVERED"
                )
                DeliveryStepItem(
                    title = "Out for Delivery",
                    subtitle = "Partner is cruising on eco-scooter. Warm fluffy batter is nearly there!",
                    isCompleted = order.progress >= 0.85f,
                    isCurrent = order.status == "OUT_FOR_DELIVERY"
                )
                DeliveryStepItem(
                    title = "Dispatched from V&N Store",
                    subtitle = "Packed secure in cold-shield bags. Fresh fermentation locked.",
                    isCompleted = order.progress >= 0.55f,
                    isCurrent = order.status == "DISPATCHED"
                )
                DeliveryStepItem(
                    title = "Fermenting & Preparing",
                    subtitle = "Our culinary team is selecting and sealing the prime active batter.",
                    isCompleted = order.progress >= 0.25f,
                    isCurrent = order.status == "PREPARING"
                )
                DeliveryStepItem(
                    title = "Secure Checkout Approved",
                    subtitle = "Encrypted digital handshake successful. Cooking order initiated.",
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
                Text("Back to Order History", fontWeight = FontWeight.Bold, color = Color.White)
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
    onTrack: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val dateStr = remember(order.timestamp) { formatter.format(Date(order.timestamp)) }

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
                        text = "ID: ${order.orderId}",
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
                    Text(
                        text = order.status,
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
                text = order.itemsSummary,
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
                    text = "Total Paid: ₹${String.format(Locale.US, "%.2f", order.totalAmount)}",
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
                        text = if (order.status == "DELIVERED") "View Summary" else "Track Live",
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
fun ProfileTabScreen() {
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
                    text = "V&N Foods",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Version 1.0.0 (Secure Build)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info details cards
        Text(
            text = "Fermentation Craftsmanship",
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
                    text = "V&N Natural Stone-Ground Method",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Unlike fast commercial mills that heat the grains and destroy crucial gut-friendly microbes, our stone-grinders spin slowly at ambient temperature. We allow our premium organic black gram and polished short-grain rice to ferment naturally in controlled pristine chambers for exactly 14 hours. No added yeast, no soda, 100% natural.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "App Features",
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
                    title = "Secure Encrypted Pay",
                    description = "Card data is formatted locally and never stored. Processed through cryptographic hashing."
                )
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ProfileFeatureRow(
                    icon = Icons.Rounded.DirectionsRun,
                    title = "Database Tracking Engine",
                    description = "Real-time delivery progress calculations persisted directly in your local SQLite Room DB."
                )
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ProfileFeatureRow(
                    icon = Icons.Rounded.Eco,
                    title = "Cold-Shield Logistics",
                    description = "All products dispatched in specialized temperature containers maintaining exactly 4°C."
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
