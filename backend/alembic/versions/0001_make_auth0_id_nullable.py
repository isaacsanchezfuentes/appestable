"""make auth0_id nullable

Revision ID: 0001_make_auth0_id_nullable
Revises: 
Create Date: 2026-05-19 00:00:00.000000
"""
from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision = '0001_make_auth0_id_nullable'
down_revision = None
branch_labels = None
depends_on = None


def upgrade():
    # Make auth0_id nullable
    op.alter_column('personas', 'auth0_id', existing_type=sa.String(), nullable=True)


def downgrade():
    # Revert: make auth0_id NOT NULL
    op.alter_column('personas', 'auth0_id', existing_type=sa.String(), nullable=False)
