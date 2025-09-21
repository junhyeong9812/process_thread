#Test/unit/process/creation/test_process_creator.py

"""
ProcessCreator 완전 테스트 스위트
TDD를 위한 모든 테스트 케이스 포함
각 테스트는 하나의 구체적인 기능을 검증
"""

import pytest
import subprocess
import os
import sys
import time
import tempfile
import platform
from pathlib import Path
from typing import List, Dict, Optional
from unittest.mock import Mock, patch, MagicMock, call
import psutil
import signal

from app.process.creation.process_creator import (
    ProcessCreator,
    ProcessInfo,
    ProcessStatus,
    ProcessCreationError,
    ProcessNotFoundError,
    ProcessTimeoutError,
    ProcessCommunicationError
)

class TestProcessStatus :
    """ ProcessStatus 열거형 테스트 """

    def test_process_status_values(self):
        """모든 프로세스 상태값이 정의되어 있는지 확인"""
        expected_statuses = [
            'INITIALIZED', 'RUNNING', 'COMPLETED', 
            'TERMINATED', 'KILLED', 'ERROR', 'TIMEOUT'
        ]
        for status in expected_statuses:
            assert hasattr(ProcessStatus, status)
    

