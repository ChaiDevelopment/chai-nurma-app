@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.chai.nurma.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chai.nurma.app.data.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

// ---------- Theme ----------

val BrandPurple = Color(0xFF7C4DFF)
val BrandPurpleDark = Color(0xFF5E35B1)
val BrandPink = Color(0xFFFF6F91)
val IncomeGreen = Color(0xFF2E7D32)
val IncomeGreenBg = Color(0xFFE8F5E9)
val ExpenseRed = Color(0xFFC62828)
val ExpenseRedBg = Color(0xFFFDECEA)
val AppBackground = Color(0xFFFAF7FD)

private val ChaiNurmaColorScheme = lightColorScheme(
    primary = BrandPurple,
    onPrimary = Color.White,
    secondary = BrandPink,
    onSecondary = Color.White,
    background = AppBackground,
    surface = Color.White,
    error = ExpenseRed
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ChaiNurmaApp() }
    }
}

@Composable
fun ChaiNurmaApp() {
    var user by remember { mutableStateOf<UserDto?>(null) }
    var token by remember { mutableStateOf<String?>(null) }

    MaterialTheme(colorScheme = ChaiNurmaColorScheme) {
        if (user == null) {
            LoginScreen { response ->
                token = response.accessToken
                user = response.user
            }
        } else {
            MainScreen(
                user = user!!,
                onLogout = {
                    token = null
                    user = null
                }
            )
        }
    }
}

// ---------- Login ----------

@Composable
fun LoginScreen(onSuccess: (LoginResponse) -> Unit) {
    var username by remember { mutableStateOf("chai") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(BrandPurpleDark, BrandPurple, BrandPink))
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(BrandPurple, BrandPink))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = null, tint = Color.White)
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "Chai \u2764\ufe0f Nurma",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Financial & Couple App",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(28.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                var passwordVisible by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible)
                        androidx.compose.ui.text.input.VisualTransformation.None
                    else
                        androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        loading = true
                        error = null

                        scope.launch {
                            try {
                                onSuccess(
                                    ApiClient.api.login(
                                        LoginRequest(username.trim(), password)
                                    )
                                )
                            } catch (e: Exception) {
                                error = e.message ?: "Login gagal"
                            } finally {
                                loading = false
                            }
                        }
                    },
                    enabled = !loading && username.isNotBlank() && password.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Masuk", fontWeight = FontWeight.SemiBold)
                    }
                }

                error?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}

// ---------- Main scaffold ----------

@Composable
fun MainScreen(user: UserDto, onLogout: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Halo, ${user.displayName} \u2764\ufe0f") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Keluar", tint = Color.White)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null) },
                    label = { Text("Finance") }
                )

                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                    label = { Text("Couple \u2764\ufe0f") }
                )

                // Tab BARU: Laporan (monthly report, filter Saya/Pasangan/Bersama, chart)
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.BarChart, contentDescription = null) },
                    label = { Text("Laporan") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (tab) {
                0 -> FinanceScreen(user)
                1 -> CoupleScreen(user)
                else -> ReportScreen(user)
            }
        }
    }
}

// ---------- Finance ----------

