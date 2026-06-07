package com.example.appestable

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.example.appestable.auth.AuthManager
import com.example.appestable.data.RolViaje
import com.example.appestable.ui.PantallaActividades
import com.example.appestable.ui.PantallaAgregarPersona
import com.example.appestable.ui.PantallaResumen
import com.example.appestable.ui.PersonaViewModel
import com.example.appestable.ui.PersonaViewModelFactory
import com.example.appestable.ui.theme.AppestableTheme


class MainActivity : ComponentActivity() {

    private lateinit var authManager: AuthManager
    private var isLoggedIn by mutableStateOf(false)
    private var userEmail by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authManager = AuthManager(this)

        if (authManager.isLoggedIn()) {
            authManager.restoreSession { session ->
                userEmail = session.email
                isLoggedIn = true
            }
        }

        setContent {
            AppestableTheme {
                val vm: PersonaViewModel = viewModel(factory = PersonaViewModelFactory(application))
                val viajes by vm.viajes.collectAsState()
                val viajeActivo by vm.viajeActivo.collectAsState()
                val session by vm.session.collectAsState()
                val sincronizando by vm.sincronizando.collectAsState()

                var showCrearViaje by remember { mutableStateOf(false) }
                var nombreNuevoViaje by remember { mutableStateOf("") }
                var viajeMenuExpanded by remember { mutableStateOf(false) }

                val pages = listOf("Personas", "Actividades", "Resumen")
                val icons = listOf(
                    Icons.Default.Person,
                    Icons.AutoMirrored.Filled.List,
                    Icons.Default.AttachMoney
                )
                val pagerState = rememberPagerState(initialPage = 0, pageCount = { pages.size })
                val scope = rememberCoroutineScope()

                if (showCrearViaje) {
                    AlertDialog(
                        onDismissRequest = { showCrearViaje = false },
                        title = { Text("Nuevo viaje") },
                        text = {
                            OutlinedTextField(
                                value = nombreNuevoViaje,
                                onValueChange = { nombreNuevoViaje = it },
                                label = { Text("Nombre del viaje") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (nombreNuevoViaje.isNotBlank()) {
                                        vm.crearViaje(nombreNuevoViaje.trim())
                                        nombreNuevoViaje = ""
                                        showCrearViaje = false
                                    }
                                }
                            ) { Text("Crear") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCrearViaje = false }) { Text("Cancelar") }
                        }
                    )
                }

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            pages.forEachIndexed { i, label ->
                                NavigationBarItem(
                                    selected = pagerState.currentPage == i,
                                    onClick = {
                                        scope.launch { pagerState.animateScrollToPage(i) }
                                    },
                                    icon = { Icon(icons[i], contentDescription = label) },
                                    label = { Text(label) }
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
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                @OptIn(ExperimentalMaterial3Api::class)
                                ExposedDropdownMenuBox(
                                    expanded = viajeMenuExpanded,
                                    onExpandedChange = { viajeMenuExpanded = it },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = viajeActivo?.nombre ?: "Sin viaje",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Viaje activo") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(viajeMenuExpanded) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = viajeMenuExpanded,
                                        onDismissRequest = { viajeMenuExpanded = false }
                                    ) {
                                        viajes.forEach { viaje ->
                                            DropdownMenuItem(
                                                text = { Text(viaje.nombre) },
                                                onClick = {
                                                    vm.seleccionarViaje(viaje.id)
                                                    viajeMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                if (vm.canCreateViaje()) {
                                    IconButton(onClick = { showCrearViaje = true }) {
                                        Icon(Icons.Default.Add, contentDescription = "Crear viaje")
                                    }
                                }
                            }

                            val rolLabel = when (session.rol) {
                                RolViaje.ORGANIZADOR -> "Organizador"
                                RolViaje.JEFE_FAMILIA -> "Jefe de familia"
                                RolViaje.MIEMBRO -> "Miembro"
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Rol: $rolLabel",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (isLoggedIn) {
                                    TextButton(
                                        onClick = { vm.sincronizarDesdeBackend() },
                                        enabled = !sincronizando
                                    ) {
                                        Text(if (sincronizando) "Sync..." else "Sincronizar")
                                    }
                                }
                            }

                            Spacer(Modifier.height(4.dp))

                            if (!isLoggedIn) {
                                Button(
                                    onClick = {
                                        authManager.login(
                                            onSuccess = { authSession ->
                                                userEmail = authSession.email
                                                isLoggedIn = true
                                                vm.onAuthSession(authSession)
                                            },
                                            onError = { Log.e("AUTH0", it) }
                                        )
                                    }
                                ) { Text("Login Auth0") }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("✅ $userEmail", modifier = Modifier.weight(1f))
                                    Button(
                                        onClick = {
                                            authManager.logout {
                                                isLoggedIn = false
                                                userEmail = ""
                                                vm.onLogout()
                                            }
                                        }
                                    ) { Text("Logout") }
                                }
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