import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';
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
        ChangeNotifierProvider(create: (_) => PersonasProvider()..refresh()),
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

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthService>();
    final personasProv = context.watch<PersonasProvider>();

    return Scaffold(
      appBar: AppBar(
        title: Text(_selectedIndex == 0 ? 'Gestión de Personas' : _selectedIndex == 1 ? 'Historial de Gastos' : 'Balance por Familia'),
        actions: [
          if (auth.isAdmin)
            const Padding(
              padding: EdgeInsets.only(right: 8.0),
              child: Chip(label: Text("ADMIN"), backgroundColor: Colors.amber, side: BorderSide.none),
            ),
          if (!auth.isLoggedIn)
            IconButton(icon: const Icon(Icons.login), onPressed: () => auth.login())
          else
            IconButton(icon: const Icon(Icons.logout), onPressed: () => auth.logout()),
          IconButton(icon: const Icon(Icons.refresh), onPressed: () => personasProv.refresh()),
        ],
      ),
      body: IndexedStack(
        index: _selectedIndex,
        children: const [
          PersonasScreen(),
          ActividadesScreen(),
          ResumenScreen(),
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _selectedIndex,
        onDestinationSelected: (index) => setState(() => _selectedIndex = index),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.group), label: 'Personas'),
          NavigationDestination(icon: Icon(Icons.list_alt), label: 'Actividades'),
          NavigationDestination(icon: Icon(Icons.analytics), label: 'Resumen'),
        ],
      ),
    );
  }
}

class PersonasScreen extends StatelessWidget {
  const PersonasScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final prov = context.watch<PersonasProvider>();
    final familias = prov.familias;

    return Scaffold(
      body: RefreshIndicator(
        onRefresh: prov.refresh,
        child: prov.isLoading && prov.personas.isEmpty
            ? const Center(child: CircularProgressIndicator())
            : familias.isEmpty
                ? const Center(child: Text("Sin registros. Desliza para actualizar."))
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
                              decoration: BoxDecoration(color: Colors.blue.shade700, borderRadius: const BorderRadius.vertical(top: Radius.circular(15))),
                              child: Text("👪 Familia ${entry.key}", style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.white, fontSize: 18)),
                            ),
                            ...entry.value.map((p) => ListTile(
                              leading: CircleAvatar(backgroundColor: p.esJefe ? Colors.amber : Colors.blue.shade100, child: Text(p.nombre[0])),
                              title: Text(p.nombre, style: const TextStyle(fontWeight: FontWeight.bold)),
                              subtitle: Text("${p.celular ?? 'Sin Celular'} • ${p.email ?? 'Sin Email'}"),
                              trailing: p.esJefe ? const Chip(label: Text('Jefe'), backgroundColor: Colors.amberAccent) : null,
                            )),
                            const SizedBox(height: 10),
                          ],
                        ),
                      );
                    }).toList(),
                  ),
      ),
      floatingActionButton: FloatingActionButton.extended(
        heroTag: "fab_persona",
        onPressed: () => showModalBottomSheet(
          context: context,
          isScrollControlled: true,
          builder: (_) => const AddPersonaSheet(),
        ),
        label: const Text("Persona"),
        icon: const Icon(Icons.add),
      ),
    );
  }
}

class ActividadesScreen extends StatelessWidget {
  const ActividadesScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final prov = context.watch<PersonasProvider>();

    return Scaffold(
      body: RefreshIndicator(
        onRefresh: prov.refresh,
        child: prov.actividades.isEmpty 
          ? const Center(child: Text("No hay actividades registradas"))
          : ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: prov.actividades.length,
              itemBuilder: (ctx, i) {
                final a = prov.actividades[i];
                return Card(
                  child: ListTile(
                    leading: const Icon(Icons.receipt_long, color: Colors.green),
                    title: Text(a.nombre, style: const TextStyle(fontWeight: FontWeight.bold)),
                    subtitle: Text(a.fecha),
                    trailing: Text("\$${a.costoTotal.toStringAsFixed(2)}", style: const TextStyle(color: Colors.green, fontSize: 18, fontWeight: FontWeight.bold)),
                  ),
                );
              },
            ),
      ),
      floatingActionButton: FloatingActionButton.extended(
        heroTag: "fab_gasto",
        onPressed: () => showModalBottomSheet(
          context: context,
          isScrollControlled: true,
          builder: (_) => const AddActividadSheet(),
        ),
        label: const Text("Gasto"),
        icon: const Icon(Icons.add_shopping_cart),
      ),
    );
  }
}

class ResumenScreen extends StatelessWidget {
  const ResumenScreen({super.key});
  
  @override
  Widget build(BuildContext context) {
    final prov = context.watch<PersonasProvider>();
    final familias = prov.familias;

    return Scaffold(
      body: RefreshIndicator(
        onRefresh: prov.refresh,
        child: prov.isLoading && prov.participaciones.isEmpty
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                const Text("Balance Consolidado", style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
                const SizedBox(height: 8),
                const Text("Toca una familia para ver detalles", style: TextStyle(color: Colors.grey, fontSize: 12)),
                const SizedBox(height: 20),
                if (familias.isEmpty) const Center(child: Text("Sin datos para resumir")),
                ...familias.entries.map((e) {
                  final total = prov.calcularGastoFamilia(e.key);
                  
                  return Card(
                    margin: const EdgeInsets.only(bottom: 12),
                    color: Colors.green.shade50,
                    child: ListTile(
                      onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => FamilyDetailScreen(familiaNombre: e.key))),
                      title: Text("Familia ${e.key}", style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
                      subtitle: Text("${e.value.length} integrantes"),
                      trailing: Wrap(
                        crossAxisAlignment: WrapCrossAlignment.center,
                        children: [
                          Text("\$${total.toStringAsFixed(2)}", style: const TextStyle(color: Colors.green, fontSize: 20, fontWeight: FontWeight.bold)),
                          const Icon(Icons.chevron_right, color: Colors.green),
                        ],
                      ),
                    ),
                  );
                }).toList(),
              ],
            ),
      ),
    );
  }
}