@Composable
fun FinanceScreen(user: UserDto) {
    var accounts by remember { mutableStateOf<List<AccountDto>>(emptyList()) }
    var transactions by remember { mutableStateOf<List<TransactionDto>>(emptyList()) }
    var summary by remember { mutableStateOf<CoupleSummaryDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var typeFilter by remember { mutableStateOf<String?>(null) }
    var selectedTx by remember { mutableStateOf<TransactionDto?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) } // BARU: fitur transfer

    val scope = rememberCoroutineScope()

    // Data digabung dari kedua user (pasangan), bukan cuma milik sendiri
    fun refresh() {
        scope.launch {
            try {
                accounts = ApiClient.api.coupleAccounts(user.id)
                transactions = ApiClient.api.coupleTransactions(user.id)
                summary = ApiClient.api.coupleSummary(user.id)
                error = null
            } catch (e: Exception) {
                error = e.message ?: "Gagal mengambil data"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    val balance = summary?.totalBalance ?: accounts.sumOf { it.balance }
    val income = summary?.totalIncome ?: transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val expense = summary?.totalExpense ?: transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val money = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    val visibleTransactions = if (typeFilter == null) {
        transactions
    } else {
        transactions.filter { it.type == typeFilter }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                // Balance card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(BrandPurple, BrandPurpleDark)))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Total Saldo Berdua", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            money.format(balance),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Clickable income / expense filters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Pemasukan",
                        value = money.format(income),
                        icon = Icons.Filled.ArrowUpward,
                        accentColor = IncomeGreen,
                        accentBg = IncomeGreenBg,
                        selected = typeFilter == "INCOME",
                        onClick = {
                            typeFilter = if (typeFilter == "INCOME") null else "INCOME"
                        },
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "Pengeluaran",
                        value = money.format(expense),
                        icon = Icons.Filled.ArrowDownward,
                        accentColor = ExpenseRed,
                        accentBg = ExpenseRedBg,
                        selected = typeFilter == "EXPENSE",
                        onClick = {
                            typeFilter = if (typeFilter == "EXPENSE") null else "EXPENSE"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Rekening", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

                    // Tombol BARU: Transfer antar rekening (fitur tambahan, tidak mengganti apapun)
                    TextButton(onClick = { showTransferDialog = true }) {
                        Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Transfer")
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            items(accounts) { account ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(BrandPurple.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = BrandPurple)
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(account.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                            Text(
                                listOfNotNull(account.type, account.user?.displayName).joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(money.format(account.balance), fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            item {
                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Transaksi Terbaru", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

                    if (typeFilter != null) {
                        AssistChip(
                            onClick = { typeFilter = null },
                            label = { Text("Reset filter") },
                            trailingIcon = { Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
            }

            if (loading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (visibleTransactions.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Belum ada transaksi",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(visibleTransactions.take(30)) { tx ->
                TransactionRow(tx = tx, money = money, onClick = { selectedTx = tx })
            }

            error?.let {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Tambah transaksi")
        }
    }

    selectedTx?.let { tx ->
        TransactionDetailDialog(tx = tx, money = money, onDismiss = { selectedTx = null })
    }

    if (showAddDialog) {
        AddTransactionDialog(
            user = user,
            accounts = accounts,
            onDismiss = { showAddDialog = false },
            onCreated = {
                showAddDialog = false
                refresh()
            }
        )
    }

    // BARU: dialog transfer antar rekening (tidak menyentuh AddTransactionDialog di atas)
    if (showTransferDialog) {
        TransferDialog(
            user = user,
            accounts = accounts,
            onDismiss = { showTransferDialog = false },
            onCreated = {
                showTransferDialog = false
                refresh()
            }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    accentBg: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) accentBg else MaterialTheme.colorScheme.surface
        ),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, accentColor) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 0.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.labelMedium)
            }

            Spacer(Modifier.height(6.dp))

            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TransactionRow(tx: TransactionDto, money: NumberFormat, onClick: () -> Unit) {
    val isIncome = tx.type == "INCOME"
    val accentColor = if (isIncome) IncomeGreen else ExpenseRed
    val accentBg = if (isIncome) IncomeGreenBg else ExpenseRedBg

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isIncome) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tx.description?.takeIf { it.isNotBlank() } ?: (tx.category?.name ?: tx.type),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    listOfNotNull(tx.account?.name, tx.user?.displayName).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                (if (isIncome) "+ " else "- ") + money.format(tx.amount),
                color = accentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun TransactionDetailDialog(tx: TransactionDto, money: NumberFormat, onDismiss: () -> Unit) {
    val isIncome = tx.type == "INCOME"
    val accentColor = if (isIncome) IncomeGreen else ExpenseRed

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        },
        title = {
            Text(if (isIncome) "Detail Pemasukan" else "Detail Pengeluaran")
        },
        text = {
            Column {
                Text(
                    (if (isIncome) "+ " else "- ") + money.format(tx.amount),
                    style = MaterialTheme.typography.headlineSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(12.dp))

                DetailRow("Deskripsi", tx.description?.takeIf { it.isNotBlank() } ?: "-")
                DetailRow("Kategori", tx.category?.name ?: "-")
                DetailRow("Rekening", tx.account?.name ?: "-")
                DetailRow("Dicatat oleh", tx.user?.displayName ?: "-")
                DetailRow("Tanggal", formatDate(tx.transactionDate))
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

private fun formatDate(iso: String): String {
    return try {
        val datePart = iso.substring(0, 10).split("-")
        val timePart = if (iso.length >= 16) iso.substring(11, 16) else ""
        "${datePart[2]}/${datePart[1]}/${datePart[0]} $timePart".trim()
    } catch (e: Exception) {
        iso
    }
}

@Composable
fun AddTransactionDialog(
    user: UserDto,
    accounts: List<AccountDto>,
    onDismiss: () -> Unit,
    onCreated: () -> Unit
) {
    var type by remember { mutableStateOf("EXPENSE") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull()) }
    var accountMenuExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // BARU: pilihan kategori (opsional, tidak mengubah alur lama jika tidak dipilih)
    var categories by remember { mutableStateOf<List<CategoryDto>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<CategoryDto?>(null) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            categories = ApiClient.api.categories(user.id)
        } catch (_: Exception) {
            // Kategori opsional: jika gagal dimuat, form tetap bisa dipakai tanpa kategori
        }
    }

    val categoryOptions = categories.filter { it.transactionType == type }
    // Reset pilihan kategori jika tidak lagi cocok dengan tipe transaksi yang dipilih
    LaunchedEffect(type, categories) {
        if (selectedCategory?.transactionType != type) selectedCategory = null
    }

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = { Text("Tambah Transaksi") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "EXPENSE",
                        onClick = { type = "EXPENSE" },
                        label = { Text("Pengeluaran") },
                        leadingIcon = { Icon(Icons.Filled.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = type == "INCOME",
                        onClick = { type = "INCOME" },
                        label = { Text("Pemasukan") },
                        leadingIcon = { Icon(Icons.Filled.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                Spacer(Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = accountMenuExpanded,
                    onExpandedChange = { accountMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedAccount?.name ?: "Pilih rekening",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Rekening") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = accountMenuExpanded,
                        onDismissRequest = { accountMenuExpanded = false }
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc.name) },
                                onClick = {
                                    selectedAccount = acc
                                    accountMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // BARU: dropdown kategori (opsional). Daftar difilter sesuai tipe INCOME/EXPENSE yang dipilih.
                ExposedDropdownMenuBox(
                    expanded = categoryMenuExpanded,
                    onExpandedChange = { categoryMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "Tanpa kategori",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategori (opsional)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Tanpa kategori") },
                            onClick = {
                                selectedCategory = null
                                categoryMenuExpanded = false
                            }
                        )
                        categoryOptions.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() } },
                    label = { Text("Jumlah (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Deskripsi (opsional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !submitting && amount.isNotBlank() && selectedAccount != null,
                onClick = {
                    val acc = selectedAccount ?: return@Button
                    val amt = amount.toDoubleOrNull() ?: return@Button
                    submitting = true
                    error = null

                    scope.launch {
                        try {
                            ApiClient.api.createTransaction(
                                CreateTransactionRequest(
                                    userId = user.id,
                                    accountId = acc.id,
                                    categoryId = selectedCategory?.id,
                                    type = type,
                                    amount = amt,
                                    description = description.trim().ifBlank { null }
                                )
                            )
                            onCreated()
                        } catch (e: Exception) {
                            error = e.message ?: "Gagal menyimpan transaksi"
                        } finally {
                            submitting = false
                        }
                    }
                }
            ) {
                if (submitting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Simpan")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !submitting) { Text("Batal") }
        }
    )
}

// ---------- Couple ----------

@Composable
fun CoupleScreen(user: UserDto) {
    var messages by remember { mutableStateOf<List<MessageDto>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            try {
                messages = ApiClient.api.messages(user.id)
            } catch (_: Exception) {
                // Ignore refresh errors for now
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Chai \u2764\ufe0f Nurma", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Ruang kita", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(messages) { msg ->
                val isMe = msg.sender.id == user.id

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 260.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isMe) 16.dp else 4.dp,
                                    bottomEnd = if (isMe) 4.dp else 16.dp
                                )
                            )
                            .background(if (isMe) BrandPurple else Color(0xFFF0EAFB))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        if (!isMe) {
                            Text(
                                msg.sender.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = BrandPurpleDark
                            )
                            Spacer(Modifier.height(2.dp))
                        }

                        Text(
                            msg.message,
                            color = if (isMe) Color.White else Color(0xFF2A2A2A)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Tulis pesan...") },
                shape = RoundedCornerShape(24.dp)
            )

            Spacer(Modifier.width(8.dp))

            FilledIconButton(
                enabled = input.isNotBlank(),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = BrandPurple),
                onClick = {
                    val text = input.trim()
                    input = ""

                    scope.launch {
                        try {
                            ApiClient.api.sendMessage(SendMessageRequest(user.id, text))
                            refresh()
                        } catch (_: Exception) {
                            // Ignore send errors for now
                        }
                    }
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Kirim", tint = Color.White)
            }
        }
    }
}
