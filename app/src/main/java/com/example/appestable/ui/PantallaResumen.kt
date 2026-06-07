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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.appestable.domain.ResumenFamiliaDetalle

@Composable
fun PantallaResumen(viewModel: PersonaViewModel) {
    val resumenes by viewModel.resumenesFamilia.collectAsState()
    val resumenGlobal by viewModel.resumenGlobal.collectAsState()

    var familiaExpandidaId by remember { mutableStateOf<Int?>(null) }
    val montosEditados = remember { mutableStateMapOf<String, String>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        Text(
            text = "Balance Consolidado",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Toca una familia para ver el desglose detallado",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        if (viewModel.canViewResumenGlobal() && resumenGlobal != null) {
            val global = resumenGlobal!!
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Resumen del viaje", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    MetricRow("Costo total", global.costoTotalViaje, Color(0xFF1565C0))
                    MetricRow("Pagado", global.totalPagado, Color(0xFF2E7D32))
                    MetricRow("Pendiente", global.totalPendiente, Color(0xFFC62828))

                    if (global.familiasRanking.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Divider()
                        Spacer(Modifier.height(8.dp))
                        Text("Ranking por familia", style = MaterialTheme.typography.labelLarge)
                        global.familiasRanking.forEachIndexed { index, (nombre, total) ->
                            Text("${index + 1}. $nombre — $${"%.2f".format(total)}")
                        }
                    }

                    if (global.actividadesConFaltante.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Divider()
                        Spacer(Modifier.height(8.dp))
                        Text("Actividades con faltante", style = MaterialTheme.typography.labelLarge, color = Color(0xFFC62828))
                        global.actividadesConFaltante.forEach { act ->
                            Text(
                                "${act.nombre}: faltan $${"%.2f".format(act.faltante)} de $${"%.2f".format(act.costoTotal)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        resumenes.forEach { resumen ->
            FamiliaResumenCard(
                resumen = resumen,
                expandida = familiaExpandidaId == resumen.familiaId,
                onToggle = {
                    familiaExpandidaId = if (familiaExpandidaId == resumen.familiaId) null else resumen.familiaId
                },
                canEdit = viewModel.canEditParticipacion(resumen.familiaId),
                montosEditados = montosEditados,
                onGuardarMonto = { personaId, actividadId, monto ->
                    viewModel.actualizarMontoParticipacion(personaId, actividadId, monto)
                    montosEditados.remove("$personaId-$actividadId")
                },
                onTogglePagado = { personaId, actividadId, pagado ->
                    viewModel.actualizarPagadoParticipacion(personaId, actividadId, pagado)
                }
            )
        }

        if (resumenes.isEmpty()) {
            Text(
                "No hay datos para mostrar en este viaje.",
                color = Color.Gray,
                modifier = Modifier.padding(top = 24.dp)
            )
        }
    }
}

@Composable
private fun MetricRow(label: String, value: Double, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(
            "$${"%.2f".format(value)}",
            color = color,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@Composable
private fun FamiliaResumenCard(
    resumen: ResumenFamiliaDetalle,
    expandida: Boolean,
    onToggle: () -> Unit,
    canEdit: Boolean,
    montosEditados: MutableMap<String, String>,
    onGuardarMonto: (Int, Int, Double) -> Unit,
    onTogglePagado: (Int, Int, Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onToggle() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = resumen.nombreFamilia,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${resumen.integrantes} integrantes · ${resumen.actividadesCount} actividades",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$${"%.2f".format(resumen.totalAsignado)}",
                        color = Color(0xFF2E7D32),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .background(Color(0xFFD7FFD9))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                    Text(
                        text = "Pend: $${"%.2f".format(resumen.pendiente)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (resumen.pendiente > 0) Color(0xFFC62828) else Color(0xFF2E7D32)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Pagado: $${"%.2f".format(resumen.totalPagado)}", style = MaterialTheme.typography.bodySmall)
                Text(
                    if (expandida) "Ocultar detalle" else "Ver detalle",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (expandida) {
                Spacer(Modifier.height(12.dp))
                Divider()
                Spacer(Modifier.height(8.dp))

                if (resumen.lineas.isEmpty()) {
                    Text(
                        "Sin actividades asignadas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    resumen.lineas.forEach { linea ->
                        val key = "${linea.personaId}-${linea.actividadId}"
                        val textoMonto = montosEditados[key] ?: "%.2f".format(linea.monto)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = MaterialTheme.shapes.small,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            linea.actividadNombre,
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "${linea.personaNombre}${if (linea.esJefe) " (Jefe)" else ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        if (linea.actividadFecha.isBlank()) "" else linea.actividadFecha,
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.End
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                if (canEdit) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = textoMonto,
                                            onValueChange = { nuevo ->
                                                if (nuevo.all { it.isDigit() || it == '.' }) {
                                                    montosEditados[key] = nuevo
                                                }
                                            },
                                            label = { Text("Monto") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Button(
                                            onClick = {
                                                textoMonto.toDoubleOrNull()?.let { monto ->
                                                    onGuardarMonto(linea.personaId, linea.actividadId, monto)
                                                }
                                            },
                                            modifier = Modifier.padding(top = 8.dp)
                                        ) { Text("Guardar") }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Pagado", style = MaterialTheme.typography.bodySmall)
                                        Spacer(Modifier.width(8.dp))
                                        Switch(
                                            checked = linea.pagado,
                                            onCheckedChange = { onTogglePagado(linea.personaId, linea.actividadId, it) }
                                        )
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("$${"%.2f".format(linea.monto)}", style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            if (linea.pagado) "✅ Pagado" else "⏳ Pendiente",
                                            color = if (linea.pagado) Color(0xFF2E7D32) else Color(0xFFC62828),
                                            style = MaterialTheme.typography.bodySmall
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
}