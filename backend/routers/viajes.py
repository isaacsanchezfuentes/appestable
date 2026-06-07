import logging
from typing import List

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import or_
from sqlalchemy.orm import Session

from auth import get_or_create_usuario, require_global_admin
from db.models import Actividad, Familia, MembresiaViaje, Participacion, Persona, RolViaje, Usuario, Viaje
from db.session import get_db
from permissions import (
    ViajeAccess,
    can_add_persona,
    can_create_actividad,
    can_delete_persona,
    can_edit_participacion,
    can_select_participante,
    can_view_familia,
    get_membresia,
    require_viaje_access,
)
from schemas import ActividadCreate, ParticipacionUpdate, PersonaCreate, ViajeCreate
from serializers import serialize_actividad, serialize_participacion, serialize_persona
from services.bootstrap import ensure_default_membresia
from services.resumen import calcular_resumen_viaje

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/viajes", tags=["viajes"])


def _get_or_create_familia(db: Session, viaje_id: int, nombre: str) -> Familia:
    nombre_limpio = nombre.strip()
    fam = (
        db.query(Familia)
        .filter(Familia.viaje_id == viaje_id, Familia.nombre_familia.ilike(nombre_limpio))
        .first()
    )
    if fam:
        return fam
    fam = Familia(nombre_familia=nombre_limpio, viaje_id=viaje_id)
    db.add(fam)
    db.commit()
    db.refresh(fam)
    return fam


@router.get("")
def listar_viajes(
    usuario: Usuario = Depends(get_or_create_usuario),
    db: Session = Depends(get_db),
):
    ensure_default_membresia(db, usuario)
    membresias = db.query(MembresiaViaje).filter(MembresiaViaje.usuario_id == usuario.id).all()
    viaje_ids = [m.viaje_id for m in membresias]
    if not viaje_ids:
        return []
    viajes = db.query(Viaje).filter(Viaje.id.in_(viaje_ids)).order_by(Viaje.nombre).all()
    return [
        {
            "id": v.id,
            "nombre": v.nombre,
            "descripcion": v.descripcion,
            "fecha_inicio": v.fecha_inicio,
            "fecha_fin": v.fecha_fin,
            "estado": v.estado,
            "organizador_usuario_id": v.organizador_usuario_id,
        }
        for v in viajes
    ]


@router.post("", status_code=201)
def crear_viaje(
    data: ViajeCreate,
    usuario: Usuario = Depends(get_or_create_usuario),
    db: Session = Depends(get_db),
):
    try:
        viaje = Viaje(
            nombre=data.nombre.strip(),
            descripcion=data.descripcion,
            fecha_inicio=data.fecha_inicio,
            fecha_fin=data.fecha_fin,
            organizador_usuario_id=usuario.id,
        )
        db.add(viaje)
        db.commit()
        db.refresh(viaje)

        membresia = MembresiaViaje(
            viaje_id=viaje.id,
            usuario_id=usuario.id,
            familia_id=None,
            rol=RolViaje.ORGANIZADOR,
        )
        db.add(membresia)
        db.commit()
    except Exception as exc:
        db.rollback()
        logger.exception("Error creando viaje")
        raise HTTPException(500, f"No se pudo crear el viaje: {exc}") from exc

    return {"status": "success", "id": viaje.id}


@router.post("/{viaje_id}/ensure-membership")
def ensure_membership(
    viaje_id: int,
    usuario: Usuario = Depends(get_or_create_usuario),
    db: Session = Depends(get_db),
):
    """Si el usuario actual no tiene membresía en este viaje, se la crea como ORGANIZADOR.
    Esto permite que quien está usando activamente la app en un viaje que puede ver,
    pueda agregar personas/familias (útil para flujos de un solo dispositivo / testing).
    """
    membresia = get_membresia(db, viaje_id, usuario.id)
    if membresia:
        return {
            "status": "exists",
            "rol": membresia.rol.value,
            "familia_id": membresia.familia_id,
        }

    # Verificar que el viaje exista
    viaje = db.query(Viaje).filter(Viaje.id == viaje_id).first()
    if not viaje:
        raise HTTPException(404, "Viaje no encontrado")

    membresia = MembresiaViaje(
        viaje_id=viaje_id,
        usuario_id=usuario.id,
        familia_id=None,
        rol=RolViaje.ORGANIZADOR,
    )
    db.add(membresia)
    db.commit()
    db.refresh(membresia)
    return {"status": "created", "rol": "ORGANIZADOR", "familia_id": None}


@router.get("/{viaje_id}")
def obtener_viaje(
    viaje_id: int,
    access: ViajeAccess = Depends(require_viaje_access()),
    db: Session = Depends(get_db),
):
    viaje = db.query(Viaje).filter(Viaje.id == viaje_id).first()
    if not viaje:
        raise HTTPException(404, "Viaje no encontrado")
    return {
        "id": viaje.id,
        "nombre": viaje.nombre,
        "descripcion": viaje.descripcion,
        "fecha_inicio": viaje.fecha_inicio,
        "fecha_fin": viaje.fecha_fin,
        "estado": viaje.estado,
        "organizador_usuario_id": viaje.organizador_usuario_id,
        "rol_actual": access.rol.value,
    }


