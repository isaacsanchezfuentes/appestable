package com.example.appestable

import android.os.Bundle
import android.util.Log

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Person

import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import androidx.compose.ui.Modifier

import androidx.lifecycle.viewmodel.compose.viewModel

import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.Callback
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials

import com.example.appestable.ui.PantallaActividades
import com.example.appestable.ui.PantallaAgregarPersona
import com.example.appestable.ui.PantallaResumen
import com.example.appestable.ui.PersonaViewModel
import com.example.appestable.ui.PersonaViewModelFactory
import com.example.appestable.ui.theme.AppestableTheme

import kotlinx.coroutines.launch

import com.auth0.android.jwt.JWT

class MainActivity : ComponentActivity() {

    private lateinit var account: Auth0

    // 🔥 Estado simple de autenticación
    private var isLoggedIn by mutableStateOf(false)

    private var userEmail by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Auth0
        account = Auth0(
            "q9hPzu6loAkYwN0oNi6bakQi3T3t0iA4",
            "dev-zbne73xs48twrr2a.us.auth0.com"
        )

        setContent {

            AppestableTheme {

                // Pager
                val pages = listOf(
                    "Personas",
                    "Actividades",
                    "Resumen"
                )

                val icons = listOf(
                    Icons.Default.Person,
                    Icons.AutoMirrored.Filled.List,
                    Icons.Default.AttachMoney
                )

                val pagerState = rememberPagerState(
                    initialPage = 0,
                    pageCount = { pages.size }
                )

                val scope = androidx.compose.runtime.rememberCoroutineScope()

                // ViewModel
                val vm: PersonaViewModel =
                    viewModel(
                        factory = PersonaViewModelFactory(application)
                    )

                Scaffold(

                    bottomBar = {

                        NavigationBar {

                            pages.forEachIndexed { i, label ->

                                NavigationBarItem(

                                    selected = pagerState.currentPage == i,

                                    onClick = {
                                        scope.launch {
                                            pagerState.animateScrollToPage(i)
                                        }
                                    },

                                    icon = {
                                        Icon(
                                            icons[i],
                                            contentDescription = label
                                        )
                                    },

                                    label = {
                                        Text(label)
                                    }
                                )
                            }
                        }
                    }

                ) { padding ->

                    Column(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                    ) {

                        // 🔥 UI reactiva basada en auth

                        if (!isLoggedIn) {

                            Button(
                                onClick = { login() }
                            ) {
                                Text("Login Auth0")
                            }

                        } else {

                            Text("✅ Sesión iniciada")

                            Text("Usuario: $userEmail")

                            Button(
                                onClick = { logout() }
                            ) {
                                Text("Logout")
                            }
                        }

                        HorizontalPager(
                            modifier = Modifier.weight(1f),
                            state = pagerState

                        ) { page ->

                            when (page) {

                                0 -> PantallaAgregarPersona(vm)

                                1 -> PantallaActividades(vm)

                                2 -> PantallaResumen(vm)
                            }
                        }
                    }
                }
            }
        }
    }

    // 🔥 LOGIN

    private fun login() {

        WebAuthProvider
            .login(account)
            .withScheme("appestable")
            .withConnection(connectionName = "google-oauth2")
            .start(
                this,

                object : Callback<Credentials, AuthenticationException> {

                    override fun onFailure(
                        error: AuthenticationException
                    ) {

                        Log.e(
                            "AUTH0",
                            error.getDescription() ?: "Login failed"
                        )
                    }

                    override fun onSuccess(
                        result: Credentials
                    ) {

                        // 🔥 Actualiza estado UI
                        isLoggedIn = true

                        val jwt = JWT(result.idToken)

                        userEmail = jwt.getClaim("email")
                            .asString() ?: "Usuario autenticado"

                        Log.d(
                            "AUTH0",
                            "ACCESS TOKEN: ${result.accessToken}"
                        )

                        Log.d(
                            "AUTH0",
                            "ID TOKEN: ${result.idToken}"
                        )
                    }
                }
            )
    }

    // 🔥 LOGOUT

    private fun logout() {

        WebAuthProvider
            .logout(account)
            .withScheme("appestable")
            .start(
                this,

                object : Callback<Void?, AuthenticationException> {

                    override fun onSuccess(result: Void?) {

                        isLoggedIn = false

                        userEmail = ""

                        Log.d(
                            "AUTH0",
                            "Logout OK"
                        )
                    }

                    override fun onFailure(
                        error: AuthenticationException
                    ) {

                        Log.e(
                            "AUTH0",
                            error.getDescription() ?: "Logout failed"
                        )
                    }
                }
            )
    }
}