import os
from functools import lru_cache

from dotenv import load_dotenv

load_dotenv()


@lru_cache
def get_settings():
    return Settings()


class Settings:
    def __init__(self):
        self.database_url: str = os.getenv(
            "DATABASE_URL",
            "postgresql://postgres:postgres123@localhost:5432/appestable",
        )
        self.auth0_domain: str = os.getenv(
            "AUTH0_DOMAIN",
            "dev-zbne73xs48twrr2a.us.auth0.com",
        )
        self.auth0_audience: str = os.getenv(
            "AUTH0_AUDIENCE",
            "https://appestable-api",
        )
        self.cors_origins: list[str] = [
            o.strip()
            for o in os.getenv("CORS_ORIGINS", "*").split(",")
            if o.strip()
        ]
        self.log_level: str = os.getenv("LOG_LEVEL", "INFO").upper()
        self.disable_auth: bool = os.getenv("DISABLE_AUTH", "false").lower() in (
            "1",
            "true",
            "yes",
        )

    @property
    def auth0_issuer(self) -> str:
        return f"https://{self.auth0_domain}/"