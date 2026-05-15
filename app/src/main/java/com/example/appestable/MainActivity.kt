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

import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // 🔥 Auth desacoplado
    private lateinit var authManager: AuthManager

    // 🔥 Estado UI
    private var isLoggedIn by mutableStateOf(false)

    private var userEmail by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔥 Inicializar AuthManager
        authManager = AuthManager(this)

        // 🔥 Restaurar sesión persistente
        if (authManager.isLoggedIn()) {

            authManager.restoreSession {

                userEmail = it

                isLoggedIn = true
            }
        }

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

                val scope =
                    androidx.compose.runtime
                        .rememberCoroutineScope()

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

                                    selected =
                                        pagerState.currentPage == i,

                                    onClick = {

                                        scope.launch {

                                            pagerState
                                                .animateScrollToPage(i)
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

                        // 🔥 UI REACTIVA

                        if (!isLoggedIn) {

                            Button(

                                onClick = {

                                    authManager.login(

                                        onSuccess = {

                                            userEmail = it

                                            isLoggedIn = true
                                        },

                                        onError = {

                                            Log.e(
                                                "AUTH0",
                                                it
                                            )
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
}