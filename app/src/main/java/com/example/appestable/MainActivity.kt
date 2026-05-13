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

import androidx.compose.runtime.rememberCoroutineScope
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

class MainActivity : ComponentActivity() {

    private lateinit var account: Auth0

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

                val scope = rememberCoroutineScope()

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

                        Button(
                            onClick = { login() }
                        ) {
                            Text("Login Auth0")
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

    private fun login() {

        WebAuthProvider
            .login(account)
            .withScheme("appestable")
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
}