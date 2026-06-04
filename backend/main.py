from fastapi import FastAPI, Depends, HTTPException, status, Response
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse
from pydantic import BaseModel, EmailStr
from jose import jwt
import requests
from sqlalchemy.orm import Session
from sqlalchemy import or_
from typing import Optional, List
import logging
from datetime import datetime

# 🔥 BD IMPORTS
from db.base import Base
from db.session import engine, SessionLocal
from db import models 

app = FastAPI(title="AppEstable Diagnostic Engine")

app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_credentials=True, allow_methods=["*"], allow_headers=["*"])
security = HTTPBearer()
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

recent_events = []
def log_event(msg: str):
    entry = f"[{datetime.now().strftime('%H:%M:%S')}] {msg}"
    recent_events.insert(0, entry)
    if len(recent_events) > 15: recent_events.pop()
    logger.info(msg)

def get_db():
    db = SessionLocal()
    try: yield db
    finally: db.close()

# --- AUTH0 ---
AUTH0_DOMAIN = "dev-zbne73xs48twrr2a.us.auth0.com"
API_AUDIENCE = "https://appestable-api"

def verify_token(credentials: HTTPAuthorizationCredentials = Depends(security)):
    token = credentials.credentials
    try:
        url = f"https://{AUTH0_DOMAIN}/.well-known/jwks.json"
        jwks = requests.get(url).json()
        unverified_header = jwt.get_unverified_header(token)
        rsa_key = next(key for key in jwks["keys"] if key["kid"] == unverified_header["kid"])
        return jwt.decode(token, rsa_key, algorithms=["RS256"], audience=API_AUDIENCE, issuer=f"https://{AUTH0_DOMAIN}/")
    except Exception as e:
        log_event(f"❌ Error de Token: {str(e)}")
        raise HTTPException(status_code=401, detail="Token inválido")

# --- MODELOS ---
class PersonaCreate(BaseModel):
    nombre: str
    familia_nombre: str
    email: Optional[str] = None
    celular: Optional[str] = None
    es_jefe: bool = False

class ActividadCreate(BaseModel):
    nombre: str
    fecha: str
    costo_total: float
    participantes_ids: List[int] = []

def serialize_persona(p):
    return {"id": p.id, "nombre": p.nombre, "email": p.email, "celular": p.celular, "es_jefe": p.es_jefe, "familia_nombre": p.familia.nombre_familia if p.familia else "Sin Familia"}

def serialize_participacion(p):
    return {
        "id": p.id, 
        "persona_id": p.persona_id, 
        "actividad_id": p.actividad_id, 
        "costo_individual": float(p.costo_individual), 
        "pagado": p.pagado
    }

def serialize_actividad(a):
    return {"id": a.id, "nombre": a.nombre, "fecha": a.fecha, "costo_total": float(a.costo_total)}

# --- DASHBOARD ---
@app.get("/status", response_class=HTMLResponse)
def status_dashboard(db: Session = Depends(get_db)):
    try:
        p_count = db.query(models.Persona).count()
        part_count = db.query(models.Participacion).count()
        db_state = "✅ CONECTADA"
    except Exception as e:
        p_count, part_count, db_state = 0, 0, f"❌ ERROR: {e}"

    events = "".join([f"<li>{e}</li>" for e in recent_events])
    return f"""
    <body style="font-family:sans-serif; background:#f4f7f6; padding:30px;">
        <h1>🚀 Panel de Control</h1>
        <div style="display:flex; gap:20px;">
            <div style="background:white; padding:20px; border-radius:10px; flex:1; box-shadow:0 2px 5px rgba(0,0,0,0.1);">
                <h3>📊 Estadísticas</h3>
                <p>Personas: <b>{p_count}</b></p>
                <p>Gastos registrados: <b style="color:red;">{part_count}</b></p>
                <hr>
                <a href="/debug/raw" style="color:blue;">🔍 Ver JSON de Datos</a>
            </div>
            <div style="background:#2d3436; color:#55efc4; padding:20px; border-radius:10px; flex:2;">
                <h3>📡 Logs en vivo</h3>
                <ul style="list-style:none; padding:0;">{events if events else '<li>Sin actividad...</li>'}</ul>
            </div>
        </div>
    </body>
    """

@app.get("/debug/raw")
def get_raw_data(db: Session = Depends(get_db)):
    return {
        "personas": [serialize_persona(p) for p in db.query(models.Persona).all()],
        "participaciones": [serialize_participacion(p) for p in db.query(models.Participacion).all()],
        "actividades": [serialize_actividad(a) for a in db.query(models.Actividad).all()]
    }

# --- ENDPOINTS ---
@app.get("/personas")
def list_personas(db: Session = Depends(get_db)):
    personas = db.query(models.Persona).filter(or_(models.Persona.is_deleted == False, models.Persona.is_deleted.is_(None))).all()
    return [serialize_persona(p) for p in personas]

@app.post("/persona", status_code=201)
def create_persona(data: PersonaCreate, db: Session = Depends(get_db), token: dict = Depends(verify_token)):
    log_event(f"📥 Registro: {data.nombre}")
    fam = db.query(models.Familia).filter(models.Familia.nombre_familia.ilike(data.familia_nombre.strip())).first()
    if not fam:
        fam = models.Familia(nombre_familia=data.familia_nombre.strip())
        db.add(fam); db.commit(); db.refresh(fam)
    nueva = models.Persona(nombre=data.nombre, email=data.email, celular=data.celular, es_jefe=data.es_jefe, familia_id=fam.id)
    db.add(nueva); db.commit(); db.refresh(nueva)
    return {"status": "success", "id": nueva.id}

@app.get("/actividades")
def list_actividades(db: Session = Depends(get_db)):
    return [serialize_actividad(a) for a in db.query(models.Actividad).all()]

@app.post("/actividades", status_code=201)
@app.post("/actividad", status_code=201)
def create_actividad(data: ActividadCreate, db: Session = Depends(get_db), token: dict = Depends(verify_token)):
    log_event(f"📝 Gasto: {data.nombre} (${data.costo_total})")
    nueva = models.Actividad(nombre=data.nombre, fecha=data.fecha, costo_total=data.costo_total)
    db.add(nueva); db.commit(); db.refresh(nueva)
    
    if data.participantes_ids:
        costo_ind = data.costo_total / len(data.participantes_ids)
        for p_id in data.participantes_ids:
            part = models.Participacion(persona_id=p_id, actividad_id=nueva.id, costo_individual=costo_ind)
            db.add(part)
        db.commit()
    return {"status": "success", "id": nueva.id}

@app.get("/participaciones")
def list_participaciones(db: Session = Depends(get_db)):
    return [serialize_participacion(p) for p in db.query(models.Participacion).all()]

@app.put("/participaciones/{part_id}")
def update_pago(part_id: int, data: dict, db: Session = Depends(get_db), token: dict = Depends(verify_token)):
    part = db.query(models.Participacion).filter(models.Participacion.id == part_id).first()
    if part and "pagado" in data:
        part.pagado = data["pagado"]
        db.commit()
    return {"status": "ok"}

@app.on_event("startup")
def startup(): Base.metadata.create_all(bind=engine); log_event("🚀 Motor Iniciado")

@app.get("/")
def health(): return {"status": "ok"}
