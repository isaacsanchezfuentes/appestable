import enum
from datetime import datetime

from sqlalchemy import (
    Boolean,
    Column,
    DateTime,
    Enum,
    Float,
    ForeignKey,
    Integer,
    String,
    UniqueConstraint,
)
from sqlalchemy.orm import relationship

from .base import Base


class RolViaje(str, enum.Enum):
    ORGANIZADOR = "ORGANIZADOR"
    JEFE_FAMILIA = "JEFE_FAMILIA"
    MIEMBRO = "MIEMBRO"


class Usuario(Base):
    __tablename__ = "usuarios"

    id = Column(Integer, primary_key=True, index=True)
    auth0_id = Column(String, unique=True, index=True, nullable=False)
    email = Column(String, unique=True, index=True, nullable=False)
    nombre = Column(String, nullable=False)

    membresias = relationship("MembresiaViaje", back_populates="usuario")


class Viaje(Base):
    __tablename__ = "viajes"

    id = Column(Integer, primary_key=True, index=True)
    nombre = Column(String, nullable=False)
    descripcion = Column(String, default="")
    fecha_inicio = Column(String, default="")
    fecha_fin = Column(String, default="")
    estado = Column(String, default="ACTIVO")
    organizador_usuario_id = Column(Integer, ForeignKey("usuarios.id"), nullable=True)

    organizador = relationship("Usuario", foreign_keys=[organizador_usuario_id])
    familias = relationship("Familia", back_populates="viaje")
    personas = relationship("Persona", back_populates="viaje")
    actividades = relationship("Actividad", back_populates="viaje")
    membresias = relationship("MembresiaViaje", back_populates="viaje")


class MembresiaViaje(Base):
    __tablename__ = "membresias_viaje"
    __table_args__ = (
        UniqueConstraint("viaje_id", "usuario_id", name="uq_membresia_viaje_usuario"),
    )

    id = Column(Integer, primary_key=True, index=True)
    viaje_id = Column(Integer, ForeignKey("viajes.id"), nullable=False)
    usuario_id = Column(Integer, ForeignKey("usuarios.id"), nullable=False)
    familia_id = Column(Integer, ForeignKey("familias.id"), nullable=True)
    rol = Column(Enum(RolViaje), nullable=False)

    viaje = relationship("Viaje", back_populates="membresias")
    usuario = relationship("Usuario", back_populates="membresias")
    familia = relationship("Familia")


class Familia(Base):
    __tablename__ = "familias"
    __table_args__ = (
        UniqueConstraint("viaje_id", "nombre_familia", name="uq_familia_viaje_nombre"),
    )

    id = Column(Integer, primary_key=True, index=True)
    nombre_familia = Column(String, index=True, nullable=False)
    viaje_id = Column(Integer, ForeignKey("viajes.id"), nullable=False, default=1)

    viaje = relationship("Viaje", back_populates="familias")
    personas = relationship("Persona", back_populates="familia")


class Persona(Base):
    __tablename__ = "personas"

    id = Column(Integer, primary_key=True, index=True)
    nombre = Column(String, index=True, nullable=False)
    email = Column(String, unique=True, index=True, nullable=True)
    celular = Column(String)
    es_jefe = Column(Boolean, default=False)
    auth0_id = Column(String, unique=True, index=True, nullable=True)
    familia_id = Column(Integer, ForeignKey("familias.id"), nullable=False)
    is_deleted = Column(Boolean, default=False)
    viaje_id = Column(Integer, ForeignKey("viajes.id"), nullable=False, default=1)
    usuario_id = Column(Integer, ForeignKey("usuarios.id"), nullable=True)
    rol = Column(Enum(RolViaje), default=RolViaje.MIEMBRO, nullable=False)

    familia = relationship("Familia", back_populates="personas")
    viaje = relationship("Viaje", back_populates="personas")
    participaciones = relationship("Participacion", back_populates="persona")


class Actividad(Base):
    __tablename__ = "actividades"

    id = Column(Integer, primary_key=True, index=True)
    nombre = Column(String, index=True, nullable=False)
    fecha = Column(String, nullable=False)
    costo_total = Column(Float, nullable=False)
    fecha_creacion = Column(DateTime, default=datetime.utcnow)
    viaje_id = Column(Integer, ForeignKey("viajes.id"), nullable=False, default=1)
    creado_por_usuario_id = Column(Integer, ForeignKey("usuarios.id"), nullable=True)

    viaje = relationship("Viaje", back_populates="actividades")
    participaciones = relationship("Participacion", back_populates="actividad")


class Participacion(Base):
    __tablename__ = "participaciones"

    id = Column(Integer, primary_key=True, index=True)
    costo_individual = Column(Float, nullable=False)
    pagado = Column(Boolean, default=False)
    persona_id = Column(Integer, ForeignKey("personas.id"), nullable=False)
    actividad_id = Column(Integer, ForeignKey("actividades.id"), nullable=False)

    persona = relationship("Persona", back_populates="participaciones")
    actividad = relationship("Actividad", back_populates="participaciones")