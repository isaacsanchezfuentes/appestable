import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';
import 'models/resumen.dart';
import 'models/rol_viaje.dart';
import 'services/api_service.dart';
import 'services/auth_service.dart';
import 'services/personas_provider.dart';
import 'ui/add_persona_sheet.dart';
import 'ui/add_actividad_sheet.dart';
import 'ui/family_detail_screen.dart';

void main() {
  runApp(
    MultiProvider(
      providers: [
        Provider(create: (_) => ApiService()),
        ChangeNotifierProvider(create: (_) => AuthService()),
        ChangeNotifierProvider(create: (_) => PersonasProvider()),
      ],
      child: const MyApp(),
    ),
  );
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'AppEstable Pro',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF1E88E5)),
        textTheme: GoogleFonts.lexendTextTheme(),
      ),
      home: const MainNavigationScreen(),
    );
  }
}

class MainNavigationScreen extends StatefulWidget {
  const MainNavigationScreen({super.key});

  @override
  State<MainNavigationScreen> createState() => _MainNavigationScreenState();
}

class _MainNavigationScreenState extends State<MainNavigationScreen> {
  int _selectedIndex = 0;
  String? _lastToken;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _syncAuth());
  }

  Future<void> _syncAuth() async {
    final auth = context.read<AuthService>();
    final prov = context.read<PersonasProvider>();
    final token = auth.accessToken;

    if (auth.isLoggedIn && token != null) {
      if (token != _lastToken || !prov.hasSession) {
        _lastToken = token;
        await prov.initializeSession(token);
      }
    } else {
      _lastToken = null;
      prov.clearSession();
    }
  }

  Future<void> _handleLogin() async {
    final auth = context.read<AuthService>();
    final ok = await auth.login();
    if (ok && mounted) await _syncAuth();
  }

  Future<void> _handleLogout() async {
    final auth = context.read<AuthService>();
    await auth.logout();
    if (mounted) await _syncAuth();
  }

  Future<void> _showCreateViajeDialog() async {
    final controller = TextEditingController();
    final prov = context.read<PersonasProvider>();
    final auth = context.read<AuthService>();

    final nombre = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Nuevo viaje'),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(
            labelText: 'Nombre del viaje',
            border: OutlineInputBorder(),
          ),
          autofocus: true,
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('CANCELAR')),
          ElevatedButton(
            onPressed: () => Navigator.pop(ctx, controller.text.trim()),
            child: const Text('CREAR'),
          ),
        ],
      ),
    );

    if (nombre == null || nombre.isEmpty) return;
    final err = await prov.createViaje(nombre);
    if (err != null && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(err)));
    } else if (auth.accessToken != null) {
      await prov.refresh();
    }
  }

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthService>();
    final prov = context.watch<PersonasProvider>();

    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              _selectedIndex == 0
                  ? 'Gestión de Personas'
                  : _selectedIndex == 1
                      ? 'Historial de Gastos'
                      : 'Balance por Familia',
              style: const TextStyle(fontSize: 16),
            ),
            if (prov.viajeActivo != null)
              Text(
                prov.viajeActivo!.nombre,
                style: const TextStyle(fontSize: 12, fontWeight: FontWeight.normal),
              ),
          ],
        ),
        actions: [
          if (prov.hasSession)
            PopupMenuButton<int>(
              tooltip: 'Cambiar viaje',
              onSelected: (id) => prov.selectViaje(id),
              itemBuilder: (ctx) => prov.viajes
                  .map(
                    (v) => PopupMenuItem(
                      value: v.id,
                      child: Row(
                        children: [
                          if (v.id == prov.viajeActivo?.id)
                            const Icon(Icons.check, size: 18)
                          else
                            const SizedBox(width: 18),
                          const SizedBox(width: 8),
                          Expanded(child: Text(v.nombre)),
                        ],
                      ),
                    ),
                  )
                  .toList(),
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 8),
                child: Row(
                  children: [
                    const Icon(Icons.flight_takeoff, size: 20),
                    const SizedBox(width: 4),
                    Text(prov.viajeActivo?.nombre ?? 'Viaje', style: const TextStyle(fontSize: 13)),
                    const Icon(Icons.arrow_drop_down),
                  ],
                ),
              ),
            ),
          if (prov.hasSession)
            Padding(
              padding: const EdgeInsets.only(right: 4),
              child: Chip(
                label: Text(prov.session.rol.label, style: const TextStyle(fontSize: 11)),
                backgroundColor: Colors.blue.shade100,
                side: BorderSide.none,
              ),
            ),
          if (auth.isLoggedIn && prov.canCreateViaje())
            IconButton(
              icon: const Icon(Icons.add_location_alt),
              tooltip: prov.needsFirstViaje ? 'Crear primer viaje' : 'Nuevo viaje',
              onPressed: _showCreateViajeDialog,
            ),
          if (!auth.isLoggedIn)
            IconButton(icon: const Icon(Icons.login), onPressed: _handleLogin)
          else
            IconButton(icon: const Icon(Icons.logout), onPressed: _handleLogout),
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: auth.isLoggedIn ? prov.refresh : null,
          ),
        ],
      ),
      body: !auth.isLoggedIn
          ? _LoginPrompt(onLogin: _handleLogin)
          : prov.needsFirstViaje
              ? _FirstViajePrompt(onCreate: _showCreateViajeDialog)
              : prov.error != null && !prov.hasSession
                  ? _ErrorState(message: prov.error!, onRetry: _syncAuth)
                  : IndexedStack(
                  index: _selectedIndex,
                  children: const [
                    PersonasScreen(),
                    ActividadesScreen(),
                    ResumenScreen(),
                  ],
                ),
      bottomNavigationBar: auth.isLoggedIn
          ? NavigationBar(
              selectedIndex: _selectedIndex,
              onDestinationSelected: (index) => setState(() => _selectedIndex = index),
              destinations: const [
                NavigationDestination(icon: Icon(Icons.group), label: 'Personas'),
                NavigationDestination(icon: Icon(Icons.list_alt), label: 'Actividades'),
                NavigationDestination(icon: Icon(Icons.analytics), label: 'Resumen'),
              ],
            )
          : null,
    );
  }
}

