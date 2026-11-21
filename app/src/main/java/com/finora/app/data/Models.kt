package com.finora.app.data

data class Transaction(
    val id: String,
    val amount: Long,
    val note: String,
    val category: String,
    val type: String,          // "income" | "expense"
    val createdAt: Long,       // epoch millis
    val accountId: String? = null,
    val loanId: String? = null
)

data class Category(
    val key: String,
    val name: String,
    val emoji: String,
    val color: String,         // "#rrggbb"
    val type: String,          // "income" | "expense"
    val budgetLimit: Long,
    val builtin: Boolean
)

data class Goal(
    val id: String,
    val title: String,
    val target: Long,
    val saved: Long,
    val deadline: String,      // Gregorian ISO day "yyyy-MM-dd" or ""
    val createdAt: Long,
    val color: String
)

data class Account(
    val id: String,
    val name: String,
    val icon: String,
    val color: String,
    val last4: String,
    val opening: Double,
    val loanDefault: Boolean,
    val createdAt: Long
)

data class AccountTransfer(
    val id: String,
    val from: String,
    val to: String,
    val amount: Double,
    val createdAt: Long
)

data class Profile(
    val name: String,
    val avatar: String,        // data: URL (SVG preset or uploaded image) or ""
    val avatarIndex: Int,
    val avatarMode: String     // "upload" | "preset"
)

data class Loan(
    val id: String,
    val title: String,
    val installmentAmount: Double,
    val totalInstallments: Double,
    val paidInstallments: Double,
    val dueDay: Int,
    val createdAt: Long
)

/** One archived Jalali month (`finora_monthly_archive[<jy>_<mm>]`). */
data class ArchiveEntry(
    val key: String,
    val jy: Int,
    val jm: Int,
    val initialBalance: Double,
    val transactions: List<Transaction>
)

/** Everything the screens read. Parsed from the raw key/value blobs (same format as the web app). */
data class AppData(
    val initialBalance: Long = 0,
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = DataParser.defaultCategories,
    val goals: List<Goal> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val accountTransfers: List<AccountTransfer> = emptyList(),
    val profile: Profile = DataParser.defaultProfile,
    val loansJson: String = "[]",
    val loans: List<Loan> = emptyList(),
    val archive: Map<String, ArchiveEntry> = emptyMap()
)
