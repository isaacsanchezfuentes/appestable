import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../domain/permission_policy.dart';
import '../domain/session_context.dart';
import '../models/persona.dart';
import '../models/actividad.dart';
import '../models/participacion.dart';
import '../models/viaje.dart';
import '../models/me_response.dart';
import '../models/resumen.dart';
import '../models/rol_viaje.dart';
import 'api_service.dart';

class PersonasProvider extends ChangeNotifier {
  final ApiService _api = ApiService();

  static const _prefsViajeKey = 'active_viaje_id';

  List<Persona> _personas = [];
  List<Actividad> _actividades = [];
  List<Participacion> _participaciones = [];
  List<Viaje> _viajes = [];
  MeResponse? _me;
  Viaje? _viajeActivo;
  SessionContext _session = SessionContext.empty;
  ResumenViaje? _resumen;
  bool _isLoading = false;
  String? _error;
  String? _token;

  List<Persona> get personas => _personas;
  List<Actividad> get actividades => _actividades;
  List<Participacion> get participaciones => _participaciones;
  List<Viaje> get viajes => _viajes;
  Viaje? get viajeActivo => _viajeActivo;
  SessionContext get session => _session;
  ResumenViaje? get resumen => _resumen;
  bool get isLoading => _isLoading;
  String? get error => _error;
  bool get hasSession => _token != null && _viajeActivo != null;

  /// Usuario autenticado pero sin ningún viaje en el backend.
  bool get needsFirstViaje => _token != null && _viajes.isEmpty && _viajeActivo == null;

  Map<String, List<Persona>> get familias {
    final map = <String, List<Persona>>{};
    for (final p in _personas) {
      map.putIfAbsent(p.familiaNombre, () => []).add(p);
    }
    final sortedKeys = map.keys.toList()
      ..sort((a, b) => a.toLowerCase().compareTo(b.toLowerCase()));
    return {for (final key in sortedKeys) key: map[key]!};
  }

  bool canViewFamilia(int familiaId) =>
      PermissionPolicy.canViewFamilia(_session, familiaId);

  bool canAddPersona(int familiaId) =>
      PermissionPolicy.canAddPersona(_session, familiaId);

  bool canDeletePersona(Persona persona) =>
      PermissionPolicy.canDeletePersona(_session, persona);

  bool canCreateActividad() =>
      PermissionPolicy.canCreateActividad(_session);

  bool canDeleteActividad() =>
      PermissionPolicy.canDeleteActividad(_session);

  bool canSelectParticipante(Persona persona) =>
      PermissionPolicy.canSelectParticipante(_session, persona);

  bool canEditParticipacion(int familiaId) =>
      PermissionPolicy.canEditParticipacion(_session, familiaId);

  bool canViewResumenGlobal() =>
      PermissionPolicy.canViewResumenGlobal(_session);

  /// Cualquier usuario autenticado puede crear un viaje (API POST /viajes).
  bool canCreateViaje() => _token != null;

