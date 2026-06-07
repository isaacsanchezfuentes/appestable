import 'rol_viaje.dart';

class UsuarioOut {
  final int id;
  final String email;
  final String nombre;
  final String auth0Id;

  UsuarioOut({
    required this.id,
    required this.email,
    required this.nombre,
    required this.auth0Id,
  });

  factory UsuarioOut.fromJson(Map<String, dynamic> json) {
    int _toInt(dynamic v) => (v is num ? v.toInt() : int.tryParse(v?.toString() ?? '') ?? 0);
    return UsuarioOut(
      id: _toInt(json['id']),
      email: json['email'] as String? ?? '',
      nombre: json['nombre'] as String? ?? '',
      auth0Id: json['auth0_id'] as String? ?? '',
    );
  }
}

class MembresiaOut {
  final int viajeId;
  final String viajeNombre;
  final RolViaje rol;
  final int? familiaId;
  final String? familiaNombre;

  MembresiaOut({
    required this.viajeId,
    required this.viajeNombre,
    required this.rol,
    this.familiaId,
    this.familiaNombre,
  });

  factory MembresiaOut.fromJson(Map<String, dynamic> json) {
    int _toInt(dynamic v) => (v is num ? v.toInt() : int.tryParse(v?.toString() ?? '') ?? 0);
    int? _toIntOrNull(dynamic v) => v == null ? null : (v is num ? v.toInt() : int.tryParse(v.toString()));
    return MembresiaOut(
      viajeId: _toInt(json['viaje_id']),
      viajeNombre: json['viaje_nombre'] as String? ?? '',
      rol: RolViaje.fromApi(json['rol'] as String?),
      familiaId: _toIntOrNull(json['familia_id']),
      familiaNombre: json['familia_nombre'] as String?,
    );
  }
}

class MeResponse {
  final UsuarioOut usuario;
  final List<MembresiaOut> membresias;

  MeResponse({
    required this.usuario,
    required this.membresias,
  });

  factory MeResponse.fromJson(Map<String, dynamic> json) {
    return MeResponse(
      usuario: UsuarioOut.fromJson(json['usuario'] as Map<String, dynamic>),
      membresias: (json['membresias'] as List<dynamic>? ?? [])
          .map((m) => MembresiaOut.fromJson(m as Map<String, dynamic>))
          .toList(),
    );
  }
}