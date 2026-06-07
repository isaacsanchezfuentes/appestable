import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/persona.dart';
import '../models/rol_viaje.dart';
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
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final prov = context.read<PersonasProvider>();
      if (prov.session.rol == RolViaje.jefeFamilia) {
        final familiaNombre = prov.personas
            .where((p) => p.familiaId == prov.session.familiaId)
            .map((p) => p.familiaNombre)
            .firstOrNull;
        if (familiaNombre != null) _familia.text = familiaNombre;
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final prov = context.watch<PersonasProvider>();
    final auth = context.read<AuthService>();
    final session = prov.session;
    final isJefe = session.rol == RolViaje.jefeFamilia;
    final isOrganizador = session.rol == RolViaje.organizador;
    final familiasExistentes = prov.familias.keys.toList();
    final familiaLocked = isJefe && session.familiaId != null;

    return Padding(
      padding: EdgeInsets.only(
        bottom: MediaQuery.of(context).viewInsets.bottom,
        left: 20,
        right: 20,
        top: 20,
      ),
      child: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text(
              'Nueva Persona',
              style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _nombre,
              decoration: const InputDecoration(
                labelText: 'Nombre Completo',
                border: OutlineInputBorder(),
                prefixIcon: Icon(Icons.person),
              ),
            ),
            const SizedBox(height: 12),
            if (familiaLocked)
              TextField(
                controller: _familia,
                readOnly: true,
                decoration: const InputDecoration(
                  labelText: 'Familia',
                  border: OutlineInputBorder(),
                  prefixIcon: Icon(Icons.group),
                ),
              )
            else
              Autocomplete<String>(
                optionsBuilder: (value) {
                  if (value.text.isEmpty) return familiasExistentes;
                  return familiasExistentes
                      .where((f) => f.toLowerCase().contains(value.text.toLowerCase()));
                },
                onSelected: (selection) => _familia.text = selection,
                fieldViewBuilder: (context, controller, focusNode, onFieldSubmitted) {
                  return TextField(
                    controller: controller,
                    focusNode: focusNode,
                    decoration: const InputDecoration(
                      labelText: 'Familia',
                      border: OutlineInputBorder(),
                      prefixIcon: Icon(Icons.group),
                    ),
                    onChanged: (v) => _familia.text = v,
                  );
                },
              ),
            const SizedBox(height: 12),
            TextField(
              controller: _email,
              decoration: const InputDecoration(
                labelText: 'Email (opcional)',
                border: OutlineInputBorder(),
                prefixIcon: Icon(Icons.email),
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _celular,
              keyboardType: TextInputType.phone,
              decoration: const InputDecoration(
                labelText: 'Celular',
                border: OutlineInputBorder(),
                prefixIcon: Icon(Icons.phone),
              ),
            ),
            if (isOrganizador) ...[
              const SizedBox(height: 12),
              SwitchListTile(
                title: const Text('¿Es Jefe de Familia?'),
                value: _esJefe,
                onChanged: (v) => setState(() => _esJefe = v),
              ),
            ],
            const SizedBox(height: 20),
            ElevatedButton(
              onPressed: _isLoading
                  ? null
                  : () async {
                      if (_nombre.text.isEmpty || _familia.text.isEmpty) return;

                      final rawFamiliaId = prov.personas
                          .where((p) => p.familiaNombre == _familia.text.trim())
                          .map((p) => p.familiaId)
                          .firstWhere((id) => id != null, orElse: () => session.familiaId);
                      final familiaId = rawFamiliaId ?? session.familiaId;

                      // Solo bloqueamos temprano si un JEFE con familia bloqueada intenta agregar fuera de la suya.
                      // Para MIEMBRO dejamos pasar: el addPersona hará "ensure-membership" para ascenderlo a ORGANIZADOR.
                      final isJefeLockedToOtherFamily = isJefe &&
                          familiaId != null &&
                          session.familiaId != null &&
                          familiaId != session.familiaId &&
                          !prov.canAddPersona(familiaId!);

                      if (isJefeLockedToOtherFamily) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(content: Text('Solo puedes agregar personas a tu propia familia')),
                        );
                        return;
                      }

                      setState(() => _isLoading = true);
                      String? err;
                      try {
                        final p = Persona(
                          nombre: _nombre.text.trim(),
                          familiaNombre: _familia.text.trim(),
                          esJefe: isOrganizador && _esJefe,
                          email: _email.text.trim().isEmpty ? null : _email.text.trim(),
                          celular: _celular.text.trim().isEmpty ? null : _celular.text.trim(),
                        );

                        err = await prov.addPersona(p, auth.accessToken ?? '');
                        if (err == null && context.mounted) {
                          Navigator.pop(context);
                        } else if (context.mounted) {
                          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(err!)));
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
                  : const Text('GUARDAR REGISTRO'),
            ),
            const SizedBox(height: 20),
          ],
        ),
      ),
    );
  }
}

extension _FirstOrNull<E> on Iterable<E> {
  E? get firstOrNull {
    final it = iterator;
    if (!it.moveNext()) return null;
    return it.current;
  }
}