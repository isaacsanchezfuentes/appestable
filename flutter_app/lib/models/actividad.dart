class Actividad {
  final int? id;
  final String nombre;
  final String fecha;
  final double costoTotal;

  Actividad({
    this.id,
    required this.nombre,
    required this.fecha,
    required this.costoTotal,
  });

  factory Actividad.fromJson(Map<String, dynamic> json) {
    return Actividad(
      id: json['id'],
      nombre: json['nombre'],
      fecha: json['fecha'] ?? '',
      costoTotal: (json['costo_total'] as num).toDouble(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'nombre': nombre,
      'fecha': fecha,
      'costo_total': costoTotal,
    };
  }
}