class _LoginPrompt extends StatelessWidget {
  final VoidCallback onLogin;

  const _LoginPrompt({required this.onLogin});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.lock_outline, size: 64, color: Colors.blue.shade300),
            const SizedBox(height: 16),
            const Text(
              'Inicia sesión para acceder a tus viajes',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            const Text(
              'La app requiere autenticación para sincronizar personas, gastos y resúmenes.',
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.grey),
            ),
            const SizedBox(height: 24),
            ElevatedButton.icon(
              onPressed: onLogin,
              icon: const Icon(Icons.login),
              label: const Text('INICIAR SESIÓN'),
            ),
          ],
        ),
      ),
    );
  }
}

class _FirstViajePrompt extends StatelessWidget {
  final VoidCallback onCreate;

  const _FirstViajePrompt({required this.onCreate});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.flight_takeoff, size: 64, color: Colors.blue.shade300),
            const SizedBox(height: 16),
            const Text(
              'Crea tu primer viaje',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            const Text(
              'Para registrar familias, personas y gastos necesitas un viaje activo. '
              'Serás organizador y podrás invitar a jefes de familia después.',
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.grey),
            ),
            const SizedBox(height: 24),
            ElevatedButton.icon(
              onPressed: onCreate,
              icon: const Icon(Icons.add_location_alt),
              label: const Text('CREAR MI PRIMER VIAJE'),
            ),
          ],
        ),
      ),
    );
  }
}

class _ErrorState extends StatelessWidget {
  final String message;
  final VoidCallback onRetry;

  const _ErrorState({required this.message, required this.onRetry});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, size: 48, color: Colors.red),
            const SizedBox(height: 12),
            Text(message, textAlign: TextAlign.center),
            const SizedBox(height: 16),
            ElevatedButton(onPressed: onRetry, child: const Text('REINTENTAR')),
          ],
        ),
      ),
    );
  }
}

class PersonasScreen extends StatelessWidget {
  const PersonasScreen({super.key});

