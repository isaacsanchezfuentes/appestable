package com.example.appestable.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking

@Composable
fun PantallaResumen(viewModel: PersonaViewModel) {
    val familias by viewModel.familiaList.collectAsState()
    val personas by viewModel.persona.collectAsState()

    val resumenPorPersona = runBlocking { viewModel.calcularResumenGastosPorPersona() }
    val resumenPorFamilia = runBlocking { viewModel.calcularResumenGastosPorFamilia() }

    val personasPorFamilia = personas.groupBy { it.familiaId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        familiesLoop@ for (familia in familias) {
            val miembros = personasPorFamilia[familia.id]?.toMutableList() ?: continue@familiesLoop
            miembros.sortByDescending { it.esJefe }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "👪 Familia: ${familia.nombreFamilia}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Divider()

                    miembros.forEach { persona ->
                        val gasto = resumenPorPersona
                            .find { it.nombre == persona.nombre && it.familia == familia.nombreFamilia }
                            ?.total ?: 0.0

                        val rol = if (persona.esJefe) " (Jefe)" else ""
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(
                                    if (persona.esJefe) Color(0xFFE3F2FD)
                                    else Color.Transparent
                                ),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "👤 ${persona.nombre}$rol",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "$${"%.2f".format(gasto)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    val totalFamilia = resumenPorFamilia.find { it.familiaId == familia.id }?.total ?: 0.0
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "TOTAL: $${"%.2f".format(totalFamilia)}",
                            color = Color(0xFF2E7D32),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .background(Color(0xFFD7FFD9))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        if (familias.isEmpty()) {
            Text("Aún no hay familias registradas.", color = Color.Gray)
        }
    }
}
