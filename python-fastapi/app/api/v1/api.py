"""API router aggregation"""

from fastapi import APIRouter

from app.api.v1.endpoints import process, thread, comparison

api_router = APIRouter()

api_router.include_router(
    process.router,
    prefix="/process",
    tags=["process"]
)

api_router.include_router(
    thread.router,
    prefix="/thread",
    tags=["thread"]
)

api_router.include_router(
    comparison.router,
    prefix="/comparison",
    tags=["comparison"]
)