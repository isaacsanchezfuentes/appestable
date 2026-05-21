import 'package:flutter/material.dart';
import '../models/persona.dart';
import '../models/actividad.dart';
import 'api_service.dart';

class PersonasProvider extends ChangeNotifier {
  final ApiService _api = ApiService();
  
  List<Persona> _personas = [];
  List<Actividad> _actividades = [];
  bool _isLoading = false;

  List<Persona> get personas => _personas;
  List<Actividad> get actividades => _actividades;
  bool get isLoading => _isLoading;

  // --- REGLA DE NEGOCIO: Agrupar por familia ---
  // Agrupar por familia para la UI y ordenar alfabéticamente
  Map<String, List<Persona>> get familias {
    // 1. Crear el mapa
    Map<String, List<Persona>> map = {};
    for (var p in _personas) {
      map.putIfAbsent(p.familiaNombre, () => []).add(p);
    }
    
    // 2. Ordenar las familias alfabéticamente
    var sortedKeys = map.keys.toList()..sort((a, b) => a.toLowerCase().compareTo(b.toLowerCase()));
    
    Map<String, List<Persona>> sortedMap = {};
    for (var key in sortedKeys) {
      // Ordenar integrantes: Jefes primero, luego por nombre
      var integrantes = map[key]!;
      integrantes.sort((a, b) {
        if (a.esJefe && !b.esJefe) return -1;
        if (!a.esJefe && b.esJefe) return 1;
        return a.nombre.toLowerCase().compareTo(b.nombre.toLowerCase());
      });
      sortedMap[key] = integrantes;
    }
    
    return sortedMap;
  }

  // --- REGLA DE NEGOCIO: Resumen de Gastos por Familia ---
  // Calcula la suma de todos los gastos donde algún miembro de la familia participó
  Map<String, double> get resumenPorFamilia {
    Map<String, double> resumen = {};
    
    // Inicializar familias con 0
    for (var f in familias.keys) {
      resumen[f] = 0.0;
    }

    // Por ahora, simulamos el cálculo basándonos en las actividades registradas
    // En una fase posterior, el backend nos dará el 'costo_individual' exacto
    for (var actividad in _actividades) {
      // Nota: Aquí necesitaríamos las participaciones reales. 
      // Por ahora, para el MVP, asumimos que todos participan o usamos un cálculo base.
      // Implementaremos la lógica real una vez que el endpoint de participaciones esté listo.
      double costoPorPersona = actividad.costoTotal; 
      
      // Ejemplo: Sumar a la familia del 'dueño' o repartir
      // (Lógica simplificada para visualización inmediata)
    }
    return resumen;
  }

  Future<void> refresh() async {
    _isLoading = true;
    notifyListeners();
    
    final results = await Future.wait([
      _api.getPersonas(),
      _api.getActividades(),
    ]);
    
    _personas = results[0] as List<Persona>;
    _actividades = results[1] as List<Actividad>;
    
    _isLoading = false;
    notifyListeners();
  }

  // --- REGLA DE NEGOCIO: Validar Jefe de Familia Único ---
  bool yaExisteJefe(String familiaNombre) {
    return _personas.any((p) => p.familiaNombre == familiaNombre && p.esJefe);
  }

  Future<String?> addPersona(Persona p, String token, bool isAdmin) async {
    // Validación local antes de ir al backend
    if (p.esJefe && yaExisteJefe(p.familiaNombre)) {
      return "Error: La familia '${p.familiaNombre}' ya tiene un jefe asignado.";
    }

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
}
