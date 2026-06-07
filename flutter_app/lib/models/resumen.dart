class ResumenLinea {
  final int personaId;
  final String personaNombre;
  final bool esJefe;
  final int actividadId;
  final String actividadNombre;
  final String actividadFecha;
  final double monto;
  final bool pagado;

  ResumenLinea({
    required this.personaId,
    required this.personaNombre,
    required this.esJefe,
    required this.actividadId,
    required this.actividadNombre,
    required this.actividadFecha,
    required this.monto,
    required this.pagado,
  });

  factory ResumenLinea.fromJson(Map<String, dynamic> json) {
    int _toInt(dynamic v) => (v is num ? v.toInt() : int.tryParse(v?.toString() ?? '') ?? 0);
    return ResumenLinea(
      personaId: _toInt(json['persona_id']),
      personaNombre: json['persona_nombre'] as String? ?? '',
      esJefe: json['es_jefe'] as bool? ?? false,
      actividadId: _toInt(json['actividad_id']),
      actividadNombre: json['actividad_nombre'] as String? ?? '',
      actividadFecha: json['actividad_fecha'] as String? ?? '',
      monto: (json['monto'] as num?)?.toDouble() ?? 0,
      pagado: json['pagado'] as bool? ?? false,
    );
  }
}

class ResumenFamilia {
  final int familiaId;
  final String nombreFamilia;
  final int integrantes;
  final int actividadesCount;
  final double totalAsignado;
  final double totalPagado;
  final double pendiente;
  final List<ResumenLinea> lineas;

  ResumenFamilia({
    required this.familiaId,
    required this.nombreFamilia,
    required this.integrantes,
    required this.actividadesCount,
    required this.totalAsignado,
    required this.totalPagado,
    required this.pendiente,
    required this.lineas,
  });

  factory ResumenFamilia.fromJson(Map<String, dynamic> json) {
    int _toInt(dynamic v) => (v is num ? v.toInt() : int.tryParse(v?.toString() ?? '') ?? 0);
    return ResumenFamilia(
      familiaId: _toInt(json['familia_id']),
      nombreFamilia: json['nombre_familia'] as String? ?? '',
      integrantes: _toInt(json['integrantes']),
      actividadesCount: _toInt(json['actividades_count']),
      totalAsignado: (json['total_asignado'] as num?)?.toDouble() ?? 0,
      totalPagado: (json['total_pagado'] as num?)?.toDouble() ?? 0,
      pendiente: (json['pendiente'] as num?)?.toDouble() ?? 0,
      lineas: (json['lineas'] as List<dynamic>? ?? [])
          .map((l) => ResumenLinea.fromJson(l as Map<String, dynamic>))
          .toList(),
    );
  }
}

class ResumenRankingFamilia {
  final String nombre;
  final double total;

  ResumenRankingFamilia({required this.nombre, required this.total});

  factory ResumenRankingFamilia.fromJson(Map<String, dynamic> json) {
    return ResumenRankingFamilia(
      nombre: json['nombre'] as String? ?? '',
      total: (json['total'] as num?)?.toDouble() ?? 0,
    );
  }
}

class ActividadConFaltante {
  final int actividadId;
  final String nombre;
  final double costoTotal;
  final double asignado;
  final double faltante;

  ActividadConFaltante({
    required this.actividadId,
    required this.nombre,
    required this.costoTotal,
    required this.asignado,
    required this.faltante,
  });

  factory ActividadConFaltante.fromJson(Map<String, dynamic> json) {
    int _toInt(dynamic v) => (v is num ? v.toInt() : int.tryParse(v?.toString() ?? '') ?? 0);
    return ActividadConFaltante(
      actividadId: _toInt(json['actividad_id']),
      nombre: json['nombre'] as String? ?? '',
      costoTotal: (json['costo_total'] as num?)?.toDouble() ?? 0,
      asignado: (json['asignado'] as num?)?.toDouble() ?? 0,
      faltante: (json['faltante'] as num?)?.toDouble() ?? 0,
    );
  }
}

class ResumenGlobal {
  final double costoTotalViaje;
  final double totalPagado;
  final double totalPendiente;
  final List<ResumenRankingFamilia> familiasRanking;
  final List<ActividadConFaltante> actividadesConFaltante;

  ResumenGlobal({
    required this.costoTotalViaje,
    required this.totalPagado,
    required this.totalPendiente,
    required this.familiasRanking,
    required this.actividadesConFaltante,
  });

  factory ResumenGlobal.fromJson(Map<String, dynamic> json) {
    return ResumenGlobal(
      costoTotalViaje: (json['costo_total_viaje'] as num?)?.toDouble() ?? 0,
      totalPagado: (json['total_pagado'] as num?)?.toDouble() ?? 0,
      totalPendiente: (json['total_pendiente'] as num?)?.toDouble() ?? 0,
      familiasRanking: (json['familias_ranking'] as List<dynamic>? ?? [])
          .map((r) => ResumenRankingFamilia.fromJson(r as Map<String, dynamic>))
          .toList(),
      actividadesConFaltante: (json['actividades_con_faltante'] as List<dynamic>? ?? [])
          .map((a) => ActividadConFaltante.fromJson(a as Map<String, dynamic>))
          .toList(),
    );
  }
}

class ResumenViaje {
  final int viajeId;
  final String rol;
  final List<ResumenFamilia> familias;
  final ResumenGlobal? global;

  ResumenViaje({
    required this.viajeId,
    required this.rol,
    required this.familias,
    this.global,
  });

  factory ResumenViaje.fromJson(Map<String, dynamic> json) {
    int _toInt(dynamic v) => (v is num ? v.toInt() : int.tryParse(v?.toString() ?? '') ?? 0);
    return ResumenViaje(
      viajeId: _toInt(json['viaje_id']),
      rol: json['rol'] as String? ?? '',
      familias: (json['familias'] as List<dynamic>? ?? [])
          .map((f) => ResumenFamilia.fromJson(f as Map<String, dynamic>))
          .toList(),
      global: json['global'] != null
          ? ResumenGlobal.fromJson(json['global'] as Map<String, dynamic>)
          : null,
    );
  }
}