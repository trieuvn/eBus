package com.example.project_bus.data.services

import com.example.project_bus.data.SupabaseProvider
import com.example.project_bus.data.Tables
import com.example.project_bus.data.models.AppUser
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

class AuthService {

    private val client = SupabaseProvider.client

    /**
     * Đăng nhập/đăng ký bằng Google (OAuth).
     *
     * Lưu ý: cần cấu hình Deeplink + Redirect URLs đúng để callback quay lại app.
     */
    suspend fun signInWithGoogle() {
        client.auth.signInWith(Google)
    }

    data class SignUpResult(
        val authId: String,
        /** true = Supabase đang bật Confirm email => user cần xác thực email trước khi login */
        val needsEmailConfirmation: Boolean
    )

    /**
     * Đăng ký bằng Supabase Auth (email + password).
     */
    suspend fun signUp(
        email: String,
        password: String,
        fullName: String,
        phoneNumber: String,
        role: Int = 0
    ): SignUpResult {
        val cleanEmail = email.trim()
        val cleanPassword = password.trim()
        val cleanFullName = fullName.trim()
        val cleanPhone = phoneNumber.trim()

        val signedUpUser = client.auth.signUpWith(Email) {
            this.email = cleanEmail
            this.password = cleanPassword
            data = buildJsonObject {
                put("full_name", cleanFullName)
                put("phone_number", cleanPhone)
                put("role", role)
            }
        }

        val authId = signedUpUser?.id
            ?: client.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("Unable to get authId from Supabase after sign-up.")

        val isLoggedInNow = client.auth.currentUserOrNull() != null
        return SignUpResult(authId = authId, needsEmailConfirmation = !isLoggedInNow)
    }

    /**
     * Đăng nhập bằng Supabase Auth (email + password).
     */
    suspend fun signIn(email: String, password: String): AppUser? {
        val cleanEmail = email.trim()
        val cleanPassword = password.trim()

        client.auth.signInWith(Email) {
            this.email = cleanEmail
            this.password = cleanPassword
        }

        val authId = client.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("Signed in successfully, but no session/user was returned.")

        return getProfileByAuthId(authId)
    }

    suspend fun signOut() {
        client.auth.signOut()
    }

    /**
     * Xác thực OTP code gửi qua email khi đăng ký.
     * Supabase gửi OTP trong email khi bật "Confirm email" trong project settings.
     */
    suspend fun verifySignupOtp(email: String, otp: String) {
        client.auth.verifyEmailOtp(
            type = OtpType.Email.SIGNUP,
            email = email.trim(),
            token = otp.trim()
        )
    }

    /**
     * Gửi lại OTP code đến email (resend confirmation).
     * Lưu ý: Supabase có rate limit (mặc định 60s giữa các lần gửi).
     */
    suspend fun resendSignupOtp(email: String) {
        client.auth.resendEmail(
            type = OtpType.Email.SIGNUP,
            email = email.trim()
        )
    }


    suspend fun getProfileByAuthId(authId: String): AppUser? {
        return try {
            val list = client
                .from(Tables.USERS)
                .select {
                    filter { eq("authid", authId) }
                }
                .decodeList<AppUser>()
            list.firstOrNull()
        } catch (e: Exception) {
            android.util.Log.e("AuthService", "getProfileByAuthId error: ${e.message}")
            null
        }
    }

    /**
     * Lấy user theo id (primary key)
     */
    suspend fun getProfileById(id: String): AppUser? {
        return try {
            val list = client
                .from(Tables.USERS)
                .select {
                    filter { eq("id", id) }
                }
                .decodeList<AppUser>()
            android.util.Log.d("AuthService", "getProfileById: found ${list.size} users for id: $id")
            list.firstOrNull()
        } catch (e: Exception) {
            android.util.Log.e("AuthService", "getProfileById error: ${e.message}")
            null
        }
    }

    /**
     * Lấy user theo email (thử nhiều cách)
     */
    suspend fun getProfileByEmail(email: String): AppUser? {
        return try {
            // Thử với eq trước (exact match)
            var list = client
                .from(Tables.USERS)
                .select {
                    filter { eq("email", email) }
                }
                .decodeList<AppUser>()
            
            android.util.Log.d("AuthService", "getProfileByEmail (eq): found ${list.size} users for email: $email")
            
            if (list.isEmpty()) {
                // Nếu không tìm thấy, thử với email lowercase
                list = client
                    .from(Tables.USERS)
                    .select {
                        filter { eq("email", email.lowercase()) }
                    }
                    .decodeList<AppUser>()
                android.util.Log.d("AuthService", "getProfileByEmail (lowercase): found ${list.size} users")
            }
            
            list.firstOrNull()
        } catch (e: Exception) {
            android.util.Log.e("AuthService", "getProfileByEmail error: ${e.message}")
            null
        }
    }

    /**
     * Cập nhật authid cho user theo id
     */
    private suspend fun updateAuthIdByEmail(email: String, authId: String): Boolean {
        return try {
            client.from(Tables.USERS)
                .update({
                    set("authid", authId)
                }) {
                    filter { ilike("email", email) }
                }
            true
        } catch (e: Exception) {
            android.util.Log.e("AuthService", "updateAuthIdByEmail error: ${e.message}")
            false
        }
    }

