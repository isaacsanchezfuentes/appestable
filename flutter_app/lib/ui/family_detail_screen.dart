import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/resumen.dart';
import '../services/personas_provider.dart';
import '../services/auth_service.dart';

class FamilyDetailScreen extends StatelessWidget {
  final int familiaId;
  final String familiaNombre;

  const FamilyDetailScreen({
    super.key,
    required this.familiaId,
    required this.familiaNombre,
  });

  void _editAmount(
    BuildContext context,
    int partId,
    double currentAmount,
    String activityName,
    String personName,
  ) {
    final controller = TextEditingController(text: currentAmount.toStringAsFixed(2));
    final auth = context.read<AuthService>();
    final prov = context.read<PersonasProvider>();

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text('Editar Gasto: $activityName'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text('Asignado a: $personName', style: const TextStyle(color: Colors.grey)),
            const SizedBox(height: 16),
            TextField(
              controller: controller,
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              decoration: const InputDecoration(
                labelText: 'Monto (\$)',
                border: OutlineInputBorder(),
              ),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('CANCELAR')),
          ElevatedButton(
            onPressed: () async {
              final newAmount = double.tryParse(controller.text) ?? 0.0;
              final err = await prov.updateParticipacion(
                partId,
                costo: newAmount,
                token: auth.accessToken ?? '',
              );
              if (ctx.mounted) Navigator.pop(ctx);
              if (err != null && context.mounted) {
                ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(err)));
              }
            },
            child: const Text('GUARDAR'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final prov = context.watch<PersonasProvider>();
    final auth = context.read<AuthService>();
    final canEdit = prov.canEditParticipacion(familiaId);
    final resumenFam = prov.resumenFamilia(familiaId) ??
        prov.resumenFamiliaPorNombre(familiaNombre);
    final integrantes = prov.familias[familiaNombre] ?? [];

    return Scaffold(
      appBar: AppBar(
        title: Text('Detalle: $familiaNombre'),
        backgroundColor: Colors.blue.shade800,
        foregroundColor: Colors.white,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (!canEdit)
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(12),
                margin: const EdgeInsets.only(bottom: 16),
                decoration: BoxDecoration(
                  color: Colors.orange.shade50,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.orange.shade200),
                ),
                child: const Text(
                  'Modo solo lectura: tu rol no permite editar montos ni marcar pagos.',
                  style: TextStyle(fontSize: 13),
                ),
              ),
            const Text(
              'Gastos por Integrante',
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 12),
            ...integrantes.map((p) {
              final totalPersona = prov.calcularGastoPersona(p.id!);
              return Card(
                elevation: 0,
                color: Colors.blue.shade50,
                child: ListTile(
                  leading: const Icon(Icons.person, color: Colors.blue),
                  title: Text(p.nombre),
                  trailing: Text(
                    '\$${totalPersona.toStringAsFixed(2)}',
                    style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                  ),
                ),
              );
            }),
            const SizedBox(height: 32),
            const Text(
              'Desglose de Actividades',
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            Text(
              canEdit ? 'Toca un monto para modificarlo' : 'Consulta los gastos asignados',
              style: const TextStyle(color: Colors.grey, fontSize: 12),
            ),
            const SizedBox(height: 12),
            if (resumenFam == null || resumenFam.lineas.isEmpty)
              const Center(
                child: Padding(
                  padding: EdgeInsets.all(20),
                  child: Text('Sin gastos registrados.'),
                ),
              ),
            ..._buildLineas(context, prov, auth, resumenFam, canEdit),
            const SizedBox(height: 40),
          ],
        ),
      ),
      bottomNavigationBar: Container(
        padding: const EdgeInsets.all(20),
        color: Colors.grey.shade100,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text('TOTAL ASIGNADO:', style: TextStyle(fontWeight: FontWeight.bold)),
                Text(
                  '\$${(resumenFam?.totalAsignado ?? prov.calcularGastoFamilia(familiaNombre)).toStringAsFixed(2)}',
                  style: const TextStyle(
                    color: Colors.green,
                    fontWeight: FontWeight.bold,
                    fontSize: 20,
                  ),
                ),
              ],
            ),
            if (resumenFam != null) ...[
              const SizedBox(height: 4),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text('Pagado:', style: TextStyle(fontSize: 13)),
                  Text(
                    '\$${resumenFam.totalPagado.toStringAsFixed(2)}',
                    style: const TextStyle(fontSize: 13, color: Colors.green),
                  ),
                ],
              ),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text('Pendiente:', style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold)),
                  Text(
                    '\$${resumenFam.pendiente.toStringAsFixed(2)}',
                    style: TextStyle(
                      fontSize: 13,
                      fontWeight: FontWeight.bold,
                      color: resumenFam.pendiente > 0 ? Colors.red : Colors.green,
                    ),
                  ),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }

  List<Widget> _buildLineas(
    BuildContext context,
    PersonasProvider prov,
    AuthService auth,
    ResumenFamilia? resumenFam,
    bool canEdit,
  ) {
    if (resumenFam == null) return [];

    return resumenFam.lineas.map((linea) {
      final partId = prov.participacionId(linea.personaId, linea.actividadId);
      return Card(
        margin: const EdgeInsets.only(bottom: 12),
        color: linea.pagado ? Colors.green.shade50 : null,
        child: ListTile(
          leading: Icon(
            linea.pagado ? Icons.check_circle : Icons.receipt_long,
            color: linea.pagado ? Colors.green : Colors.grey,
          ),
          title: Text(linea.actividadNombre),
          subtitle: Text('Para: ${linea.personaNombre}'),
          trailing: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              if (canEdit && partId != null)
                Checkbox(
                  value: linea.pagado,
                  onChanged: (val) async {
                    final err = await prov.updateParticipacion(
                      partId,
                      pagado: val ?? false,
                      token: auth.accessToken ?? '',
                    );
                    if (err != null && context.mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(err)));
                    }
                  },
                ),
              TextButton(
                onPressed: canEdit && partId != null
                    ? () => _editAmount(
                          context,
                          partId,
                          linea.monto,
                          linea.actividadNombre,
                          linea.personaNombre,
                        )
                    : null,
                style: TextButton.styleFrom(backgroundColor: Colors.green.shade50),
                child: Text(
                  '\$${linea.monto.toStringAsFixed(2)}',
                  style: TextStyle(
                    fontWeight: FontWeight.bold,
                    color: linea.pagado ? Colors.grey : Colors.green,
                    fontSize: 16,
                    decoration: linea.pagado ? TextDecoration.lineThrough : null,
                  ),
                ),
              ),
            ],
          ),
        ),
      );
    }).toList();
  }
}