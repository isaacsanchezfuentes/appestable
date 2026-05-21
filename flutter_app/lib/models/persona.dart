class Persona {
  final int? id;
  final String nombre;
  final String? email;
  final String? celular;
  final bool esJefe;
  final String familiaNombre;
  final String? auth0Id;

  Persona({
    this.id,
    required this.nombre,
    this.email,
    this.celular,
    required this.esJefe,
    required this.familiaNombre,
    this.auth0Id,
  });

  factory Persona.fromJson(Map<String, dynamic> json) {
    return Persona(
      id: json['id'],
      nombre: json['nombre'] ?? 'Sin nombre',
      email: json['email'],
      celular: json['celular'],
      esJefe: json['es_jefe'] ?? false,
      familiaNombre: json['familia_nombre'] ?? 'Sin Familia',
      auth0Id: json['auth0_id'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'nombre': nombre,
      'email': email,
      'celular': celular,
      'es_jefe': esJefe,
      'familia_nombre': familiaNombre,
      'auth0_id': auth0Id,
    };
  }
}
