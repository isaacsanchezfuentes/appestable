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

import com.example.appestable.auth.AuthManager

import com.example.appestable.ui.PantallaActividades
import com.example.appestable.ui.PantallaAgregarPersona
import com.example.appestable.ui.PantallaResumen
import com.example.appestable.ui.PersonaViewModel
import com.example.appestable.ui.PersonaViewModelFactory
import com.example.appestable.ui.theme.AppestableTheme

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class MainActivity : ComponentActivity() {

    // 🔥 Auth desacoplado
    private lateinit var authManager: AuthManager

    // 🔥 Estado UI
    private var isLoggedIn by mutableStateOf(false)
    private var userEmail by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔥 AuthManager
        authManager = AuthManager(this)

        // 🔥 Restaurar sesión
        if (authManager.isLoggedIn()) {
            authManager.restoreSession { email, token ->
                userEmail = email
                isLoggedIn = true
                // Validar contra backend al restaurar
                validarContraBackend(token)
            }
        }

        setContent {
            AppestableTheme {

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

                        // 🔥 UI REACTIVA AUTH

                        if (!isLoggedIn) {
                            Button(
                                onClick = {
                                    authManager.login(
                                        onSuccess = { email, token ->
                                            userEmail = email
                                            isLoggedIn = true
                                            Log.d("AUTH0", "TOKEN: $token")
                                            validarContraBackend(token, vm)
                                        },
                                        onError = {
                                            Log.e("AUTH0", it)
                                        }
                                    )
                                }
                            ) {
                                Text("Login Auth0")
                            }

                        } else {
                            Text("✅ Sesión iniciada")
                            Text("Usuario: $userEmail")

                            Button(
                                onClick = {
                                    authManager.logout {
                                        isLoggedIn = false
                                        userEmail = ""
                                    }
                                }
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

    private fun validarContraBackend(token: String, viewModel: PersonaViewModel? = null) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Validar Token (Get Me)
                val response = com.example.appestable.network.RetrofitClient.api.getMe("Bearer $token")
                
                if (response.isSuccessful) {
                    val me = response.body()
                    Log.d("API", "SUCCESS LOGIN: $me")
                    
                    // 2. Intentar registrar/sincronizar el usuario automáticamente
                    // Usamos valores temporales o los que vienen de Auth0
                    val request = com.example.appestable.network.PersonaRequest(
                        nombre = userEmail.split("@")[0], // Nombre basado en email
                        familia_nombre = "Mi Familia",     // Familia por defecto
                        email = userEmail,
                        es_jefe = true
                    )
                    
                    val regResponse = com.example.appestable.network.RetrofitClient.api.registrarPersona(
                        "Bearer $token",
                        request
                    )
                    
                    if (regResponse.isSuccessful) {
                        Log.d("API", "USER SYNCED: ${regResponse.body()}")
                        viewModel?.sincronizarPersonasDesdeBackend()
                    }

                } else {
                    Log.e("API", "ERROR CODE: ${response.code()} - ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("API", "CONNECTION ERROR: ${e.message}")
            }
        }
    }
}
