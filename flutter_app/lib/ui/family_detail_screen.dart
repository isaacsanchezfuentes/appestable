import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/personas_provider.dart';
import '../services/auth_service.dart';

class FamilyDetailScreen extends StatelessWidget {
  final String familiaNombre;

  const FamilyDetailScreen({super.key, required this.familiaNombre});

  @override
  Widget build(BuildContext context) {
    final prov = context.watch<PersonasProvider>();
    final auth = context.read<AuthService>();
    
    // Obtener IDs de los integrantes de esta familia
    final integrantes = prov.familias[familiaNombre] ?? [];
    final integrantesIds = integrantes.map((p) => p.id).toList();

    // Filtrar participaciones que pertenecen a esta familia
    final participacionesFamilia = prov.participaciones
        .where((part) => integrantesIds.contains(part.personaId))
        .toList();

    return Scaffold(
      appBar: AppBar(
        title: Text("Detalle: $familiaNombre"),
        backgroundColor: Colors.blue.shade800,
        foregroundColor: Colors.white,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text("Resumen por Integrante", style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
            const SizedBox(height: 12),
            ...integrantes.map((p) {
              final totalPersona = prov.calcularGastoPersona(p.id!);
              return Card(
                elevation: 0,
                color: Colors.blue.shade50,
                child: ListTile(
                  leading: const Icon(Icons.person, color: Colors.blue),
                  title: Text(p.nombre),
                  trailing: Text("\$${totalPersona.toStringAsFixed(2)}", 
                    style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                ),
              );
            }),
            
            const SizedBox(height: 32),
            const Text("Desglose de Actividades", style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            const Text("Marca los gastos pagados", style: TextStyle(color: Colors.grey, fontSize: 12)),
            const SizedBox(height: 12),

            if (participacionesFamilia.isEmpty)
              const Center(child: Padding(
                padding: EdgeInsets.all(20.0),
                child: Text("Sin gastos registrados aún."),
              )),

            ...participacionesFamilia.map((part) {
              // Buscar el nombre de la actividad y de la persona
              final actividad = prov.actividades.firstWhere((a) => a.id == part.actividadId, orElse: () => prov.actividades.first);
              final persona = integrantes.firstWhere((p) => p.id == part.personaId, orElse: () => integrantes.first);

              return Card(
                margin: const EdgeInsets.only(bottom: 12),
                child: SwitchListTile(
                  secondary: const Icon(Icons.receipt_long, color: Colors.green),
                  title: Text(actividad.nombre),
                  subtitle: Text("Para: ${persona.nombre}"),
                  value: part.pagado,
                  onChanged: (bool value) async {
                    final err = await prov.updatePago(part.id!, value, auth.accessToken ?? "");
                    if (err != null) {
                      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(err)));
                    }
                  },
                  activeColor: Colors.green,
                  controlAffinity: ListTileControlAffinity.trailing,
                ),
              );
            }),
            const SizedBox(height: 40),
          ],
        ),
      ),
      bottomNavigationBar: Container(
        padding: const EdgeInsets.all(20),
        color: Colors.grey.shade100,
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text("TOTAL FAMILIA:", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
            Text("\$${prov.calcularGastoFamilia(familiaNombre).toStringAsFixed(2)}", 
              style: const TextStyle(color: Colors.green, fontWeight: FontWeight.bold, fontSize: 22)),
          ],
        ),
      ),
    );
  }
}
