"""Thread API endpoints"""

from typing import Dict, Any
from fastapi import APIRouter

router = APIRouter()


@router.post("/create")
async def create_thread() -> Dict[str, Any]:
    """Create a new thread"""
    return {"message": "Thread creation endpoint"}


@router.get("/async-demo")
async def async_demo() -> Dict[str, Any]:
    """Demonstrate async/await"""
    import asyncio
    await asyncio.sleep(0.1)
    return {"type": "async", "status": "completed"}