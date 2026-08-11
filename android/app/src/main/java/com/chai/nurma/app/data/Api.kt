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
}
