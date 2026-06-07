import logging

from sqlalchemy import text
from sqlalchemy.orm import Session

logger = logging.getLogger(__name__)

_TABLES_WITH_SERIAL = (
    "viajes",
    "usuarios",
    "membresias_viaje",
    "familias",
    "personas",
    "actividades",
    "participaciones",
)


def fix_postgres_sequences(db: Session) -> None:
    """Corrige secuencias tras inserts manuales en migraciones (evita duplicate key)."""
    bind = db.get_bind()
    if bind.dialect.name != "postgresql":
        return

    for table in _TABLES_WITH_SERIAL:
        try:
            db.execute(
                text(
                    "SELECT setval(pg_get_serial_sequence(:table, 'id'), "
                    "COALESCE((SELECT MAX(id) FROM " + table + "), 1))"
                ),
                {"table": table},
            )
        except Exception as exc:
            logger.warning("No se pudo ajustar secuencia de %s: %s", table, exc)
    db.commit()