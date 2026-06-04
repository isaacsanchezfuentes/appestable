import 'package:flutter/material.dart';
import '../models/persona.dart';
import '../models/actividad.dart';
import '../models/participacion.dart';
import 'api_service.dart';

class PersonasProvider extends ChangeNotifier {
  final ApiService _api = ApiService();
  
  List<Persona> _personas = [];
  List<Actividad> _actividades = [];
  List<Participacion> _participaciones = [];
  bool _isLoading = false;

  List<Persona> get personas => _personas;
  List<Actividad> get actividades => _actividades;
  List<Participacion> get participaciones => _participaciones;
  bool get isLoading => _isLoading;

  Map<String, List<Persona>> get familias {
    Map<String, List<Persona>> map = {};
    for (var p in _personas) {
      map.putIfAbsent(p.familiaNombre, () => []).add(p);
    }
    var sortedKeys = map.keys.toList()..sort((a, b) => a.toLowerCase().compareTo(b.toLowerCase()));
    Map<String, List<Persona>> sortedMap = {};
    for (var key in sortedKeys) {
      sortedMap[key] = map[key]!;
    }
    return sortedMap;
  }

  Future<void> refresh() async {
    _isLoading = true;
    notifyListeners();
    
    try {
      print("📡 Descargando datos del servidor...");
      
      final p = await _api.getPersonas();
      final a = await _api.getActividades();
      final part = await _api.getParticipaciones();
      
      _personas = p;
      _actividades = a;
      _participaciones = part;
      
      print("✅ DATOS CARGADOS: ${_personas.length} personas y ${_participaciones.length} deudas.");
    } catch (e) {
      print("❌ ERROR EN REFRESH: $e");
    }
    
    _isLoading = false;
    notifyListeners();
  }

  double calcularGastoPersona(int personaId) {
    final deudas = _participaciones.where((p) => p.personaId.toInt() == personaId).toList();
    return deudas.fold(0.0, (sum, p) => sum + p.costoIndividual);
  }

  double calcularGastoFamilia(String familiaNombre) {
    final integrantes = _personas.where((p) => p.familiaNombre == familiaNombre).toList();
    if (integrantes.isEmpty) return 0.0;

    // Aseguramos que solo comparamos IDs que existen
    final ids = integrantes.map((p) => p.id).whereType<int>().toList();
    
    double total = 0.0;
    int encontradas = 0;
    
    for (var part in _participaciones) {
      if (ids.contains(part.personaId)) {
        total += part.costoIndividual;
        encontradas++;
      }
    }
    
    if (total > 0) {
      print("💰 ÉXITO: Familia $familiaNombre (IDs: $ids) tiene $encontradas deudas. Suma: \$${total.toStringAsFixed(2)}");
    } else {
      print("⚠️ AVISO: Familia $familiaNombre (IDs: $ids) no tiene deudas vinculadas en las ${_participaciones.length} cargadas.");
    }

    return total;
  }

  List<Actividad> actividadesDeFamilia(String familiaNombre) {
    final ids = _personas.where((p) => p.familiaNombre == familiaNombre).map((p) => p.id).toList();
    final actIds = _participaciones.where((p) => ids.contains(p.personaId)).map((p) => p.actividadId).toSet();
    return _actividades.where((a) => actIds.contains(a.id)).toList();
  }

  Future<String?> addPersona(Persona p, String token, bool isAdmin) async {
    final error = await _api.registrarPersona(p, token, isAdmin: isAdmin);
    if (error == null) await refresh();
    return error;
  }

  Future<String?> addActividad(String nombre, double costo, List<int> pIds, String token) async {
    final fecha = DateTime.now().toString().split(' ')[0];
    final error = await _api.registrarActividad(nombre, costo, fecha, pIds, token);
    if (error == null) await refresh();
    return error;
  }

  Future<String?> updatePago(int partId, bool pagado, String token) async {
    final err = await _api.actualizarParticipacion(partId, pagado, token);
    if (err == null) await refresh();
    return err;
  }
}
