class Participacion {
  final int? id;
  final int personaId;
  final int actividadId;
  final double costoIndividual;
  final bool pagado;

  Participacion({
    this.id,
    required this.personaId,
    required this.actividadId,
    required this.costoIndividual,
    required this.pagado,
  });

  factory Participacion.fromJson(Map<String, dynamic> json) {
    int? _toInt(dynamic v) => v == null ? null : (v is num ? v.toInt() : int.tryParse(v.toString()));
    int _toIntReq(dynamic v) => _toInt(v) ?? 0;
    return Participacion(
      id: _toInt(json['id']),
      // Mapeamos los nombres del backend (con _) a los de la app
      personaId: _toIntReq(json['persona_id']),
      actividadId: _toIntReq(json['actividad_id']),
      costoIndividual: (json['costo_individual'] as num?)?.toDouble() ?? 0,
      pagado: json['pagado'] ?? false,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'persona_id': personaId,
      'actividad_id': actividadId,
      'costo_individual': costoIndividual,
      'pagado': pagado,
    };
  }
}
