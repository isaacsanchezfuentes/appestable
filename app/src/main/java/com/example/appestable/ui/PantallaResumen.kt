package com.example.appestable.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.appestable.data.Actividad
import com.example.appestable.data.Participacion
import com.example.appestable.data.Persona

@Composable
fun PantallaResumen(viewModel: PersonaViewModel) {
    val familias by viewModel.familiaList.collectAsState()
    val personas by viewModel.persona.collectAsState()
    val actividades by viewModel.actividades.collectAsState()
    val participaciones: List<Participacion> by viewModel.participaciones.collectAsState()

    var familiaExpandidaId by remember { mutableStateOf<Int?>(null) }
    val montosEditados = remember { mutableStateMapOf<String, String>() }

    val personasPorFamilia = personas.groupBy { it.familiaId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        familias.forEach { familia ->
            val miembros = personasPorFamilia[familia.id]
                .orEmpty()
                .sortedWith(
                    compareByDescending<Persona> { it.esJefe }
                        .thenBy { it.nombre.lowercase() }
                )

            val participacionesFamilia: List<Participacion> = participaciones.filter { part: Participacion ->
                miembros.any { it.id == part.personaId }
            }

            val totalFamilia = participacionesFamilia.sumOf { part: Participacion ->
                val actividad = actividades.find { it.id == part.actividadId }
                if (actividad != null) viewModel.montoAsignado(part, actividad) else 0.0
            }

            val estaExpandida = familiaExpandidaId == familia.id

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        familiaExpandidaId =
                            if (estaExpandida) null else familia.id
                    },
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = familia.nombreFamilia,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${miembros.size} integrantes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "$${"%.2f".format(totalFamilia)}",
                            color = Color(0xFF2E7D32),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .background(Color(0xFFD7FFD9))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    if (estaExpandida) {
                        Spacer(Modifier.height(12.dp))
                        Divider()
                        Spacer(Modifier.height(8.dp))

                        if (participacionesFamilia.isEmpty()) {
                            Text(
                                text = "Sin actividades asignadas.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val desglose = participacionesFamilia
                                .mapNotNull { part: Participacion ->
                                    val persona = miembros.find { it.id == part.personaId }
                                    val actividad = actividades.find { it.id == part.actividadId }
                                    if (persona != null && actividad != null) {
                                        Triple(persona, actividad, part)
                                    } else {
                                        null
                                    }
                                }
                                .sortedWith(
                                    compareBy<Triple<Persona, Actividad, Participacion>> {
                                        it.second.fecha
                                    }.thenBy { it.second.nombre.lowercase() }
                                        .thenBy { it.first.nombre.lowercase() }
                                )

                            desglose.forEach { item: Triple<Persona, Actividad, Participacion> ->
                                val persona = item.first
                                val actividad = item.second
                                val part = item.third
                                val key = "${part.personaId}-${part.actividadId}"
                                val montoActual = viewModel.montoAsignado(part, actividad)
                                val textoMonto = montosEditados[key] ?: "%.2f".format(montoActual)

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = MaterialTheme.shapes.small,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = actividad.nombre,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${persona.nombre}${if (persona.esJefe) " (Jefe)" else ""}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Text(
                                                text = if (actividad.fecha.isBlank()) "" else actividad.fecha,
                                                style = MaterialTheme.typography.bodySmall,
                                                textAlign = TextAlign.End,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Spacer(Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = textoMonto,
                                                onValueChange = { nuevo ->
                                                    if (nuevo.all { it.isDigit() || it == '.' }) {
                                                        montosEditados[key] = nuevo
                                                    }
                                                },
                                                label = { Text("Monto") },
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = KeyboardType.Decimal
                                                ),
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )

                                            Button(
                                                onClick = {
                                                    val monto = textoMonto.toDoubleOrNull()
                                                    if (monto != null) {
                                                        viewModel.actualizarMontoParticipacion(
                                                            part.personaId,
                                                            part.actividadId,
                                                            monto
                                                        )
                                                        montosEditados.remove(key)
                                                    }
                                                },
                                                modifier = Modifier.padding(top = 8.dp)
                                            ) {
                                                Text("Guardar")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (familias.isEmpty()) {
            Text("Aun no hay familias registradas.", color = Color.Gray)
        }
    }
}