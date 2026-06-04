package com.example.appestable.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.appestable.data.Persona

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAgregarPersona(viewModel: PersonaViewModel) {

    val cs = MaterialTheme.colorScheme
    val shapes = MaterialTheme.shapes

    // Estados campos

    var nombre by remember {
        mutableStateOf("")
    }

    var email by remember {
        mutableStateOf("")
    }

    var celular by remember {
        mutableStateOf("")
    }

    var familia by remember {
        mutableStateOf("")
    }

    var esJefe by remember {
        mutableStateOf(false)
    }

    // Validación celular

    val celularError =
        celular.any { !it.isDigit() } ||
                (celular.length != 10 &&
                        celular.isNotEmpty())

    // Países / ladas

    val paises = remember {

        listOf(

            Pais(
                "+52",
                "México",
                "\uD83C\uDDF2\uD83C\uDDFD"
            ),

            Pais(
                "+1",
                "Estados Unidos",
                "\uD83C\uDDFA\uD83C\uDDF8"
            ),

            Pais(
                "+34",
                "España",
                "\uD83C\uDDEA\uD83C\uDDF8"
            ),

            Pais(
                "+86",
                "China",
                "\uD83C\uDDE8\uD83C\uDDF3"
            ),

            Pais(
                "+61",
                "Australia",
                "\uD83C\uDDE6\uD83C\uDDFA"
            )
        )
    }

    var paisSeleccionado by remember {
        mutableStateOf(paises[0])
    }

    var ladaExpanded by remember {
        mutableStateOf(false)
    }

    // Datos ViewModel

    val personas by
    viewModel.persona.collectAsState()

    val familiasDb by
    viewModel.familiaList.collectAsState()

    val mensajeError by
    viewModel.mensajeError.collectAsState()

    // Dialog error

    if (mensajeError != null) {

        AlertDialog(

            onDismissRequest = {
                viewModel.limpiarError()
            },

            confirmButton = {

                TextButton(

                    onClick = {
                        viewModel.limpiarError()
                    }

                ) {
                    Text("OK")
                }
            },

            title = {
                Text("Validación")
            },

            text = {
                Text(mensajeError!!)
            }
        )
    }

    // Dropdown familia

    var familiaExpanded by remember {
        mutableStateOf(false)
    }

    val familiasNombres = familiasDb

        .map {
            it.nombreFamilia
        }

        .distinct()

        .filter {
            it.isNotBlank()
        }

    val gruposFamilia = remember(familiasDb, personas) {
        val idsFamilias = familiasDb.map { it.id }.toSet()
        val grupos = familiasDb
            .sortedBy { it.nombreFamilia.lowercase() }
            .map { famDb ->
                val miembros = personas
                    .filter { it.familiaId == famDb.id }
                    .sortedWith(
                        compareByDescending<Persona> { it.esJefe }
                            .thenBy { it.nombre.lowercase() }
                    )
                famDb.nombreFamilia to miembros
            }
            .toMutableList()

        val sinFamilia = personas
            .filter { it.familiaId !in idsFamilias }
            .sortedWith(
                compareByDescending<Persona> { it.esJefe }
                    .thenBy { it.nombre.lowercase() }
            )

        if (sinFamilia.isNotEmpty()) {
            grupos.add("Sin familia" to sinFamilia)
        }

        grupos
    }
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

            onValueChange = {
                nombre = it
            },

            label = {
                Text("Nombre")
            },

            singleLine = true,

            shape = shapes.medium,

            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        // Email

        OutlinedTextField(

            value = email,

            onValueChange = {
                email = it
            },

            label = {
                Text("Email (opcional)")
            },

            singleLine = true,

            shape = shapes.medium,

            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        // Lada + celular

        Row(

            Modifier.fillMaxWidth(),

            verticalAlignment = Alignment.CenterVertically

        ) {

            OutlinedTextField(

                value = "${paisSeleccionado.bandera} ${paisSeleccionado.lada}",

                onValueChange = { },

                readOnly = true,

                trailingIcon = {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null
                    )
                },

                singleLine = true,

                shape = shapes.medium,

                modifier = Modifier
                    .width(100.dp)
                    .clickable {
                        ladaExpanded = true
                    }
            )

            DropdownMenu(

                expanded = ladaExpanded,

                onDismissRequest = {
                    ladaExpanded = false
                }

            ) {

                paises.forEach { p ->

                    DropdownMenuItem(

                        text = {
                            Text(
                                "${p.bandera} ${p.lada} ${p.nombre}"
                            )
                        },

                        onClick = {

                            paisSeleccionado = p
                            ladaExpanded = false
                        }
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            OutlinedTextField(

                value = celular,

                onValueChange = {

                    if (
                        it.length <= 10 &&
                        it.all { ch -> ch.isDigit() }
                    ) {
                        celular = it
                    }
                },

                label = {
                    Text("Celular (10 dígitos)")
                },

                isError = celularError &&
                        celular.isNotEmpty(),

                singleLine = true,

                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),

                shape = shapes.medium,

                modifier = Modifier.weight(1f)
            )
        }

        if (
            celularError &&
            celular.isNotEmpty()
        ) {

            Text(
                "El celular debe tener exactamente 10 dígitos.",
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(
                    start = 4.dp,
                    top = 2.dp
                )
            )
        }

        Spacer(Modifier.height(8.dp))

        // Familia

        ExposedDropdownMenuBox(

            expanded = familiaExpanded,

            onExpandedChange = {
                familiaExpanded = !familiaExpanded
            }

        ) {

            OutlinedTextField(

                value = familia,

                onValueChange = {
                    familia = it
                },

                label = {
                    Text("Familia")
                },

                singleLine = true,

                trailingIcon = {

                    ExposedDropdownMenuDefaults
                        .TrailingIcon(
                            expanded = familiaExpanded
                        )
                },

                shape = shapes.medium,

                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(

                expanded = familiaExpanded,

                onDismissRequest = {
                    familiaExpanded = false
                }

            ) {

                familiasNombres.forEach { fn ->

                    DropdownMenuItem(

                        text = {
                            Text(fn)
                        },

                        onClick = {

                            familia = fn
                            familiaExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Jefe familia

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Checkbox(

                checked = esJefe,

                onCheckedChange = {
                    esJefe = it
                }
            )

            Spacer(Modifier.width(4.dp))

            Text("¿Es jefe de familia?")
        }

        Spacer(Modifier.height(12.dp))

        // Guardar

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

                nombre = ""
                email = ""
                celular = ""
                familia = ""
                esJefe = false

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

        // Personas registradas

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Familias y personas",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )

            Text(
                "${personas.size} personas",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.primary
            )
        }

        Spacer(Modifier.height(8.dp))

        if (gruposFamilia.isEmpty()) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Aun no hay personas registradas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

        } else {

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                gruposFamilia.forEach { (nombreFamilia, miembros) ->

                    item(key = "familia-$nombreFamilia") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                nombreFamilia,
                                style = MaterialTheme.typography.titleMedium,
                                color = cs.primary,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                "${miembros.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }

                    if (miembros.isEmpty()) {
                        item(key = "vacia-$nombreFamilia") {
                            Text(
                                "Sin integrantes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                            )
                        }
                    }

                    items(
                        items = miembros,
                        key = { p -> "persona-${p.id}" }
                    ) { p ->

                        Card(

                            shape = shapes.small,

                            modifier = Modifier.fillMaxWidth(),

                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )

                        ) {

                            Row(

                                verticalAlignment =
                                    Alignment.CenterVertically,

                                modifier = Modifier
                                    .padding(10.dp)
                                    .fillMaxWidth()

                            ) {

                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (p.esJefe) cs.primary else MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(Modifier.width(8.dp))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {

                                    Text(

                                        text = p.nombre +
                                                if (p.esJefe)
                                                    " (Jefe)"
                                                else "",

                                        maxLines = 1,

                                        overflow =
                                            TextOverflow.Ellipsis
                                    )

                                    val contacto = listOf(p.celular, p.email)
                                        .filter { it.isNotBlank() }
                                        .joinToString("  |  ")

                                    if (contacto.isNotBlank()) {
                                        Text(
                                            contacto,
                                            style =
                                                MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                IconButton(

                                    onClick = {
                                        viewModel.eliminarPersona(p)
                                    }

                                ) {

                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Eliminar"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Modelo países

data class Pais(
    val lada: String,
    val nombre: String,
    val bandera: String
)