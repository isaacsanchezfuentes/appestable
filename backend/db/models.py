from sqlalchemy import Column, Integer, String, Float, Boolean, ForeignKey, DateTime
from sqlalchemy.orm import relationship
from datetime import datetime
from .base import Base

class Familia(Base):
    __tablename__ = "familias"

    id = Column(Integer, primary_key=True, index=True)
    nombre_familia = Column(String, unique=True, index=True)

    # Relación: Una familia tiene muchas personas
    personas = relationship("Persona", back_populates="familia")

class Persona(Base):
    __tablename__ = "personas"

    id = Column(Integer, primary_key=True, index=True)
    nombre = Column(String, index=True)
    email = Column(String, unique=True, index=True)
    celular = Column(String)
    es_jefe = Column(Boolean, default=False)
    auth0_id = Column(String, unique=True, index=True, nullable=True) # El 'sub' de Auth0 (nullable para permitir creación por admin)
    
    familia_id = Column(Integer, ForeignKey("familias.id"))
    is_deleted = Column(Boolean, default=False)
    
    # Relaciones
    familia = relationship("Familia", back_populates="personas")
    participaciones = relationship(
        "Participacion",
        back_populates="persona",
    )

class Actividad(Base):
    __tablename__ = "actividades"

    id = Column(Integer, primary_key=True, index=True)
    nombre = Column(String, index=True)
    fecha = Column(String) # Guardamos como string para coincidir con tu app o DateTime
    costo_total = Column(Float)
    fecha_creacion = Column(DateTime, default=datetime.utcnow)

    # Relación: Una actividad tiene muchas participaciones
    participaciones = relationship(
        "Participacion",
        back_populates="actividad",
    )

class Participacion(Base):
    __tablename__ = "participaciones"

    id = Column(Integer, primary_key=True, index=True)
    costo_individual = Column(Float)
    pagado = Column(Boolean, default=False)
    persona_id = Column(Integer, ForeignKey("personas.id"))
    actividad_id = Column(Integer, ForeignKey("actividades.id"))

    # Relaciones
    persona = relationship("Persona", back_populates="participaciones")
    actividad = relationship("Actividad", back_populates="participaciones")