@router.get("/{viaje_id}/resumen")
def resumen_viaje(
    viaje_id: int,
    access: ViajeAccess = Depends(require_viaje_access()),
    db: Session = Depends(get_db),
):
    return calcular_resumen_viaje(db, viaje_id, access)


@router.get("/{viaje_id}/familias")
def listar_familias(
    viaje_id: int,
    access: ViajeAccess = Depends(require_viaje_access()),
    db: Session = Depends(get_db),
):
    familias = db.query(Familia).filter(Familia.viaje_id == viaje_id).all()
    return [
        {"id": f.id, "nombre_familia": f.nombre_familia, "viaje_id": f.viaje_id}
        for f in familias
        if can_view_familia(access, f.id) or access.rol == RolViaje.ORGANIZADOR
    ]


@router.get("/{viaje_id}/personas")
def listar_personas(
    viaje_id: int,
    access: ViajeAccess = Depends(require_viaje_access()),
    db: Session = Depends(get_db),
):
    personas = (
        db.query(Persona)
        .filter(
            Persona.viaje_id == viaje_id,
            or_(Persona.is_deleted.is_(False), Persona.is_deleted.is_(None)),
        )
        .all()
    )
    logger.info(f"Listando {len(personas)} personas para viaje {viaje_id}")
    return [
        serialize_persona(p)
        for p in personas
        if can_view_familia(access, p.familia_id)
    ]


@router.post("/{viaje_id}/personas", status_code=201)
def crear_persona(
    viaje_id: int,
    data: PersonaCreate,
    access: ViajeAccess = Depends(require_viaje_access()),
    db: Session = Depends(get_db),
):
    logger.info(f"Creando persona: {data.nombre} en viaje {viaje_id}")
    familia = _get_or_create_familia(db, viaje_id, data.familia_nombre)
    if not can_add_persona(access, familia.id):
        raise HTTPException(403, "No puedes agregar personas en esta familia")

    if data.es_jefe:
        jefe = (
            db.query(Persona)
            .filter(Persona.familia_id == familia.id, Persona.es_jefe.is_(True), Persona.is_deleted.is_(False))
            .first()
        )
        if jefe:
            raise HTTPException(400, "Solo puede existir un jefe por familia")

    rol = RolViaje.JEFE_FAMILIA if data.es_jefe else RolViaje.MIEMBRO
    persona = Persona(
        nombre=data.nombre,
        email=data.email,
        celular=data.celular,
        es_jefe=data.es_jefe,
        familia_id=familia.id,
        viaje_id=viaje_id,
        rol=rol,
    )
    db.add(persona)
    db.commit()
    db.refresh(persona)
    return {"status": "success", "id": persona.id}


@router.post("/{viaje_id}/personas/admin", status_code=201)
def crear_persona_admin(
    viaje_id: int,
    data: PersonaCreate,
    _: dict = Depends(require_global_admin),
    db: Session = Depends(get_db),
):
    familia = _get_or_create_familia(db, viaje_id, data.familia_nombre)
    rol = RolViaje.JEFE_FAMILIA if data.es_jefe else RolViaje.MIEMBRO
    persona = Persona(
        nombre=data.nombre,
        email=data.email,
        celular=data.celular,
        es_jefe=data.es_jefe,
        familia_id=familia.id,
        viaje_id=viaje_id,
        rol=rol,
    )
    db.add(persona)
    db.commit()
    db.refresh(persona)
    return {"status": "success", "id": persona.id}


@router.delete("/{viaje_id}/personas/{persona_id}")
def eliminar_persona(
    viaje_id: int,
    persona_id: int,
    access: ViajeAccess = Depends(require_viaje_access()),
    db: Session = Depends(get_db),
):
    persona = (
        db.query(Persona)
        .filter(Persona.id == persona_id, Persona.viaje_id == viaje_id)
        .first()
    )
    if not persona:
        raise HTTPException(404, "Persona no encontrada")
    if not can_delete_persona(access, persona):
        raise HTTPException(403, "No puedes eliminar esta persona")

    persona.is_deleted = True
    db.commit()
    return {"status": "success"}


