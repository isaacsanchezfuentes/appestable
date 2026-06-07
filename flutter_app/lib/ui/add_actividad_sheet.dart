import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/personas_provider.dart';
import '../services/auth_service.dart';

class AddActividadSheet extends StatefulWidget {
  const AddActividadSheet({super.key});

  @override
  State<AddActividadSheet> createState() => _AddActividadSheetState();
}

class _AddActividadSheetState extends State<AddActividadSheet> {
  final _nombre = TextEditingController();
  final _costo = TextEditingController();
  final List<int> _selectedIds = [];
  bool _isLoading = false;

  @override
  Widget build(BuildContext context) {
    final prov = context.watch<PersonasProvider>();
    final familias = prov.familias;
    final auth = context.read<AuthService>();

    final visibleFamilias = familias.entries.where((entry) {
      final familiaId = entry.value.first.familiaId;
      return familiaId == null || prov.canViewFamilia(familiaId);
    });

    return Padding(
      padding: EdgeInsets.only(
        bottom: MediaQuery.of(context).viewInsets.bottom,
        left: 16,
        right: 16,
        top: 20,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Text(
            'Registrar Actividad / Gasto',
            style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _nombre,
            decoration: const InputDecoration(
              labelText: '¿En qué se gastó?',
              border: OutlineInputBorder(),
              prefixIcon: Icon(Icons.shopping_cart),
            ),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _costo,
            keyboardType: TextInputType.number,
            decoration: const InputDecoration(
              labelText: 'Costo Total',
              border: OutlineInputBorder(),
              prefixText: '\$ ',
            ),
          ),
          const SizedBox(height: 20),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text('Participantes:', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
              TextButton.icon(
                icon: const Icon(Icons.star, size: 18),
                label: const Text('Solo Jefes'),
                onPressed: () {
                  setState(() {
                    _selectedIds.clear();
                    for (final p in prov.personas) {
                      if (p.esJefe && prov.canSelectParticipante(p)) {
                        _selectedIds.add(p.id!);
                      }
                    }
                  });
                },
              ),
            ],
          ),
          Flexible(
            child: ListView(
              shrinkWrap: true,
              children: visibleFamilias.map((entry) {
                final miembros =
                    entry.value.where((p) => prov.canSelectParticipante(p)).toList();
                if (miembros.isEmpty) return const SizedBox.shrink();

                final todosSeleccionados =
                    miembros.every((m) => _selectedIds.contains(m.id));

                return Card(
                  margin: const EdgeInsets.only(bottom: 12),
                  child: Column(
                    children: [
                      ListTile(
                        tileColor: Colors.blue.withValues(alpha: 0.05),
                        title: Text(
                          'Familia ${entry.key}',
                          style: const TextStyle(fontWeight: FontWeight.bold),
                        ),
                        trailing: TextButton(
                          child: Text(todosSeleccionados ? 'Quitar todos' : 'Todos'),
                          onPressed: () {
                            setState(() {
                              if (todosSeleccionados) {
                                for (final m in miembros) {
                                  _selectedIds.remove(m.id);
                                }
                              } else {
                                for (final m in miembros) {
                                  if (!_selectedIds.contains(m.id)) {
                                    _selectedIds.add(m.id!);
                                  }
                                }
                              }
                            });
                          },
                        ),
                      ),
                      ...miembros.map(
                        (p) => CheckboxListTile(
                          title: Text(p.nombre),
                          subtitle: Text(p.esJefe ? 'Jefe de Familia' : 'Integrante'),
                          value: _selectedIds.contains(p.id),
                          onChanged: (val) {
                            setState(() {
                              if (val == true) {
                                _selectedIds.add(p.id!);
                              } else {
                                _selectedIds.remove(p.id);
                              }
                            });
                          },
                        ),
                      ),
                    ],
                  ),
                );
              }).toList(),
            ),
          ),
          const SizedBox(height: 20),
          ElevatedButton(
            onPressed: _isLoading || _selectedIds.isEmpty
                ? null
                : () async {
                    setState(() => _isLoading = true);
                    String? err;
                    try {
                      err = await prov.addActividad(
                        _nombre.text.trim(),
                        double.tryParse(_costo.text) ?? 0,
                        _selectedIds,
                        auth.accessToken ?? '',
                      );
                      if (err == null && context.mounted) {
                        Navigator.pop(context);
                      } else if (context.mounted && err != null) {
                        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(err)));
                      }
                    } catch (e) {
                      if (context.mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Error inesperado: $e')));
                      }
                    } finally {
                      if (mounted) {
                        setState(() => _isLoading = false);
                      }
                    }
                  },
            style: ElevatedButton.styleFrom(
              padding: const EdgeInsets.all(16),
              backgroundColor: Colors.blue,
              foregroundColor: Colors.white,
            ),
            child: _isLoading
                ? const CircularProgressIndicator(color: Colors.white)
                : const Text('GUARDAR ACTIVIDAD'),
          ),
          const SizedBox(height: 20),
        ],
      ),
    );
  }
}