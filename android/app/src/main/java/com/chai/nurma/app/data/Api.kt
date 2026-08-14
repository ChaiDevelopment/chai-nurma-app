package com.chai.nurma.app.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class LoginRequest(val username: String, val password: String)
data class UserDto(val id: String, val username: String, val displayName: String, val role: String)
data class LoginResponse(val accessToken: String, val user: UserDto)
data class OwnerDto(val id: String, val displayName: String)
data class AccountDto(
    val id: String, val name: String, val type: String, val balance: Double, val currency: String,
    val visibility: String, val user: OwnerDto? = null
)
data class TransactionDto(
    val id: String, val type: String, val amount: Double, val description: String?,
    val transactionDate: String, val account: AccountDto?, val category: CategoryDto?,
    val user: OwnerDto? = null
)
data class CoupleSummaryDto(val totalBalance: Double, val totalIncome: Double, val totalExpense: Double)
data class CategoryDto(val id: String, val name: String, val transactionType: String)
data class CreateTransactionRequest(
    val userId: String, val accountId: String, val categoryId: String? = null,
    val type: String, val amount: Double, val description: String? = null,
    val visibility: String = "PRIVATE"
)
data class MessageDto(val id: String, val message: String, val createdAt: String, val sender: SenderDto)
data class SenderDto(val id: String, val displayName: String)
data class SendMessageRequest(val userId: String, val message: String)

// ===== DTO BARU (fitur tambahan, tidak mengubah DTO di atas) =====

data class TransferDto(
    val id: String, val amount: Double, val description: String?, val transferDate: String,
    val fromAccount: AccountDto?, val toAccount: AccountDto?
)
data class CreateTransferRequest(
    val userId: String, val fromAccountId: String, val toAccountId: String,
    val amount: Double, val description: String? = null
)

data class CategoryTotalDto(val category: String, val total: Double)
data class MonthlyReportDto(
    val period: String,
    val scope: String,
    val totalIncome: Double,
    val totalExpense: Double,
    val netCashFlow: Double,
    val totalBalance: Double,
    val transactionCount: Int,
    val incomeByCategory: List<CategoryTotalDto>,
    val expenseByCategory: List<CategoryTotalDto>
)
data class CashflowPointDto(val month: String, val income: Double, val expense: Double)

interface Api {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("accounts")
    suspend fun accounts(@Query("userId") userId: String): List<AccountDto>

    @GET("transactions")
    suspend fun transactions(@Query("userId") userId: String): List<TransactionDto>

    @POST("transactions")
    suspend fun createTransaction(@Body request: CreateTransactionRequest): TransactionDto

    @GET("couple/messages")
    suspend fun messages(@Query("userId") userId: String): List<MessageDto>

    @POST("couple/messages")
    suspend fun sendMessage(@Body request: SendMessageRequest): MessageDto

    @GET("couple/summary")
    suspend fun coupleSummary(@Query("userId") userId: String): CoupleSummaryDto

    @GET("couple/accounts")
    suspend fun coupleAccounts(@Query("userId") userId: String): List<AccountDto>

    @GET("couple/transactions")
    suspend fun coupleTransactions(@Query("userId") userId: String): List<TransactionDto>

    // ===== Endpoint BARU (fitur tambahan, endpoint di atas tidak diubah) =====

    @GET("categories")
    suspend fun categories(@Query("userId") userId: String): List<CategoryDto>

    @GET("transfers")
    suspend fun transfers(@Query("userId") userId: String): List<TransferDto>

    @POST("transfers")
    suspend fun createTransfer(@Body request: CreateTransferRequest): TransferDto

    @GET("couple/report")
    suspend fun monthlyReport(
        @Query("userId") userId: String,
        @Query("year") year: Int,
        @Query("month") month: Int,
        @Query("scope") scope: String
    ): MonthlyReportDto

    @GET("couple/cashflow")
    suspend fun cashflow(
        @Query("userId") userId: String,
        @Query("scope") scope: String,
        @Query("months") months: Int = 6
    ): List<CashflowPointDto>
}
