package com.example.data.repo

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.User
import com.example.data.local.UserDao
import com.example.util.PasswordHasher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Data model representing the active authenticated session.
 */
data class UserSession(
    val id: Long,
    val fullName: String,
    val email: String,
    val isGuest: Boolean = false
)

/**
 * Repository responsible for user authentication, registration, session management,
 * and guest access backed by the local Room database and SharedPreferences.
 */
class AuthRepository(
    private val userDao: UserDao,
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_session_prefs", Context.MODE_PRIVATE)

    private val _sessionState = MutableStateFlow<UserSession?>(loadInitialSession())
    val sessionState: StateFlow<UserSession?> = _sessionState.asStateFlow()

    private fun loadInitialSession(): UserSession? {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (!isLoggedIn) return null

        val isGuest = prefs.getBoolean(KEY_IS_GUEST, false)
        val id = prefs.getLong(KEY_USER_ID, -1L)
        val name = prefs.getString(KEY_USER_NAME, "") ?: ""
        val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""

        return UserSession(
            id = id,
            fullName = if (isGuest) "Guest User" else name,
            email = email,
            isGuest = isGuest
        )
    }

    /**
     * Registers a new user account into the local Room database.
     */
    suspend fun register(
        fullName: String,
        email: String,
        password: String
    ): Result<UserSession> = withContext(ioDispatcher) {
        val cleanName = fullName.trim()
        val cleanEmail = email.trim().lowercase()

        if (cleanName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter your name."))
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (password.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }

        // Check if email already exists
        val existing = userDao.getUserByEmail(cleanEmail)
        if (existing != null) {
            return@withContext Result.failure(IllegalStateException("An account with this email already exists."))
        }

        // Generate cryptographic salt and hash
        val salt = PasswordHasher.generateSalt()
        val passwordHash = PasswordHasher.hashPassword(password, salt)

        val newUser = User(
            fullName = cleanName,
            email = cleanEmail,
            passwordHash = passwordHash,
            salt = salt
        )

        try {
            val generatedId = userDao.insertUser(newUser)
            val session = UserSession(
                id = generatedId,
                fullName = cleanName,
                email = cleanEmail,
                isGuest = false
            )
            saveSession(session)
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Authenticates an existing user against the local Room database.
     */
    suspend fun login(
        email: String,
        password: String
    ): Result<UserSession> = withContext(ioDispatcher) {
        val cleanEmail = email.trim().lowercase()

        if (cleanEmail.isBlank() || password.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Email and password cannot be empty."))
        }

        val user = userDao.getUserByEmail(cleanEmail)
            ?: return@withContext Result.failure(IllegalArgumentException("No account found with this email."))

        val isPasswordValid = PasswordHasher.verifyPassword(password, user.salt, user.passwordHash)
        if (!isPasswordValid) {
            return@withContext Result.failure(IllegalArgumentException("Incorrect password."))
        }

        val session = UserSession(
            id = user.id,
            fullName = user.fullName,
            email = user.email,
            isGuest = false
        )
        saveSession(session)
        Result.success(session)
    }

    /**
     * Logs in as a Guest without requiring credentials.
     */
    fun continueAsGuest(): UserSession {
        val session = UserSession(
            id = 0L,
            fullName = "Guest User",
            email = "guest@local",
            isGuest = true
        )
        saveSession(session)
        return session
    }

    /**
     * Clears the current session and logs out the user.
     */
    fun logout() {
        prefs.edit()
            .clear()
            .apply()
        _sessionState.value = null
    }

    private fun saveSession(session: UserSession) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putBoolean(KEY_IS_GUEST, session.isGuest)
            .putLong(KEY_USER_ID, session.id)
            .putString(KEY_USER_NAME, session.fullName)
            .putString(KEY_USER_EMAIL, session.email)
            .apply()
        _sessionState.value = session
    }

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_IS_GUEST = "is_guest"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
    }
}
