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

    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom, left: 16, right: 16, top: 20),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Text('Registrar Actividad / Gasto', style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
          const SizedBox(height: 16),
          TextField(controller: _nombre, decoration: const InputDecoration(labelText: '¿En qué se gastó?', border: OutlineInputBorder(), prefixIcon: Icon(Icons.shopping_cart))),
          const SizedBox(height: 12),
          TextField(controller: _costo, keyboardType: TextInputType.number, decoration: const InputDecoration(labelText: 'Costo Total', border: OutlineInputBorder(), prefixText: '\$ ')),
          const SizedBox(height: 20),
          
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text("Participantes:", style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
              TextButton.icon(
                icon: const Icon(Icons.star, size: 18),
                label: const Text("Solo Jefes"),
                onPressed: () {
                  setState(() {
                    _selectedIds.clear();
                    for (var p in prov.personas) {
                      if (p.esJefe) _selectedIds.add(p.id!);
                    }
                  });
                },
              )
            ],
          ),

          // --- LISTA DE PARTICIPANTES POR FAMILIA (Igual que en Kotlin) ---
          Flexible(
            child: ListView(
              shrinkWrap: true,
              children: familias.entries.map((entry) {
                final miembros = entry.value;
                final todosSeleccionados = miembros.every((m) => _selectedIds.contains(m.id));

                return Card(
                  margin: const EdgeInsets.only(bottom: 12),
                  child: Column(
                    children: [
                      ListTile(
                        tileColor: Colors.blue.withOpacity(0.05),
                        title: Text("Familia ${entry.key}", style: const TextStyle(fontWeight: FontWeight.bold)),
                        trailing: TextButton(
                          child: Text(todosSeleccionados ? "Quitar todos" : "Todos"),
                          onPressed: () {
                            setState(() {
                              if (todosSeleccionados) {
                                for (var m in miembros) { _selectedIds.remove(m.id); }
                              } else {
                                for (var m in miembros) { if (!_selectedIds.contains(m.id)) _selectedIds.add(m.id!); }
                              }
                            });
                          },
                        ),
                      ),
                      ...miembros.map((p) => CheckboxListTile(
                        title: Text(p.nombre),
                        subtitle: Text(p.esJefe ? "Jefe de Familia" : "Integrante"),
                        value: _selectedIds.contains(p.id),
                        onChanged: (val) {
                          setState(() {
                            if (val == true) _selectedIds.add(p.id!);
                            else _selectedIds.remove(p.id);
                          });
                        },
                      )),
                    ],
                  ),
                );
              }).toList(),
            ),
          ),

          const SizedBox(height: 20),
          ElevatedButton(
            onPressed: _isLoading || _selectedIds.isEmpty ? null : () async {
              setState(() => _isLoading = true);
              final err = await prov.addActividad(
                _nombre.text.trim(), 
                double.tryParse(_costo.text) ?? 0, 
                _selectedIds, 
                auth.accessToken ?? ""
              );
              setState(() => _isLoading = false);
              if (err == null) Navigator.pop(context);
              else ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(err)));
            },
            style: ElevatedButton.styleFrom(padding: const EdgeInsets.all(16), backgroundColor: Colors.blue, foregroundColor: Colors.white),
            child: _isLoading ? const CircularProgressIndicator(color: Colors.white) : const Text('GUARDAR ACTIVIDAD'),
          ),
          const SizedBox(height: 20),
        ],
      ),
    );
  }
}
