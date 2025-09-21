"""Comparison API endpoints"""

from typing import Dict, Any
from fastapi import APIRouter

router = APIRouter()


@router.get("/metrics")
async def get_comparison_metrics() -> Dict[str, Any]:
    """Get comparison metrics"""
    return {"message": "Comparison metrics endpoint"}