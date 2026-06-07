from typing import List, Optional

from pydantic import BaseModel, Field


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
    participantes_ids: List[int] = Field(default_factory=list)


class ParticipacionUpdate(BaseModel):
    costo_individual: Optional[float] = None
    pagado: Optional[bool] = None


class ViajeCreate(BaseModel):
    nombre: str
    descripcion: str = ""
    fecha_inicio: str = ""
    fecha_fin: str = ""


class MembresiaOut(BaseModel):
    viaje_id: int
    viaje_nombre: str
    rol: str
    familia_id: Optional[int] = None
    familia_nombre: Optional[str] = None


class UsuarioOut(BaseModel):
    id: int
    email: str
    nombre: str
    auth0_id: str


class MeResponse(BaseModel):
    usuario: UsuarioOut
    membresias: List[MembresiaOut]


class ViajeOut(BaseModel):
    id: int
    nombre: str
    descripcion: str
    fecha_inicio: str
    fecha_fin: str
    estado: str
    organizador_usuario_id: Optional[int] = None