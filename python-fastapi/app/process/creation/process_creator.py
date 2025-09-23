#app/process/creation/process_creator.py
"""
프로세스 생성 및 관리 모듈

"""

import logging
from enum import Enum
from typing import Dict,List,Optional,Tuple,Any,Union

logger = logging.getLogger(__name__)

class ProcessStatus(Enum) :
    """ 프로세스 상태 열거형 """
    INITIALIZED = "initialized"
    RUNNING = "runnung"
    COMPLETED = "completed"
    TERMINATED = "terminated"
    KILLED = "killed"
    ERROR = "error"
    TIMEOUT = "timeout"
    
    def is_active(self) -> bool :
        """ 활성 상태인지 확인 """
        return self in [ProcessStatus.INITIALIZED,ProcessStatus.RUNNING]
    
    def is_finished(self) -> bool :
        """ 종료 상태인지 확인 """
        return self in [
            ProcessStatus.COMPLETED,
            ProcessStatus.TERMINATED,
            ProcessStatus.KILLED,
            ProcessStatus.ERROR,
            ProcessStatus.TIMEOUT
        ]

class ProcessCreator :
    pass

class ProcessInfo :
    """ 프로세스 정보 데이터 클래스"""
    pid : int
    command = List[str]
    popen: Optional[subprocess.Popen] = None
    status:ProcessStatus = ProcessStatus.INITIALIZED
    

    pass

class ProcessCreationError :
    pass

class ProcessNotFoundError :
    pass

class ProcessTimeoutError :
    pass

class ProcessCommunicationError :
    pass
