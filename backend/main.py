import logging
from datetime import datetime

from fastapi import Depends, FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse
from sqlalchemy import or_
from sqlalchemy.orm import Session

from auth import get_or_create_usuario, require_global_admin, verify_token
from config import get_settings
from db import models
from db.session import get_db
from permissions import ViajeAccess, get_membresia
from routers import me, viajes
from services.db_maintenance import fix_postgres_sequences
from schemas import ActividadCreate, ParticipacionUpdate, PersonaCreate
from serializers import serialize_actividad, serialize_participacion, serialize_persona

settings = get_settings()

app = FastAPI(title="AppEstable API v1")

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(me.router)
app.include_router(viajes.router)

logging.basicConfig(level=getattr(logging, settings.log_level, logging.INFO))
logger = logging.getLogger(__name__)
recent_events: list[str] = []


def log_event(msg: str) -> None:
    entry = f"[{datetime.now().strftime('%H:%M:%S')}] {msg}"
    recent_events.insert(0, entry)
    if len(recent_events) > 15:
        recent_events.pop()
    logger.info(msg)


DEFAULT_VIAJE_ID = 1


def _legacy_access(db: Session, usuario: models.Usuario, viaje_id: int = DEFAULT_VIAJE_ID) -> ViajeAccess:
    membresia = get_membresia(db, viaje_id, usuario.id)
    if not membresia:
        membresia = models.MembresiaViaje(
            viaje_id=viaje_id,
            usuario_id=usuario.id,
            familia_id=None,
            rol=models.RolViaje.ORGANIZADOR,
        )
        db.add(membresia)
        db.commit()
        db.refresh(membresia)
        log_event(f"Membresía ORGANIZADOR auto-creada para usuario {usuario.email} en viaje {viaje_id}")
    return ViajeAccess(membresia=membresia, usuario=usuario)


@app.get("/status", response_class=HTMLResponse)
def status_dashboard(db: Session = Depends(get_db)):
    try:
        p_count = db.query(models.Persona).count()
        v_count = db.query(models.Viaje).count()
        part_count = db.query(models.Participacion).count()
        db_state = "✅ ACTIVA"
    except Exception as exc:
        p_count, v_count, part_count, db_state = 0, 0, 0, f"❌ ERROR: {exc}"

    events = "".join([f"<li>{e}</li>" for e in recent_events])
    return f"""
    <body style="font-family:sans-serif; background:#f4f7f6; padding:30px;">
        <h1>🚀 AppEstable API v1</h1>
        <div style="display:flex; gap:20px;">
            <div style="background:white; padding:20px; border-radius:10px; flex:1;">
                <h3>📊 Estadísticas</h3>
                <p>Base de Datos: <b>{db_state}</b></p>
                <p>Viajes: <b>{v_count}</b></p>
                <p>Personas: <b>{p_count}</b></p>
                <p>Participaciones: <b>{part_count}</b></p>
            </div>
            <div style="background:#2d3436; color:#55efc4; padding:20px; border-radius:10px; flex:2;">
                <h3>📡 Logs en vivo</h3>
                <ul style="list-style:none; padding:0;">{events or '<li>Sin actividad...</li>'}</ul>
            </div>
        </div>
    </body>
    """


@app.get("/")
def health():
    return {"status": "ok", "version": "v1"}


# --- Legacy endpoints (compatibilidad temporal, requieren JWT) ---

@app.get("/personas")
def legacy_listar_personas(
    viaje_id: int = DEFAULT_VIAJE_ID,
    usuario: models.Usuario = Depends(get_or_create_usuario),
    db: Session = Depends(get_db),
):
    access = _legacy_access(db, usuario, viaje_id)
    personas = (
        db.query(models.Persona)
        .filter(
            models.Persona.viaje_id == viaje_id,
            or_(models.Persona.is_deleted.is_(False), models.Persona.is_deleted.is_(None)),
        )
        .all()
    )
    from permissions import can_view_familia

    return [serialize_persona(p) for p in personas if can_view_familia(access, p.familia_id)]


@app.post("/persona", status_code=201)
def legacy_crear_persona(
    data: PersonaCreate,
    viaje_id: int = DEFAULT_VIAJE_ID,
    usuario: models.Usuario = Depends(get_or_create_usuario),
    db: Session = Depends(get_db),
):
    access = _legacy_access(db, usuario, viaje_id)
    return viajes.crear_persona(viaje_id, data, access, db)


@app.post("/personas/admin", status_code=201)
def legacy_crear_persona_admin(
    data: PersonaCreate,
    viaje_id: int = DEFAULT_VIAJE_ID,
    admin: dict = Depends(require_global_admin),
    db: Session = Depends(get_db),
):
    return viajes.crear_persona_admin(viaje_id, data, admin, db)


@app.get("/actividades")
def legacy_listar_actividades(
    viaje_id: int = DEFAULT_VIAJE_ID,
    usuario: models.Usuario = Depends(get_or_create_usuario),
    db: Session = Depends(get_db),
):
    access = _legacy_access(db, usuario, viaje_id)
    return viajes.listar_actividades(viaje_id, access, db)


@app.post("/actividades", status_code=201)
def legacy_crear_actividad(
    data: ActividadCreate,
    viaje_id: int = DEFAULT_VIAJE_ID,
    usuario: models.Usuario = Depends(get_or_create_usuario),
    db: Session = Depends(get_db),
):
    access = _legacy_access(db, usuario, viaje_id)
    return viajes.crear_actividad(viaje_id, data, access, db)


@app.get("/participaciones")
def legacy_listar_participaciones(
    viaje_id: int = DEFAULT_VIAJE_ID,
    usuario: models.Usuario = Depends(get_or_create_usuario),
    db: Session = Depends(get_db),
):
    access = _legacy_access(db, usuario, viaje_id)
    return viajes.listar_participaciones(viaje_id, access, db)


@app.put("/participaciones/{part_id}")
def legacy_actualizar_participacion(
    part_id: int,
    data: ParticipacionUpdate,
    viaje_id: int = DEFAULT_VIAJE_ID,
    usuario: models.Usuario = Depends(get_or_create_usuario),
    db: Session = Depends(get_db),
):
    access = _legacy_access(db, usuario, viaje_id)
    return viajes.actualizar_participacion(viaje_id, part_id, data, access, db)


@app.on_event("startup")
def startup():
    db = next(get_db())
    try:
        fix_postgres_sequences(db)
    finally:
        db.close()
    log_event("🚀 AppEstable API v1 iniciada")