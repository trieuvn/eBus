package com.example.project_bus.data.services

import com.example.project_bus.data.SupabaseProvider
import com.example.project_bus.data.Tables
import com.example.project_bus.data.models.AppUser
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthService {

    private val client = SupabaseProvider.client

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

        // Theo docs: Confirm email ON => trả về user (session null)
        // Confirm email OFF => trả về null (session có)
        val authId = signedUpUser?.id
            ?: client.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("Không lấy được authId từ Supabase sau khi đăng ký.")

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
            ?: throw IllegalStateException("Đăng nhập thành công nhưng không có session/user.")

        return getProfileByAuthId(authId)
    }

    suspend fun signOut() {
        client.auth.signOut()
    }

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