  Future<void> _confirmDelete(
    BuildContext context,
    String name,
    VoidCallback onDelete,
  ) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Icon(Icons.delete_forever, color: Colors.red, size: 50),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text(
              '¿Eliminar este registro?',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
            ),
            const SizedBox(height: 8),
            Text(
              'Estás a punto de borrar a $name. Esta acción no se puede deshacer.',
              textAlign: TextAlign.center,
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('CANCELAR')),
          ElevatedButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: ElevatedButton.styleFrom(backgroundColor: Colors.red, foregroundColor: Colors.white),
            child: const Text('ELIMINAR'),
          ),
        ],
      ),
    );
    if (confirm == true) onDelete();
  }

  @override
  Widget build(BuildContext context) {
    final prov = context.watch<PersonasProvider>();
    final auth = context.read<AuthService>();
    final familias = prov.familias;
    final canAddAny = prov.session.rol == RolViaje.organizador ||
        prov.session.rol == RolViaje.jefeFamilia;

    return Scaffold(
      body: RefreshIndicator(
        onRefresh: prov.refresh,
        child: prov.isLoading && prov.personas.isEmpty
            ? const Center(child: CircularProgressIndicator())
            : familias.isEmpty
                ? const Center(child: Text('Sin registros. Desliza para actualizar.'))
                : ListView(
                    padding: const EdgeInsets.all(12),
                    children: familias.entries.map((entry) {
                      return Card(
                        margin: const EdgeInsets.only(bottom: 20),
                        elevation: 4,
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
                        child: Column(
                          children: [
                            Container(
                              padding: const EdgeInsets.all(15),
                              width: double.infinity,
                              decoration: BoxDecoration(
                                color: Colors.blue.shade700,
                                borderRadius: const BorderRadius.vertical(top: Radius.circular(15)),
                              ),
                              child: Text(
                                '👪 Familia ${entry.key}',
                                style: const TextStyle(
                                  fontWeight: FontWeight.bold,
                                  color: Colors.white,
                                  fontSize: 18,
                                ),
                              ),
                            ),
                            ...entry.value.map((p) {
                              final canDelete = prov.canDeletePersona(p);
                              return ListTile(
                                onLongPress: canDelete
                                    ? () => _confirmDelete(context, p.nombre, () async {
                                          final err = await prov.removePersona(
                                            p.id!,
                                            auth.accessToken ?? '',
                                          );
                                          if (err != null && context.mounted) {
                                            ScaffoldMessenger.of(context)
                                                .showSnackBar(SnackBar(content: Text(err)));
                                          }
                                        })
                                    : null,
                                leading: CircleAvatar(
                                  backgroundColor: p.esJefe ? Colors.amber : Colors.blue.shade100,
                                  child: Text(p.nombre[0]),
                                ),
                                title: Text(p.nombre, style: const TextStyle(fontWeight: FontWeight.bold)),
                                subtitle: Text('${p.celular ?? 'Sin Celular'} • ${p.email ?? 'Sin Email'}'),
                              );
                            }),
                            const SizedBox(height: 10),
                          ],
                        ),
                      );
                    }).toList(),
                  ),
      ),
      floatingActionButton: canAddAny || prov.canCreateViaje()
          ? FloatingActionButton.extended(
              heroTag: 'fab_persona',
              onPressed: () => showModalBottomSheet(
                context: context,
                isScrollControlled: true,
                builder: (_) => const AddPersonaSheet(),
              ),
              label: const Text('Persona'),
              icon: const Icon(Icons.add),
            )
          : null,
    );
  }
}

class ActividadesScreen extends StatelessWidget {
  const ActividadesScreen({super.key});

