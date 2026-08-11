@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.chai.nurma.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chai.nurma.app.data.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

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

    MaterialTheme {
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

@Composable
fun LoginScreen(onSuccess: (LoginResponse) -> Unit) {
    var username by remember { mutableStateOf("chai") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Chai ❤️ Nurma",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            "Financial & Couple App",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                loading = true
                error = null

                scope.launch {
                    try {
                        onSuccess(
                            ApiClient.api.login(
                                LoginRequest(
                                    username.trim(),
                                    password
                                )
                            )
                        )
                    } catch (e: Exception) {
                        error = e.message ?: "Login gagal"
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (loading) "Login..." else "Masuk")
        }

        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                it,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun MainScreen(
    user: UserDto,
    onLogout: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Halo, ${user.displayName} ❤️")
                },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Keluar")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = {},
                    label = { Text("Finance") }
                )

                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = {},
                    label = { Text("Couple ❤️") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (tab == 0) {
                FinanceScreen(user)
            } else {
                CoupleScreen(user)
            }
        }
    }
}

@Composable
fun FinanceScreen(user: UserDto) {
    var accounts by remember {
        mutableStateOf<List<AccountDto>>(emptyList())
    }

    var transactions by remember {
        mutableStateOf<List<TransactionDto>>(emptyList())
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            try {
                accounts = ApiClient.api.accounts(user.id)
                transactions = ApiClient.api.transactions(user.id)
                error = null
            } catch (e: Exception) {
                error = e.message ?: "Gagal mengambil data"
            }
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    val balance = accounts.sumOf { it.balance }

    val income = transactions
        .filter { it.type == "INCOME" }
        .sumOf { it.amount }

    val expense = transactions
        .filter { it.type == "EXPENSE" }
        .sumOf { it.amount }

    val money = NumberFormat.getCurrencyInstance(
        Locale("id", "ID")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                "Total Saldo",
                style = MaterialTheme.typography.labelLarge
            )

            Text(
                money.format(balance),
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "Pemasukan",
                    value = money.format(income),
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Pengeluaran",
                    value = money.format(expense),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Rekening",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(Modifier.height(8.dp))
        }

        items(accounts) { account ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Text(
                        account.name,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(money.format(account.balance))
                    Text(account.type)
                }
            }
        }

        item {
            Spacer(Modifier.height(16.dp))

            Text(
                "Transaksi Terbaru",
                style = MaterialTheme.typography.titleLarge
            )
        }

        items(transactions.take(20)) { tx ->
            ListItem(
                headlineContent = {
                    Text(tx.description ?: tx.type)
                },
                supportingContent = {
                    Text(tx.account?.name ?: "")
                },
                trailingContent = {
                    Text(
                        (if (tx.type == "INCOME") "+ " else "- ") +
                            money.format(tx.amount)
                    )
                }
            )
        }

        error?.let {
            item {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(modifier) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium
            )

            Text(
                value,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun CoupleScreen(user: UserDto) {
    var messages by remember {
        mutableStateOf<List<MessageDto>>(emptyList())
    }

    var input by remember {
        mutableStateOf("")
    }

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

    LaunchedEffect(Unit) {
        refresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Chai ❤️ Nurma",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            "Ruang kita",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = false
        ) {
            items(messages) { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text(
                            msg.sender.displayName,
                            style = MaterialTheme.typography.labelMedium
                        )

                        Text(msg.message)
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("Tulis pesan...")
                }
            )

            Spacer(Modifier.width(8.dp))

            Button(
                enabled = input.isNotBlank(),
                onClick = {
                    val text = input.trim()
                    input = ""

                    scope.launch {
                        try {
                            ApiClient.api.sendMessage(
                                SendMessageRequest(
                                    user.id,
                                    text
                                )
                            )

                            refresh()
                        } catch (_: Exception) {
                            // Ignore send errors for now
                        }
                    }
                }
            ) {
                Text("Kirim")
            }
        }
    }
}