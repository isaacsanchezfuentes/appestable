import 'rol_viaje.dart';

class Persona {
  final int? id;
  final String nombre;
  final String? email;
  final String? celular;
  final bool esJefe;
  final int? familiaId;
  final String familiaNombre;
  final int? viajeId;
  final RolViaje rol;
  final String? auth0Id;

  Persona({
    this.id,
    required this.nombre,
    this.email,
    this.celular,
    required this.esJefe,
    this.familiaId,
    required this.familiaNombre,
    this.viajeId,
    this.rol = RolViaje.miembro,
    this.auth0Id,
  });

  factory Persona.fromJson(Map<String, dynamic> json) {
    int? _toInt(dynamic v) => v == null ? null : (v is num ? v.toInt() : int.tryParse(v.toString()));
    return Persona(
      id: _toInt(json['id']),
      nombre: json['nombre'] as String? ?? 'Sin nombre',
      email: json['email'] as String?,
      celular: json['celular'] as String?,
      esJefe: json['es_jefe'] as bool? ?? false,
      familiaId: _toInt(json['familia_id']),
      familiaNombre: json['familia_nombre'] as String? ?? 'Sin Familia',
      viajeId: _toInt(json['viaje_id']),
      rol: RolViaje.fromApi(json['rol'] as String?),
      auth0Id: json['auth0_id'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'nombre': nombre,
      'email': email,
      'celular': celular,
      'es_jefe': esJefe,
      'familia_nombre': familiaNombre,
      if (auth0Id != null) 'auth0_id': auth0Id,
    };
  }
}