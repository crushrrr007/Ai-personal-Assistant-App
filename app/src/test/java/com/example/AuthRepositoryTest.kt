package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repo.AuthRepository
import com.example.util.PasswordHasher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var authRepository: AuthRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        authRepository = AuthRepository(
            userDao = database.userDao(),
            context = context
        )
    }

    @After
    fun tearDown() {
        authRepository.logout()
        database.close()
    }

    @Test
    fun passwordHasher_correctlyHashesAndVerifies() {
        val salt = PasswordHasher.generateSalt()
        val password = "SecurePassword123"
        val hash = PasswordHasher.hashPassword(password, salt)

        assertTrue(PasswordHasher.verifyPassword("SecurePassword123", salt, hash))
        assertFalse(PasswordHasher.verifyPassword("WrongPassword", salt, hash))
    }

    @Test
    fun register_and_login_success() = runTest {
        val regResult = authRepository.register("Himanshu Meena", "himanshu@test.com", "pass123")
        assertTrue(regResult.isSuccess)
        val session = regResult.getOrNull()
        assertNotNull(session)
        assertEquals("Himanshu Meena", session?.fullName)
        assertFalse(session?.isGuest == true)

        // Logout and log back in
        authRepository.logout()
        assertNull(authRepository.sessionState.value)

        val loginResult = authRepository.login("himanshu@test.com", "pass123")
        assertTrue(loginResult.isSuccess)
        assertEquals("himanshu@test.com", loginResult.getOrNull()?.email)
    }

    @Test
    fun register_duplicateEmail_fails() = runTest {
        authRepository.register("User One", "dup@test.com", "password")
        val dupResult = authRepository.register("User Two", "dup@test.com", "differentpass")

        assertTrue(dupResult.isFailure)
    }

    @Test
    fun login_wrongPassword_fails() = runTest {
        authRepository.register("User", "user@test.com", "correctpass")
        val loginResult = authRepository.login("user@test.com", "wrongpass")

        assertTrue(loginResult.isFailure)
    }

    @Test
    fun continueAsGuest_createsGuestSession() {
        val guestSession = authRepository.continueAsGuest()
        assertTrue(guestSession.isGuest)
        assertEquals("Guest User", guestSession.fullName)
        assertEquals(guestSession, authRepository.sessionState.value)
    }
}
