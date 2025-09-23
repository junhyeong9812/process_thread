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

    def test_is_active_status(self):
        """ 활성 상태 판별 메서드 테스트 """
        assert ProcessStatus.INITIALIZED.is_active() == True
        assert ProcessStatus.RUNNING.is_active() == True
        assert ProcessStatus.COMPLETED.is_active() == False
        assert ProcessStatus.TERMINATED.is_active() == False
        assert ProcessStatus.KILLED.is_active() == False
        

    def test_is_finished_status(self):
        """ 종료 상태 판별 메서드 테스트 """
        assert ProcessStatus.COMPLETED.is_finished() == True
        assert ProcessStatus.TERMINATED.is_finished() == True
        assert ProcessStatus.KILLED.is_finished() == True
        assert ProcessStatus.ERROR.is_finished() == True
        assert ProcessStatus.RUNNING.is_finished() == False
        
class TestProcessInfo:
    """ProcessInfo 데이터 클래스 테스트"""
    
    def test_process_info_creation_minimal(self):
        """최소 정보로 ProcessInfo 생성"""
        info = ProcessInfo(pid=12345, command=["python","--version"])

        assert info.pid == 12345
        assert info.command == ["python","--version"]
        assert info.status == ProcessStatus.INITIALIZED
        assert info.popen is None
        assert info.created_at is None
        assert info.ended_at is None

