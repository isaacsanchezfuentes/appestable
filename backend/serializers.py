from db.models import Actividad, Participacion, Persona


def serialize_persona(persona: Persona) -> dict:
    return {
        "id": persona.id,
        "nombre": persona.nombre,
        "email": persona.email,
        "celular": persona.celular,
        "es_jefe": persona.es_jefe,
        "familia_id": persona.familia_id,
        "familia_nombre": persona.familia.nombre_familia if persona.familia else "Sin Familia",
        "viaje_id": persona.viaje_id,
        "rol": persona.rol.value if persona.rol else "MIEMBRO",
        "auth0_id": persona.auth0_id,
    }


def serialize_actividad(actividad: Actividad) -> dict:
    return {
        "id": actividad.id,
        "nombre": actividad.nombre,
        "fecha": actividad.fecha,
        "costo_total": actividad.costo_total,
        "viaje_id": actividad.viaje_id,
        "creado_por_usuario_id": actividad.creado_por_usuario_id,
    }


def serialize_participacion(part: Participacion) -> dict:
    return {
        "id": part.id,
        "persona_id": part.persona_id,
        "actividad_id": part.actividad_id,
        "costo_individual": part.costo_individual,
        "pagado": part.pagado,
    }