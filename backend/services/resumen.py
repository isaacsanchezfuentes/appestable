from typing import Any, Dict, List

from sqlalchemy.orm import Session

from db.models import Actividad, Familia, Participacion, Persona, RolViaje
from permissions import ViajeAccess, can_view_familia


def _monto_efectivo(part: Participacion, actividad: Actividad, participaciones: List[Participacion]) -> float:
    if part.costo_individual and part.costo_individual > 0:
        return part.costo_individual
    count = max(1, sum(1 for p in participaciones if p.actividad_id == actividad.id))
    return actividad.costo_total / count


def calcular_resumen_viaje(
    db: Session,
    viaje_id: int,
    access: ViajeAccess,
) -> Dict[str, Any]:
    familias = db.query(Familia).filter(Familia.viaje_id == viaje_id).all()
    personas = (
        db.query(Persona)
        .filter(Persona.viaje_id == viaje_id, Persona.is_deleted.is_(False))
        .all()
    )
    actividades = db.query(Actividad).filter(Actividad.viaje_id == viaje_id).all()
    actividad_ids = [a.id for a in actividades]
    participaciones = (
        db.query(Participacion).filter(Participacion.actividad_id.in_(actividad_ids)).all()
        if actividad_ids
        else []
    )

    familias_visibles = [f for f in familias if can_view_familia(access, f.id)]
    resumenes_familia = []

    for familia in familias_visibles:
        miembros = [p for p in personas if p.familia_id == familia.id]
        miembro_ids = {m.id for m in miembros}
        parts_familia = [p for p in participaciones if p.persona_id in miembro_ids]

        lineas = []
        for part in parts_familia:
            persona = next((m for m in miembros if m.id == part.persona_id), None)
            actividad = next((a for a in actividades if a.id == part.actividad_id), None)
            if not persona or not actividad:
                continue
            monto = _monto_efectivo(part, actividad, participaciones)
            lineas.append(
                {
                    "persona_id": persona.id,
                    "persona_nombre": persona.nombre,
                    "es_jefe": persona.es_jefe,
                    "actividad_id": actividad.id,
                    "actividad_nombre": actividad.nombre,
                    "actividad_fecha": actividad.fecha,
                    "monto": monto,
                    "pagado": part.pagado,
                }
            )

        total_asignado = sum(l["monto"] for l in lineas)
        total_pagado = sum(l["monto"] for l in lineas if l["pagado"])
        resumenes_familia.append(
            {
                "familia_id": familia.id,
                "nombre_familia": familia.nombre_familia,
                "integrantes": len(miembros),
                "actividades_count": len({l["actividad_id"] for l in lineas}),
                "total_asignado": total_asignado,
                "total_pagado": total_pagado,
                "pendiente": total_asignado - total_pagado,
                "lineas": lineas,
            }
        )

    global_resumen = None
    if access.rol == RolViaje.ORGANIZADOR:
        costo_total = sum(a.costo_total for a in actividades)
        total_pagado = sum(r["total_pagado"] for r in resumenes_familia)
        total_pendiente = sum(r["pendiente"] for r in resumenes_familia)
        ranking = sorted(resumenes_familia, key=lambda r: r["total_asignado"], reverse=True)
        faltantes = []
        for actividad in actividades:
            asignado = sum(
                _monto_efectivo(p, actividad, participaciones)
                for p in participaciones
                if p.actividad_id == actividad.id
            )
            diff = actividad.costo_total - asignado
            if abs(diff) > 0.01:
                faltantes.append(
                    {
                        "actividad_id": actividad.id,
                        "nombre": actividad.nombre,
                        "costo_total": actividad.costo_total,
                        "asignado": asignado,
                        "faltante": diff,
                    }
                )
        global_resumen = {
            "costo_total_viaje": costo_total,
            "total_pagado": total_pagado,
            "total_pendiente": total_pendiente,
            "familias_ranking": [
                {"nombre": r["nombre_familia"], "total": r["total_asignado"]} for r in ranking
            ],
            "actividades_con_faltante": faltantes,
        }

    return {
        "viaje_id": viaje_id,
        "rol": access.rol.value,
        "familias": resumenes_familia,
        "global": global_resumen,
    }