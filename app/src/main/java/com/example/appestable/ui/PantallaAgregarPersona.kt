package com.example.appestable.ui


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAgregarPersona(viewModel: PersonaViewModel) {
    val cs = MaterialTheme.colorScheme
    val shapes = MaterialTheme.shapes

    // Estados de los campos
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var celular by remember { mutableStateOf("") }
    var familia by remember { mutableStateOf("") }
    var esJefe by remember { mutableStateOf(false) }

    // Validación celular
    val celularError = celular.any { !it.isDigit() } ||
            (celular.length != 10 && celular.isNotEmpty())

    // Ladas / países
    val paises = remember {
        listOf(
            Pais("+52", "México", "\uD83C\uDDF2\uD83C\uDDFD"),
            Pais("+1", "Estados Unidos", "\uD83C\uDDFA\uD83C\uDDF8"),
            Pais("+34", "España", "\uD83C\uDDEA\uD83C\uDDF8"),
            Pais("+86", "China", "\uD83C\uDDE8\uD83C\uDDF3"),
            Pais("+61", "Australia", "\uD83C\uDDE6\uD83C\uDDFA")
            // ... agrega los demás
        )
    }
    var paisSeleccionado by remember { mutableStateOf(paises[0]) }
    var ladaExpanded by remember { mutableStateOf(false) }

    // Datos desde ViewModel
    val personas by viewModel.persona.collectAsState()
    val familiasDb by viewModel.familiaList.collectAsState()

    // Dropdown de familia
    var familiaExpanded by remember { mutableStateOf(false) }
    val familiasNombres = familiasDb
        .map { it.nombreFamilia }
        .distinct()
        .filter { it.isNotBlank() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Título
        Text(
            "Agregar Persona",
            style = MaterialTheme.typography.headlineSmall,
            color = cs.primary
        )
        Spacer(Modifier.height(12.dp))

        // Nombre
        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre") },
            singleLine = true,
            shape = shapes.medium,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        // Email (opcional)
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email (opcional)") },
            singleLine = true,
            shape = shapes.medium,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        // LADA + Celular
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // LADA desplegable
            OutlinedTextField(
                value = "${paisSeleccionado.bandera} ${paisSeleccionado.lada}",
                onValueChange = { },
                readOnly = true,
                trailingIcon = {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                },
                singleLine = true,
                shape = shapes.medium,
                modifier = Modifier
                    .width(100.dp)
                    .clickable { ladaExpanded = true }
            )
            DropdownMenu(
                expanded = ladaExpanded,
                onDismissRequest = { ladaExpanded = false }
            ) {
                paises.forEach { p ->
                    DropdownMenuItem(
                        text = { Text("${p.bandera} ${p.lada} ${p.nombre}") },
                        onClick = {
                            paisSeleccionado = p
                            ladaExpanded = false
                        }
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Celular con tu validación intacta
            OutlinedTextField(
                value = celular,
                onValueChange = {
                    if (it.length <= 10 && it.all { ch -> ch.isDigit() }) {
                        celular = it
                    }
                },
                label = { Text("Celular (10 dígitos)") },
                isError = celularError && celular.isNotEmpty(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = shapes.medium,
                modifier = Modifier.weight(1f)
            )
        }
        if (celularError && celular.isNotEmpty()) {
            Text(
                "El celular debe tener exactamente 10 dígitos.",
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
        Spacer(Modifier.height(8.dp))

        // Familia: texto libre + dropdown
        ExposedDropdownMenuBox(
            expanded = familiaExpanded,
            onExpandedChange = { familiaExpanded = !familiaExpanded }
        ) {
            OutlinedTextField(
                value = familia,
                onValueChange = { familia = it },
                label = { Text("Familia") },
                singleLine = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = familiaExpanded)
                },
                shape = shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = familiaExpanded,
                onDismissRequest = { familiaExpanded = false }
            ) {
                familiasNombres.forEach { fn ->
                    DropdownMenuItem(
                        text = { Text(fn) },
                        onClick = {
                            familia = fn
                            familiaExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        // Checkbox jefe de familia
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = esJefe,
                onCheckedChange = { esJefe = it }
            )
            Spacer(Modifier.width(4.dp))
            Text("¿Es jefe de familia?")
        }
        Spacer(Modifier.height(12.dp))

        // Botón Guardar
        Button(
            onClick = {
                viewModel.agregarPersona(
                    nombre.trim(),
                    email.trim(),
                    "${paisSeleccionado.lada}$celular",
                    familia.trim(),
                    esJefe
                )
                // reset
                nombre = ""; email = ""; celular = ""; familia = ""; esJefe = false
                paisSeleccionado = paises[0]
            },
            enabled = nombre.isNotBlank()
                    && celular.length == 10
                    && familia.isNotBlank()
                    && !celularError,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar Persona")
        }
        Spacer(Modifier.height(12.dp))

        Divider()
        Spacer(Modifier.height(12.dp))

        // Listado agrupado por familia
        Text("Personas registradas:", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))

        val porFamilia = personas.groupBy { it.familiaId }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            familiasDb.forEach { famDb ->
                val miembros = porFamilia[famDb.id]?.toMutableList() ?: return@forEach
                if (miembros.isEmpty()) return@forEach
                miembros.sortByDescending { it.esJefe }

                item {
                    Text(
                        "👪 Familia: ${famDb.nombreFamilia}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                items(miembros) { p ->
                    Card(
                        shape = shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = p.nombre + if (p.esJefe) " (Jefe)" else "",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(p.celular, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { viewModel.eliminarPersona(p) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                            }
                        }
                    }
                }
            }
        }
    }
}

// Clase para LADA/paises
data class Pais(val lada: String, val nombre: String, val bandera: String)
