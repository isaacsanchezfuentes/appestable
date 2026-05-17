package com.example.appestable.auth

import android.content.Context
import android.util.Log

import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.storage.CredentialsManager
import com.auth0.android.authentication.storage.CredentialsManagerException
import com.auth0.android.authentication.storage.SharedPreferencesStorage

import com.auth0.android.callback.Callback
import com.auth0.android.jwt.JWT
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import com.auth0.android.authentication.AuthenticationException

class AuthManager(
    private val context: Context
) {

    private val account = Auth0.getInstance(
        "q9hPzu6loAkYwN0oNi6bakQi3T3t0iA4",
        "dev-zbne73xs48twrr2a.us.auth0.com"
    )

    private val credentialsManager = CredentialsManager(
        AuthenticationAPIClient(account),
        SharedPreferencesStorage(context)
    )

    // 🔥 LOGIN
    fun login(
        onSuccess: (String, String) -> Unit,
        onError: (String) -> Unit
    ) {

        WebAuthProvider
            .login(account)
            .withScheme("appestable")
            .withConnection("google-oauth2")
            .withScope("openid profile email")
            .withAudience("https://appestable-api")

            .start(

                context,

                object : Callback<Credentials, AuthenticationException> {

                    override fun onSuccess(result: Credentials) {

                        // Guardar sesión
                        credentialsManager.saveCredentials(result)

                        val idToken = result.idToken

                        if (idToken == null) {

                            onSuccess(
                                "Usuario autenticado",
                                result.accessToken ?: ""
                            )

                            return
                        }

                        val jwt = JWT(idToken)

                        val email = jwt
                            .getClaim("email")
                            .asString()
                            ?: "Usuario autenticado"

                        val token = result.accessToken ?: ""

                        Log.d("AUTH0", "Login OK: $email")
                        Log.d("AUTH0", "TOKEN: $token")

                        onSuccess(email, token)
                    }

                    override fun onFailure(error: AuthenticationException) {

                        Log.e(
                            "AUTH0",
                            error.getDescription() ?: "Login failed"
                        )

                        onError(
                            error.getDescription()
                                ?: "Login failed"
                        )
                    }
                }
            )
    }

    // 🔥 LOGOUT
    fun logout(
        onLogout: () -> Unit
    ) {

        WebAuthProvider
            .logout(account)
            .withScheme("appestable")
            .start(

                context,

                object : Callback<Void?, AuthenticationException> {

                    override fun onSuccess(result: Void?) {

                        credentialsManager.clearCredentials()

                        Log.d("AUTH0", "Logout OK")

                        onLogout()
                    }

                    override fun onFailure(error: AuthenticationException) {

                        Log.e(
                            "AUTH0",
                            error.getDescription() ?: "Logout failed"
                        )
                    }
                }
            )
    }

    // 🔥 RESTORE SESSION
    fun restoreSession(
        onSessionRestored: (String) -> Unit
    ) {

        if (!credentialsManager.hasValidCredentials()) return

        credentialsManager.getCredentials(

            object : Callback<
                    Credentials,
                    CredentialsManagerException
                    > {

                override fun onSuccess(
                    result: Credentials
                ) {

                    val idToken = result.idToken ?: return

                    val jwt = JWT(idToken)

                    val email = jwt
                        .getClaim("email")
                        .asString()
                        ?: "Usuario autenticado"

                    val token = result.accessToken

                    Log.d(
                        "AUTH0",
                        "Session restored: $email"
                    )

                    Log.d(
                        "AUTH0",
                        "TOKEN RESTORED: $token"
                    )

                    onSessionRestored(email)
                }

                override fun onFailure(error: CredentialsManagerException) {

                    Log.e(
                        "AUTH0",
                        "Restore session failed"
                    )
                }
            }
        )
    }

    fun isLoggedIn(): Boolean {

        return credentialsManager
            .hasValidCredentials()
    }
}