@router.get("/{viaje_id}/actividades")
def listar_actividades(
    viaje_id: int,
    access: ViajeAccess = Depends(require_viaje_access()),
    db: Session = Depends(get_db),
):
    actividades = db.query(Actividad).filter(Actividad.viaje_id == viaje_id).all()
    if access.rol == RolViaje.ORGANIZADOR:
        return [serialize_actividad(a) for a in actividades]

    familia_id = access.familia_id
    if familia_id is None:
        return []

    persona_ids = {
        p.id
        for p in db.query(Persona).filter(Persona.familia_id == familia_id, Persona.viaje_id == viaje_id).all()
    }
    actividad_ids = {
        part.actividad_id
        for part in db.query(Participacion).filter(Participacion.persona_id.in_(persona_ids)).all()
    }
    return [serialize_actividad(a) for a in actividades if a.id in actividad_ids]


@router.post("/{viaje_id}/actividades", status_code=201)
def crear_actividad(
    viaje_id: int,
    data: ActividadCreate,
    access: ViajeAccess = Depends(require_viaje_access()),
    db: Session = Depends(get_db),
):
    if not can_create_actividad(access):
        raise HTTPException(403, "No puedes crear actividades")

    participantes = (
        db.query(Persona)
        .filter(Persona.id.in_(data.participantes_ids), Persona.viaje_id == viaje_id)
        .all()
    )
    if len(participantes) != len(data.participantes_ids):
        raise HTTPException(400, "Participantes inválidos para este viaje")

    for participante in participantes:
        if not can_select_participante(access, participante):
            raise HTTPException(403, f"No puedes incluir a {participante.nombre}")

    actividad = Actividad(
        nombre=data.nombre,
        fecha=data.fecha,
        costo_total=data.costo_total,
        viaje_id=viaje_id,
        creado_por_usuario_id=access.usuario.id,
    )
    db.add(actividad)
    db.commit()
    db.refresh(actividad)

    if participantes:
        costo_ind = data.costo_total / len(participantes)
        for participante in participantes:
            db.add(
                Participacion(
                    persona_id=participante.id,
                    actividad_id=actividad.id,
                    costo_individual=costo_ind,
                )
            )
        db.commit()

    return {"status": "success", "id": actividad.id}


@router.delete("/{viaje_id}/actividades/{actividad_id}")
def eliminar_actividad(
    viaje_id: int,
    actividad_id: int,
    access: ViajeAccess = Depends(require_viaje_access([RolViaje.ORGANIZADOR, RolViaje.JEFE_FAMILIA])),
    db: Session = Depends(get_db),
):
    actividad = (
        db.query(Actividad)
        .filter(Actividad.id == actividad_id, Actividad.viaje_id == viaje_id)
        .first()
    )
    if not actividad:
        raise HTTPException(404, "Actividad no encontrada")

    if access.rol == RolViaje.JEFE_FAMILIA:
        participaciones = db.query(Participacion).filter(Participacion.actividad_id == actividad_id).all()
        for part in participaciones:
            persona = db.query(Persona).filter(Persona.id == part.persona_id).first()
            if persona and persona.familia_id != access.familia_id:
                raise HTTPException(403, "No puedes eliminar actividades de otras familias")

    db.query(Participacion).filter(Participacion.actividad_id == actividad_id).delete()
    db.delete(actividad)
    db.commit()
    return {"status": "success"}


@router.get("/{viaje_id}/participaciones")
def listar_participaciones(
    viaje_id: int,
    access: ViajeAccess = Depends(require_viaje_access()),
    db: Session = Depends(get_db),
):
    actividad_ids = [a.id for a in db.query(Actividad).filter(Actividad.viaje_id == viaje_id).all()]
    if not actividad_ids:
        return []

    participaciones = db.query(Participacion).filter(Participacion.actividad_id.in_(actividad_ids)).all()
    if access.rol == RolViaje.ORGANIZADOR:
        return [serialize_participacion(p) for p in participaciones]

    familia_id = access.familia_id
    if familia_id is None:
        return []

    persona_ids = {
        p.id
        for p in db.query(Persona).filter(Persona.familia_id == familia_id, Persona.viaje_id == viaje_id).all()
    }
    return [serialize_participacion(p) for p in participaciones if p.persona_id in persona_ids]


@router.put("/{viaje_id}/participaciones/{part_id}")
def actualizar_participacion(
    viaje_id: int,
    part_id: int,
    data: ParticipacionUpdate,
    access: ViajeAccess = Depends(require_viaje_access()),
    db: Session = Depends(get_db),
):
    part = db.query(Participacion).filter(Participacion.id == part_id).first()
    if not part:
        raise HTTPException(404, "Participación no encontrada")

    actividad = db.query(Actividad).filter(Actividad.id == part.actividad_id, Actividad.viaje_id == viaje_id).first()
    if not actividad:
        raise HTTPException(404, "Participación no pertenece a este viaje")

    persona = db.query(Persona).filter(Persona.id == part.persona_id).first()
    if not persona or not can_edit_participacion(access, persona.familia_id):
        raise HTTPException(403, "No puedes editar esta participación")

    if data.costo_individual is not None:
        part.costo_individual = data.costo_individual
    if data.pagado is not None:
        part.pagado = data.pagado

    db.commit()
    return {"status": "success"}