    /**
     * Xử lý sau khi đăng nhập Google thành công.
     * 1. Kiểm tra user theo authid
     * 2. Nếu không có, kiểm tra theo email (case-insensitive)
     * 3. Nếu email tồn tại -> cập nhật authid
     * 4. Nếu không tồn tại -> tạo mới
     * 5. Nếu insert bị duplicate -> cập nhật authid
     * 
     * full_name lấy từ Display name của Authentication (metadata)
     */
    suspend fun handleOAuthSuccess(): AppUser {
        val authUser = client.auth.currentUserOrNull()
            ?: throw Exception("Không tìm thấy session sau khi đăng nhập Google")

        val authId = authUser.id
        val email = authUser.email ?: throw Exception("Không có email từ tài khoản Google")

        android.util.Log.d("AuthService", "handleOAuthSuccess: authId=$authId, email=$email")

        // Lấy full_name từ metadata của Authentication (Display name)
        val fullName = authUser.userMetadata?.get("full_name")?.toString()?.removeSurrounding("\"")
            ?: authUser.userMetadata?.get("name")?.toString()?.removeSurrounding("\"")
            ?: email.substringBefore("@")

        // 1. Kiểm tra user theo authid trước
        val existingUser = getProfileByAuthId(authId)
        android.util.Log.d("AuthService", "Check by authId: ${existingUser != null}")

        if (existingUser != null) {
            return existingUser
        }

        // 2. Thử update authid cho user có email này (có thể đã đăng ký trước)
        android.util.Log.d("AuthService", "Trying to update existing user by email: $email")
        
        try {
            // Update authid cho user có email trùng (dùng ilike cho case-insensitive)
            client.from(Tables.USERS)
                .update({
                    set("authid", authId)
                    set("full_name", fullName)
                }) {
                    filter { ilike("email", email) }
                }

            android.util.Log.d("AuthService", "Update by email completed, checking result...")

            // Kiểm tra xem update có thành công không
            val updatedUser = getProfileByAuthId(authId)
            if (updatedUser != null) {
                android.util.Log.d("AuthService", "Update successful, user found by authId")
                return updatedUser
            }
        } catch (updateError: Exception) {
            android.util.Log.e("AuthService", "Update by email error: ${updateError.message}")
        }

        // 3. Nếu không tìm thấy user nào có email đó, tạo mới
        // Sử dụng authId làm id để match với foreign key auth.users.id
        android.util.Log.d("AuthService", "Creating new user with id=authId: $authId")
        
        try {
            client.from(Tables.USERS)
                .insert(buildJsonObject {
                    put("id", authId)  // Dùng authId thay vì random UUID
                    put("email", email)
                    put("password", "123")
                    put("full_name", fullName)
                    put("role", 0)
                    put("authid", authId)
                })

            android.util.Log.d("AuthService", "Insert successful")

            return AppUser(
                id = authId,
                email = email,
                password = "123",
                fullName = fullName,
                phoneNumber = null,
                role = 0,
                authId = authId
            )
        } catch (insertError: Exception) {
            android.util.Log.e("AuthService", "Insert error: ${insertError.message}")
            
            // Kiểm tra nếu là duplicate id (User_pkey)
            if (insertError.message?.contains("User_pkey", ignoreCase = true) == true ||
                (insertError.message?.contains("duplicate", ignoreCase = true) == true && 
                 insertError.message?.contains("(id)", ignoreCase = true) == true)) {
                
                android.util.Log.d("AuthService", "Duplicate id detected, trying to get user by id: $authId")
                
                // User với id này đã tồn tại, cập nhật authid và email
                try {
                    client.from(Tables.USERS)
                        .update({
                            set("authid", authId)
                            set("email", email)
                            set("full_name", fullName)
                        }) {
                            filter { eq("id", authId) }
                        }
                    
                    android.util.Log.d("AuthService", "Update by id successful")
                    
                    return AppUser(
                        id = authId,
                        email = email,
                        password = "123",
                        fullName = fullName,
                        phoneNumber = null,
                        role = 0,
                        authId = authId
                    )
                } catch (updateError: Exception) {
                    android.util.Log.e("AuthService", "Update by id error: ${updateError.message}")
                }
            }
            
            // Nếu là duplicate email, thử lấy user theo email
            val userByEmail = getProfileByEmail(email)
            if (userByEmail != null) {
                android.util.Log.d("AuthService", "Found user by email after insert error, updating authid")
                
                client.from(Tables.USERS)
                    .update({
                        set("authid", authId)
                    }) {
                        filter { eq("id", userByEmail.id) }
                    }
                
                return userByEmail.copy(authId = authId)
            }
            
            // Thử lấy user theo id
            val userById = getProfileById(authId)
            if (userById != null) {
                android.util.Log.d("AuthService", "Found user by id, returning")
                return userById
            }
            
            throw Exception("Không thể tạo hoặc liên kết tài khoản: ${insertError.message}")
        }
    }
}


