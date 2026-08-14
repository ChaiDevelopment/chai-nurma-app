@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.chai.nurma.app

// File BARU: dialog transfer antar rekening. Tidak mengubah AddTransactionDialog yang sudah ada.
// Menggunakan style/component yang sama (AlertDialog, OutlinedTextField, ExposedDropdownMenuBox)
// seperti pada MainActivity.kt.

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chai.nurma.app.data.AccountDto
import com.chai.nurma.app.data.ApiClient
import com.chai.nurma.app.data.CreateTransferRequest
import com.chai.nurma.app.data.UserDto
import kotlinx.coroutines.launch

@Composable
fun TransferDialog(
    user: UserDto,
    accounts: List<AccountDto>,
    onDismiss: () -> Unit,
    onCreated: () -> Unit
) {
    var fromAccount by remember { mutableStateOf(accounts.firstOrNull()) }
    var toAccount by remember { mutableStateOf(accounts.getOrNull(1) ?: accounts.firstOrNull()) }
    var fromMenuExpanded by remember { mutableStateOf(false) }
    var toMenuExpanded by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = { Text("Transfer Antar Rekening") },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = fromMenuExpanded,
                    onExpandedChange = { fromMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = fromAccount?.name ?: "Pilih rekening asal",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Dari") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromMenuExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = fromMenuExpanded, onDismissRequest = { fromMenuExpanded = false }) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(text = { Text(acc.name) }, onClick = {
                                fromAccount = acc
                                fromMenuExpanded = false
                            })
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = toMenuExpanded,
                    onExpandedChange = { toMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = toAccount?.name ?: "Pilih rekening tujuan",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Ke") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toMenuExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = toMenuExpanded, onDismissRequest = { toMenuExpanded = false }) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(text = { Text(acc.name) }, onClick = {
                                toAccount = acc
                                toMenuExpanded = false
                            })
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
                    label = { Text("Keterangan (opsional)") },
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
                enabled = !submitting && amount.isNotBlank() && fromAccount != null && toAccount != null && fromAccount?.id != toAccount?.id,
                onClick = {
                    val from = fromAccount ?: return@Button
                    val to = toAccount ?: return@Button
                    val amt = amount.toDoubleOrNull() ?: return@Button
                    submitting = true
                    error = null

                    scope.launch {
                        try {
                            ApiClient.api.createTransfer(
                                CreateTransferRequest(
                                    userId = user.id,
                                    fromAccountId = from.id,
                                    toAccountId = to.id,
                                    amount = amt,
                                    description = description.trim().ifBlank { null }
                                )
                            )
                            onCreated()
                        } catch (e: Exception) {
                            error = e.message ?: "Gagal melakukan transfer"
                        } finally {
                            submitting = false
                        }
                    }
                }
            ) {
                if (submitting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Transfer")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !submitting) { Text("Batal") }
        }
    )
}
