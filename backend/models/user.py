from sqlalchemy import Column, String
from db.base import Base

class User(Base):
    __tablename__ = "users"

    id = Column(String, primary_key=True)
    auth0_id = Column(String, unique=True, index=True)
    email = Column(String, unique=True, index=True)
    name = Column(String, nullable=True)
    role = Column(String, default="user")