package com.example.appestable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appestable.ui.PantallaAgregarPersona
import com.example.appestable.ui.PantallaActividades
import com.example.appestable.ui.PantallaResumen
import com.example.appestable.ui.PersonaViewModel
import com.example.appestable.ui.PersonaViewModelFactory
import com.example.appestable.ui.theme.AppestableTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppestableTheme {
                // armado del Pager
                val pages = listOf("Personas","Actividades","Resumen")
                val icons = listOf(Icons.Default.Person, Icons.AutoMirrored.Filled.List, Icons.Default.AttachMoney)
                val pagerState = rememberPagerState(initialPage = 0, pageCount = { pages.size })
                val scope = rememberCoroutineScope()

                // tu ViewModel único
                val vm: PersonaViewModel = viewModel(factory = PersonaViewModelFactory(application))

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            pages.forEachIndexed { i, label ->
                                NavigationBarItem(
                                    selected = pagerState.currentPage == i,
                                    onClick = { scope.launch { pagerState.animateScrollToPage(i) } },
                                    icon  = { Icon(icons[i], contentDescription = label) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                ) { padding ->
                    HorizontalPager(
                        modifier = Modifier.padding(padding),
                        state    = pagerState
                    ) { page ->
                        when(page) {
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
