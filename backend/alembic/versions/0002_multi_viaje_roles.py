"""multi viaje roles schema

Revision ID: 0002_multi_viaje_roles
Revises: 0001_make_auth0_id_nullable
Create Date: 2026-06-07 00:00:00.000000
"""
from alembic import op
import sqlalchemy as sa

revision = "0002_multi_viaje_roles"
down_revision = "0001_make_auth0_id_nullable"
branch_labels = None
depends_on = None

rol_enum = sa.Enum("ORGANIZADOR", "JEFE_FAMILIA", "MIEMBRO", name="rolviaje")


def upgrade():
    op.create_table(
        "usuarios",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("auth0_id", sa.String(), nullable=False),
        sa.Column("email", sa.String(), nullable=False),
        sa.Column("nombre", sa.String(), nullable=False),
    )
    op.create_index("ix_usuarios_auth0_id", "usuarios", ["auth0_id"], unique=True)
    op.create_index("ix_usuarios_email", "usuarios", ["email"], unique=True)

    op.create_table(
        "viajes",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("nombre", sa.String(), nullable=False),
        sa.Column("descripcion", sa.String(), server_default=""),
        sa.Column("fecha_inicio", sa.String(), server_default=""),
        sa.Column("fecha_fin", sa.String(), server_default=""),
        sa.Column("estado", sa.String(), server_default="ACTIVO"),
        sa.Column("organizador_usuario_id", sa.Integer(), sa.ForeignKey("usuarios.id"), nullable=True),
    )

    op.execute(
        "INSERT INTO viajes (id, nombre, descripcion, estado) "
        "VALUES (1, 'Viaje Principal', 'Migrado automáticamente', 'ACTIVO')"
    )

    op.add_column("familias", sa.Column("viaje_id", sa.Integer(), server_default="1", nullable=False))
    op.create_foreign_key("fk_familias_viaje_id", "familias", "viajes", ["viaje_id"], ["id"])

    try:
        op.drop_constraint("familias_nombre_familia_key", "familias", type_="unique")
    except Exception:
        pass
    op.create_unique_constraint("uq_familia_viaje_nombre", "familias", ["viaje_id", "nombre_familia"])

    rol_enum.create(op.get_bind(), checkfirst=True)

    op.add_column("personas", sa.Column("viaje_id", sa.Integer(), server_default="1", nullable=False))
    op.add_column("personas", sa.Column("usuario_id", sa.Integer(), nullable=True))
    op.add_column(
        "personas",
        sa.Column("rol", rol_enum, server_default="MIEMBRO", nullable=False),
    )
    op.create_foreign_key("fk_personas_viaje_id", "personas", "viajes", ["viaje_id"], ["id"])
    op.create_foreign_key("fk_personas_usuario_id", "personas", "usuarios", ["usuario_id"], ["id"])
    op.execute(
        "UPDATE personas SET rol = CASE WHEN es_jefe THEN 'JEFE_FAMILIA'::rolviaje "
        "ELSE 'MIEMBRO'::rolviaje END"
    )

    op.add_column("actividades", sa.Column("viaje_id", sa.Integer(), server_default="1", nullable=False))
    op.add_column("actividades", sa.Column("creado_por_usuario_id", sa.Integer(), nullable=True))
    op.create_foreign_key("fk_actividades_viaje_id", "actividades", "viajes", ["viaje_id"], ["id"])
    op.create_foreign_key(
        "fk_actividades_creado_por_usuario_id",
        "actividades",
        "usuarios",
        ["creado_por_usuario_id"],
        ["id"],
    )

    op.create_table(
        "membresias_viaje",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("viaje_id", sa.Integer(), sa.ForeignKey("viajes.id"), nullable=False),
        sa.Column("usuario_id", sa.Integer(), sa.ForeignKey("usuarios.id"), nullable=False),
        sa.Column("familia_id", sa.Integer(), sa.ForeignKey("familias.id"), nullable=True),
        sa.Column("rol", rol_enum, nullable=False),
        sa.UniqueConstraint("viaje_id", "usuario_id", name="uq_membresia_viaje_usuario"),
    )


def downgrade():
    op.drop_table("membresias_viaje")
    op.drop_constraint("fk_actividades_creado_por_usuario_id", "actividades", type_="foreignkey")
    op.drop_constraint("fk_actividades_viaje_id", "actividades", type_="foreignkey")
    op.drop_column("actividades", "creado_por_usuario_id")
    op.drop_column("actividades", "viaje_id")

    op.drop_constraint("fk_personas_usuario_id", "personas", type_="foreignkey")
    op.drop_constraint("fk_personas_viaje_id", "personas", type_="foreignkey")
    op.drop_column("personas", "rol")
    op.drop_column("personas", "usuario_id")
    op.drop_column("personas", "viaje_id")

    op.drop_constraint("uq_familia_viaje_nombre", "familias", type_="unique")
    op.create_unique_constraint("familias_nombre_familia_key", "familias", ["nombre_familia"])
    op.drop_constraint("fk_familias_viaje_id", "familias", type_="foreignkey")
    op.drop_column("familias", "viaje_id")

    op.drop_table("viajes")
    op.drop_table("usuarios")
    rol_enum.drop(op.get_bind(), checkfirst=True)