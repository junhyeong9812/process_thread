"""Application configuration"""

from typing import Optional
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """Application settings"""

    APP_NAME: str = "process-thread-fastapi"
    VERSION: str = "1.0.0"
    DEBUG: bool = True

    API_V1_STR: str = "/api/python"

    # Server
    HOST: str = "0.0.0.0"
    PORT: int = 8000
    WORKERS: int = 1

    # Process settings
    MAX_PROCESSES: int = 10
    PROCESS_TIMEOUT: int = 30

    # Thread settings
    MAX_THREADS: int = 100
    THREAD_POOL_SIZE: int = 10

    # Monitoring
    ENABLE_METRICS: bool = True

    class Config:
        env_file = ".env"
        case_sensitive = True


settings = Settings()