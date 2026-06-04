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
    return Participacion(
      id: json['id'],
      // Mapeamos los nombres del backend (con _) a los de la app
      personaId: json['persona_id'] ?? 0,
      actividadId: json['actividad_id'] ?? 0,
      costoIndividual: (json['costo_individual'] as num).toDouble(),
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
