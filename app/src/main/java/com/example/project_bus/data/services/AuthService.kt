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
     *
     * Lưu ý quan trọng:
     * - Nếu Supabase bật "Confirm email" thì sau signUp user CHƯA đăng nhập ngay.
     * - Metadata (full_name/phone_number/role) sẽ nằm trong user metadata.
     *   Bạn nên tạo TRIGGER ở DB để tự đổ metadata này sang bảng public."User".
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

        // If "Confirm email" is ON => Supabase returns a user (session is null)
        // If "Confirm email" is OFF => Supabase returns null (session exists)
        val authId = signedUpUser?.id
            ?: client.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("Unable to get authId from Supabase after sign-up.")

        val isLoggedInNow = client.auth.currentUserOrNull() != null
        return SignUpResult(authId = authId, needsEmailConfirmation = !isLoggedInNow)
    }

    /**
     * Đăng nhập bằng Supabase Auth (email + password).
     * Nếu DB đã setup trigger, bảng public."User" sẽ có profile tương ứng.
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
     * Xác thực mã OTP (6 chữ số) được gửi trong email để hoàn tất signup.
     *
     * Supabase (Kotlin) xác thực OTP qua verifyEmailOtp().
     * Lưu ý: Supabase dùng OtpType.Email.EMAIL để verify OTP code ({{ .Token }}).
     */
    suspend fun verifySignupOtp(email: String, otp: String) {
        client.auth.verifyEmailOtp(
            type = OtpType.Email.EMAIL,
            email = email.trim(),
            token = otp.trim()
        )
    }

    /**
     * Resend verification (signup confirmation) email.
     * Supabase chỉ resend nếu trước đó đã có attempt signup/email change.
     */
    suspend fun resendSignupOtp(email: String) {
        client.auth.resendEmail(OtpType.Email.SIGNUP, email.trim())
    }

    // Giữ lại tên cũ để các chỗ khác (nếu có) không bị vỡ.
    suspend fun resendSignupConfirmation(email: String) = resendSignupOtp(email)

suspend fun getProfileByAuthId(authId: String): AppUser? {
        val list = client
            .from(Tables.USERS)
            .select {
                filter { eq("authid", authId) }
            }
            .decodeList<AppUser>()

        return list.firstOrNull()
    }
}