  Future<void> initializeSession(String token) async {
    _token = token;
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      _me = await _api.getMe(token);
      if (_me == null) {
        _error =
            'No se pudo conectar al servidor. Revisa que el backend esté corriendo '
            'y que la URL en la app sea correcta.';
        return;
      }

      _viajes = await _api.getViajes(token);

      if (_viajes.isEmpty) {
        _viajeActivo = null;
        _session = SessionContext(
          viajeId: 0,
          viajeNombre: '',
          rol: RolViaje.organizador,
          usuarioId: _me?.usuario.id,
          isLoggedIn: true,
        );
        _clearData();
        _error = null;
        return;
      }

      final prefs = await SharedPreferences.getInstance();
      final savedId = prefs.getInt(_prefsViajeKey);
      Viaje? seleccionado;
      if (savedId != null) {
        seleccionado = _viajes.where((v) => v.id == savedId).firstOrNull;
      }
      seleccionado ??= _viajes.first;
      await _selectViajeInternal(seleccionado, persist: false);
    } catch (e) {
      _error = 'Error al cargar sesión: $e';
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  void clearSession() {
    _token = null;
    _me = null;
    _viajes = [];
    _viajeActivo = null;
    _session = SessionContext.empty;
    _clearData();
    notifyListeners();
  }

  Future<void> selectViaje(int viajeId) async {
    final viaje = _viajes.where((v) => v.id == viajeId).firstOrNull;
    if (viaje == null || _token == null) return;
    _isLoading = true;
    notifyListeners();
    await _selectViajeInternal(viaje);
    _isLoading = false;
    notifyListeners();
  }

  Future<void> _selectViajeInternal(Viaje viaje, {bool persist = true}) async {
    _viajeActivo = viaje;
    _updateSessionForViaje(viaje);
    if (persist) {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setInt(_prefsViajeKey, viaje.id);
    }
    await refresh();

    // Si quedamos como MIEMBRO, ascendemos en background para que la UI
    // (deletes, switches, selección de participantes, etc.) se actualice sola.
    if (session.rol == RolViaje.miembro && _token != null) {
      ensureCanManageCurrentViaje(); // fire & forget, notificará cuando termine
    }
  }

  void _updateSessionForViaje(Viaje viaje) {
    final membresia = _me?.membresias
        .where((m) => m.viajeId == viaje.id)
        .firstOrNull;
    if (membresia != null) {
      _session = SessionContext(
        viajeId: viaje.id,
        viajeNombre: viaje.nombre,
        rol: membresia.rol,
        familiaId: membresia.familiaId,
        usuarioId: _me?.usuario.id,
        isLoggedIn: true,
      );
    } else {
      _session = SessionContext(
        viajeId: viaje.id,
        viajeNombre: viaje.nombre,
        rol: RolViaje.miembro,
        usuarioId: _me?.usuario.id,
        isLoggedIn: _token != null,
      );
    }
  }

  Future<String?> createViaje(String nombre) async {
    if (_token == null) return 'Debes iniciar sesión';
    final result = await _api.crearViaje(_token!, nombre);
    if (result.error != null) return result.error;
    final id = result.id;
    if (id == null) return 'No se pudo crear el viaje';
    _me = await _api.getMe(_token!);
    _viajes = await _api.getViajes(_token!);
    final nuevo = _viajes.where((v) => v.id == id).firstOrNull;
    if (nuevo != null) {
      await selectViaje(nuevo.id);
    }
    _error = null;
    notifyListeners();
    return null;
  }

  Future<void> refresh() async {
    if (_token == null || _viajeActivo == null) return;

    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final viajeId = _viajeActivo!.id;
      print("Refrescando datos para viaje $viajeId...");
      final results = await Future.wait([
        _api.getPersonas(_token!, viajeId),
        _api.getActividades(_token!, viajeId),
        _api.getParticipaciones(_token!, viajeId),
        _api.getResumen(_token!, viajeId),
      ]);

      _personas = results[0] as List<Persona>;
      _actividades = results[1] as List<Actividad>;
      _participaciones = results[2] as List<Participacion>;
      _resumen = results[3] as ResumenViaje?;
      print("Sincronización completa: ${_personas.length} personas encontradas.");
    } catch (e) {
      print("Error en PersonasProvider.refresh: $e");
      _error = 'Error al actualizar: $e';
    }

    _isLoading = false;
    notifyListeners();
  }

  void _clearData() {
    _personas = [];
    _actividades = [];
    _participaciones = [];
    _resumen = null;
  }

  ResumenFamilia? resumenFamilia(int familiaId) =>
      _resumen?.familias.where((f) => f.familiaId == familiaId).firstOrNull;

  ResumenFamilia? resumenFamiliaPorNombre(String nombre) =>
      _resumen?.familias.where((f) => f.nombreFamilia == nombre).firstOrNull;

  int? participacionId(int personaId, int actividadId) {
    final part = _participaciones
        .where((p) => p.personaId == personaId && p.actividadId == actividadId)
        .firstOrNull;
    return part?.id;
  }

  double calcularGastoPersona(int personaId) {
    final resumenLineas = _resumen?.familias
        .expand((f) => f.lineas)
        .where((l) => l.personaId == personaId);
    if (resumenLineas != null && resumenLineas.isNotEmpty) {
      return resumenLineas.fold(0.0, (sum, l) => sum + l.monto);
    }
    return _participaciones
        .where((p) => p.personaId == personaId)
        .fold(0.0, (sum, p) => sum + p.costoIndividual);
  }

  double calcularGastoFamilia(String familiaNombre) {
    final res = resumenFamiliaPorNombre(familiaNombre);
    if (res != null) return res.totalAsignado;
    final integrantes = _personas.where((p) => p.familiaNombre == familiaNombre);
    final ids = integrantes.map((p) => p.id).toSet();
    return _participaciones
        .where((p) => ids.contains(p.personaId))
        .fold(0.0, (sum, p) => sum + p.costoIndividual);
  }

