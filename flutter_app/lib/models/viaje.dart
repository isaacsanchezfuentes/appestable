class Viaje {
  final int id;
  final String nombre;
  final String descripcion;
  final String fechaInicio;
  final String fechaFin;
  final String estado;
  final int? organizadorUsuarioId;

  Viaje({
    required this.id,
    required this.nombre,
    this.descripcion = '',
    this.fechaInicio = '',
    this.fechaFin = '',
    this.estado = 'ACTIVO',
    this.organizadorUsuarioId,
  });

  factory Viaje.fromJson(Map<String, dynamic> json) {
    int _toInt(dynamic v) => (v is num ? v.toInt() : int.tryParse(v?.toString() ?? '') ?? 0);
    return Viaje(
      id: _toInt(json['id']),
      nombre: json['nombre'] as String? ?? 'Sin nombre',
      descripcion: json['descripcion'] as String? ?? '',
      fechaInicio: json['fecha_inicio'] as String? ?? '',
      fechaFin: json['fecha_fin'] as String? ?? '',
      estado: json['estado'] as String? ?? 'ACTIVO',
      organizadorUsuarioId: _toInt(json['organizador_usuario_id']) as int?,
    );
  }
}