  Future<void> _confirmDelete(
    BuildContext context,
    String name,
    VoidCallback onDelete,
  ) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Icon(Icons.delete_sweep, color: Colors.red, size: 50),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text(
              '¿Eliminar actividad?',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
            ),
            const SizedBox(height: 8),
            Text(
              "Se borrará '$name' y todos sus gastos asociados.",
              textAlign: TextAlign.center,
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('CANCELAR')),
          ElevatedButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: ElevatedButton.styleFrom(backgroundColor: Colors.red, foregroundColor: Colors.white),
            child: const Text('ELIMINAR'),
          ),
        ],
      ),
    );
    if (confirm == true) onDelete();
  }

  @override
  Widget build(BuildContext context) {
    final prov = context.watch<PersonasProvider>();
    final auth = context.read<AuthService>();

    return Scaffold(
      body: RefreshIndicator(
        onRefresh: prov.refresh,
        child: prov.actividades.isEmpty
            ? const Center(child: Text('No hay actividades registradas'))
            : ListView.builder(
                padding: const EdgeInsets.all(16),
                itemCount: prov.actividades.length,
                itemBuilder: (ctx, i) {
                  final a = prov.actividades[i];
                  final faltante = prov.faltantePorAsignar(a.id!);
                  final canDelete = prov.canDeleteActividad();

                  return Card(
                    child: ListTile(
                      onLongPress: canDelete
                          ? () => _confirmDelete(context, a.nombre, () async {
                                final err = await prov.removeActividad(
                                  a.id!,
                                  auth.accessToken ?? '',
                                );
                                if (err != null && context.mounted) {
                                  ScaffoldMessenger.of(context)
                                      .showSnackBar(SnackBar(content: Text(err)));
                                }
                              })
                          : null,
                      leading: const Icon(Icons.receipt_long, color: Colors.green),
                      title: Text(a.nombre, style: const TextStyle(fontWeight: FontWeight.bold)),
                      subtitle: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(a.fecha),
                          if (faltante.abs() > 0.01)
                            Text(
                              faltante > 0
                                  ? 'Faltan \$${faltante.toStringAsFixed(2)} por asignar'
                                  : 'Exceso de \$${(faltante * -1).toStringAsFixed(2)}',
                              style: TextStyle(
                                color: faltante > 0 ? Colors.red : Colors.blue,
                                fontSize: 12,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                        ],
                      ),
                      trailing: Text(
                        '\$${a.costoTotal.toStringAsFixed(2)}',
                        style: const TextStyle(
                          color: Colors.green,
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  );
                },
              ),
      ),
      floatingActionButton: prov.canCreateActividad()
          ? FloatingActionButton.extended(
              heroTag: 'fab_gasto',
              onPressed: () => showModalBottomSheet(
                context: context,
                isScrollControlled: true,
                builder: (_) => const AddActividadSheet(),
              ),
              label: const Text('Gasto'),
              icon: const Icon(Icons.add_shopping_cart),
            )
          : null,
    );
  }
}

class ResumenScreen extends StatelessWidget {
  const ResumenScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final prov = context.watch<PersonasProvider>();
    final resumen = prov.resumen;
    final familias = resumen?.familias ?? [];

    return Scaffold(
      body: RefreshIndicator(
        onRefresh: prov.refresh,
        child: prov.isLoading && familias.isEmpty
            ? const Center(child: CircularProgressIndicator())
            : ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  const Text(
                    'Balance Consolidado',
                    style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 8),
                  const Text(
                    'Toca una familia para ver detalles',
                    style: TextStyle(color: Colors.grey, fontSize: 12),
                  ),
                  const SizedBox(height: 20),
                  if (prov.canViewResumenGlobal() && resumen?.global != null) ...[
                    _GlobalResumenCard(global: resumen!.global!),
                    const SizedBox(height: 20),
                  ],
                  if (familias.isEmpty)
                    const Center(child: Text('Sin datos para resumir')),
                  ...familias.map((f) {
                    return Card(
                      margin: const EdgeInsets.only(bottom: 12),
                      color: Colors.green.shade50,
                      child: ListTile(
                        onTap: () => Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (_) => FamilyDetailScreen(
                              familiaId: f.familiaId,
                              familiaNombre: f.nombreFamilia,
                            ),
                          ),
                        ),
                        title: Text(
                          'Familia ${f.nombreFamilia}',
                          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
                        ),
                        subtitle: Text(
                          '${f.integrantes} integrantes · Pend: \$${f.pendiente.toStringAsFixed(2)}',
                        ),
                        trailing: Wrap(
                          crossAxisAlignment: WrapCrossAlignment.center,
                          children: [
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.end,
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Text(
                                  '\$${f.totalAsignado.toStringAsFixed(2)}',
                                  style: const TextStyle(
                                    color: Colors.green,
                                    fontSize: 18,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                                Text(
                                  'Pagado: \$${f.totalPagado.toStringAsFixed(2)}',
                                  style: const TextStyle(fontSize: 11, color: Colors.grey),
                                ),
                              ],
                            ),
                            const Icon(Icons.chevron_right, color: Colors.green),
                          ],
                        ),
                      ),
                    );
                  }),
                ],
              ),
      ),
    );
  }
}

class _GlobalResumenCard extends StatelessWidget {
  final ResumenGlobal global;

  const _GlobalResumenCard({required this.global});

  @override
  Widget build(BuildContext context) {
    return Card(
      color: Colors.amber.shade50,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Resumen global (Organizador)',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
            ),
            const SizedBox(height: 8),
            Text('Costo total viaje: \$${global.costoTotalViaje.toStringAsFixed(2)}'),
            Text('Total pagado: \$${global.totalPagado.toStringAsFixed(2)}'),
            Text(
              'Total pendiente: \$${global.totalPendiente.toStringAsFixed(2)}',
              style: TextStyle(
                color: global.totalPendiente > 0 ? Colors.red : Colors.green,
                fontWeight: FontWeight.bold,
              ),
            ),
            if (global.actividadesConFaltante.isNotEmpty) ...[
              const SizedBox(height: 8),
              const Text('Actividades con faltante:', style: TextStyle(fontSize: 12)),
              ...global.actividadesConFaltante.map<Widget>(
                (a) => Text(
                  '• ${a.nombre}: \$${a.faltante.toStringAsFixed(2)}',
                  style: const TextStyle(fontSize: 12, color: Colors.red),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}