package com.example.appestable.ui

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.*

@Composable
fun PantallaActividades(viewModel: PersonaViewModel) {
    val personas by viewModel.persona.collectAsState()
    val actividades by viewModel.actividades.collectAsState()
    val familias by viewModel.familiaList.collectAsState()

    var nombreActividad by remember { mutableStateOf("") }
    var costoTotal by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf("") }
    val seleccionados = remember { mutableStateMapOf<Int, Boolean>() }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    fun abrirDatePicker() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        DatePickerDialog(context, { _: DatePicker, y: Int, m: Int, d: Int ->
            fecha = "%02d/%02d/%04d".format(d, m + 1, y)
        }, year, month, day).show()
    }

    fun montoValido(valor: String): Double? =
        valor.replace(",", ".").toDoubleOrNull()

    fun textoMontoValido(valor: String): Boolean {
        val normalizado = valor.replace(",", ".")
        return normalizado.count { it == '.' } <= 1 &&
                normalizado.all { it.isDigit() || it == '.' }
    }

    val participantesSeleccionados = personas.filter { seleccionados[it.id] == true }
    val costoActividad = montoValido(costoTotal)
    val montoPorParticipante =
        if (costoActividad != null && participantesSeleccionados.isNotEmpty())
            costoActividad / participantesSeleccionados.size
        else
            0.0

    val personasPorFamilia = personas.groupBy { it.familiaId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Card(modifier = Modifier.fillMaxWidth(), shape = shapes.medium) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Registrar Actividad", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = nombreActividad,
                    onValueChange = { nombreActividad = it },
                    label = { Text("Nombre de la actividad") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = shapes.medium
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = costoTotal,
                    onValueChange = {
                        if (it.isBlank() || textoMontoValido(it)) {
                            costoTotal = it
                        }
                    },
                    label = { Text("Costo total") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = shapes.medium
                )
                Spacer(Modifier.height(8.dp))

                OutlinedButton(onClick = { abrirDatePicker() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Event, contentDescription = "Seleccionar fecha")
                    Spacer(Modifier.width(8.dp))
                    Text(if (fecha.isEmpty()) "Seleccionar fecha (opcional)" else fecha)
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        personas.filter { it.esJefe }.forEach {
                            seleccionados[it.id] = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = shapes.medium
                ) {
                    Text("Un solo pago por Familia")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("Participantes por familia:", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        familias.forEach { familia ->
            val miembros = personasPorFamilia[familia.id] ?: return@forEach
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = shapes.medium) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "👪 ${familia.nombreFamilia}",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(onClick = {
                            val todosSeleccionados = miembros.all { seleccionados[it.id] == true }
                            miembros.forEach {
                                seleccionados[it.id] = !todosSeleccionados
                            }
                        }) {
                            Text("Seleccionar todos")
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    miembros.sortedByDescending { it.esJefe }.forEach { persona ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val rol = if (persona.esJefe) " (Jefe)" else ""
                            Text(
                                "${persona.nombre} (${persona.email})$rol",
                                modifier = Modifier.weight(1f)
                            )
                            Checkbox(
                                checked = seleccionados[persona.id] ?: false,
                                onCheckedChange = { seleccionados[persona.id] = it }
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = if (participantesSeleccionados.isEmpty())
                "Selecciona al menos una persona."
            else
                "Seleccionados: ${participantesSeleccionados.size} | Monto por persona: $${"%.2f".format(montoPorParticipante)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                val participantes = participantesSeleccionados
                val costo = costoActividad
                if (nombreActividad.isNotBlank() && costo != null && participantes.isNotEmpty()) {
                    viewModel.agregarActividad(
                        nombreActividad,
                        fecha,
                        costo,
                        participantes
                    )
                    nombreActividad = ""
                    costoTotal = ""
                    fecha = ""
                    seleccionados.clear()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = nombreActividad.isNotBlank() && costoActividad != null && participantesSeleccionados.isNotEmpty(),
            shape = shapes.medium
        ) {
            Text("Guardar actividad")
        }

        Spacer(Modifier.height(16.dp))
        Divider()
        Spacer(Modifier.height(8.dp))

        Text("Actividades registradas:", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))

        actividades.forEach { actividad ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = shapes.medium
            ) {
                Text(
                    text = "🔹 ${actividad.nombre}: \$${actividad.costoTotal} (${actividad.fecha})",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