  double faltantePorAsignar(int actividadId) {
    final actividad = _actividades
        .where((a) => a.id == actividadId)
        .firstOrNull;
    if (actividad == null) return 0;
    final asignado = _participaciones
        .where((p) => p.actividadId == actividadId)
        .fold(0.0, (sum, p) => sum + p.costoIndividual);
    return actividad.costoTotal - asignado;
  }

  List<Actividad> actividadesDeFamilia(String familiaNombre) {
    final integrantes = _personas.where((p) => p.familiaNombre == familiaNombre);
    final ids = integrantes.map((p) => p.id).toSet();
    final actIds = _participaciones
        .where((p) => ids.contains(p.personaId))
        .map((p) => p.actividadId)
        .toSet();
    return _actividades.where((a) => actIds.contains(a.id)).toList();
  }

  /// Asegura que el usuario actual tenga rol alto (ORGANIZADOR) en el viaje activo.
  /// Si actualmente es MIEMBRO (o no tiene membresía suficiente), llama al backend
  /// para crear/ascender la membresía. Luego refresca /me para actualizar la sesión local.
  /// Devuelve error solo si el ensure falló.
  Future<String?> ensureCanManageCurrentViaje() async {
    if (_viajeActivo == null || _token == null) return null;
    if (session.rol != RolViaje.miembro) return null; // ya tiene privilegios

    final viajeId = _viajeActivo!.id;
    final err = await _api.ensureMembership(_token!, viajeId);
    if (err == null) {
      try {
        _me = await _api.getMe(_token!);
        _updateSessionForViaje(_viajeActivo!);
        notifyListeners();
      } catch (_) {}
    }
    return err;
  }

  Future<String?> addPersona(Persona p, String token) async {
    try {
      final viajeId = _viajeActivo?.id;
      if (viajeId == null) return 'Sin viaje activo';

      final ensureErr = await ensureCanManageCurrentViaje();
      if (ensureErr != null) return ensureErr;

      final error = await _api.registrarPersona(p, token, viajeId);
      if (error == null) await refresh();
      return error;
    } catch (e) {
      return 'Error al registrar persona: $e';
    }
  }

  Future<String?> addActividad(
    String nombre,
    double costo,
    List<int> pIds,
    String token,
  ) async {
    try {
      final viajeId = _viajeActivo?.id;
      if (viajeId == null) return 'Sin viaje activo';

      final ensureErr = await ensureCanManageCurrentViaje();
      if (ensureErr != null) return ensureErr;

      final fecha = DateTime.now().toIso8601String().split('T').first;
      final error =
          await _api.registrarActividad(nombre, costo, fecha, pIds, token, viajeId);
      if (error == null) await refresh();
      return error;
    } catch (e) {
      return 'Error al registrar actividad: $e';
    }
  }

  Future<String?> updateParticipacion(
    int partId, {
    double? costo,
    bool? pagado,
    required String token,
  }) async {
    try {
      final viajeId = _viajeActivo?.id;
      if (viajeId == null) return 'Sin viaje activo';

      final ensureErr = await ensureCanManageCurrentViaje();
      if (ensureErr != null) return ensureErr;

      final err = await _api.actualizarParticipacion(
        viajeId,
        partId,
        token,
        costo: costo,
        pagado: pagado,
      );
      if (err == null) await refresh();
      return err;
    } catch (e) {
      return 'Error al actualizar participación: $e';
    }
  }

  Future<String?> removePersona(int id, String token) async {
    try {
      final viajeId = _viajeActivo?.id;
      if (viajeId == null) return 'Sin viaje activo';

      final ensureErr = await ensureCanManageCurrentViaje();
      if (ensureErr != null) return ensureErr;

      final err = await _api.eliminarPersona(viajeId, id, token);
      if (err == null) await refresh();
      return err;
    } catch (e) {
      return 'Error al eliminar persona: $e';
    }
  }

  Future<String?> removeActividad(int id, String token) async {
    try {
      final viajeId = _viajeActivo?.id;
      if (viajeId == null) return 'Sin viaje activo';

      final ensureErr = await ensureCanManageCurrentViaje();
      if (ensureErr != null) return ensureErr;

      final err = await _api.eliminarActividad(viajeId, id, token);
      if (err == null) await refresh();
      return err;
    } catch (e) {
      return 'Error al eliminar actividad: $e';
    }
  }
}

extension _FirstOrNull<E> on Iterable<E> {
  E? get firstOrNull {
    final it = iterator;
    if (!it.moveNext()) return null;
    return it.current;
  }
}