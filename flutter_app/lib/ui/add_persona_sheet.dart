import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/persona.dart';
import '../services/personas_provider.dart';
import '../services/auth_service.dart';

class AddPersonaSheet extends StatefulWidget {
  const AddPersonaSheet({super.key});

  @override
  State<AddPersonaSheet> createState() => _AddPersonaSheetState();
}

class _AddPersonaSheetState extends State<AddPersonaSheet> {
  final _nombre = TextEditingController();
  final _familia = TextEditingController();
  final _email = TextEditingController();
  final _celular = TextEditingController();
  bool _esJefe = false;
  bool _isLoading = false;

  @override
  Widget build(BuildContext context) {
    final prov = context.watch<PersonasProvider>();
    final auth = context.read<AuthService>();
    
    // Lista de familias únicas para sugerencias
    final familiasExistentes = prov.familias.keys.toList();

    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom, left: 20, right: 20, top: 20),
      child: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text('Nueva Persona', style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
            const SizedBox(height: 16),
            TextField(controller: _nombre, decoration: const InputDecoration(labelText: 'Nombre Completo', border: OutlineInputBorder(), prefixIcon: Icon(Icons.person))),
            const SizedBox(height: 12),
            
            // --- SUGERENCIA DE FAMILIA (Autocomplete) ---
            Autocomplete<String>(
              optionsBuilder: (TextEditingValue value) {
                if (value.text.isEmpty) return familiasExistentes;
                return familiasExistentes.where((f) => f.toLowerCase().contains(value.text.toLowerCase()));
              },
              onSelected: (String selection) => _familia.text = selection,
              fieldViewBuilder: (context, controller, focusNode, onFieldSubmitted) {
                if (_familia.text.isEmpty) _familia.text = controller.text;
                return TextField(
                  controller: controller,
                  focusNode: focusNode,
                  decoration: const InputDecoration(labelText: 'Familia', border: OutlineInputBorder(), prefixIcon: Icon(Icons.group)),
                  onChanged: (v) => _familia.text = v,
                );
              },
            ),

            const SizedBox(height: 12),
            TextField(controller: _email, decoration: const InputDecoration(labelText: 'Email (opcional)', border: OutlineInputBorder(), prefixIcon: Icon(Icons.email))),
            const SizedBox(height: 12),
            TextField(controller: _celular, keyboardType: TextInputType.phone, decoration: const InputDecoration(labelText: 'Celular', border: OutlineInputBorder(), prefixIcon: Icon(Icons.phone))),
            const SizedBox(height: 12),
            SwitchListTile(title: const Text('¿Es Jefe de Familia?'), value: _esJefe, onChanged: (v) => setState(() => _esJefe = v)),
            const SizedBox(height: 20),
            ElevatedButton(
              onPressed: _isLoading ? null : () async {
                if (_nombre.text.isEmpty || _familia.text.isEmpty) return;
                setState(() => _isLoading = true);
                
                final p = Persona(
                  nombre: _nombre.text.trim(),
                  familiaNombre: _familia.text.trim(),
                  esJefe: _esJefe,
                  email: _email.text.trim().isEmpty ? null : _email.text.trim(),
                  celular: _celular.text.trim().isEmpty ? null : _celular.text.trim(),
                );

                final err = await prov.addPersona(p, auth.accessToken ?? "", auth.isAdmin);
                setState(() => _isLoading = false);
                
                if (err == null) Navigator.pop(context);
                else ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(err)));
              },
              style: ElevatedButton.styleFrom(padding: const EdgeInsets.all(16), backgroundColor: Colors.blue, foregroundColor: Colors.white),
              child: _isLoading ? const CircularProgressIndicator(color: Colors.white) : const Text('GUARDAR REGISTRO'),
            ),
            const SizedBox(height: 20),
          ],
        ),
      ),
    );
  }
}
