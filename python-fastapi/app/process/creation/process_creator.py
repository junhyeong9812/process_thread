#app/process/creation/process_creator.py
"""
프로세스 생성 및 관리 모듈

"""

import logging

logger = logging.getLogger(__name__)

class ProcessStatus :
    """ 프로세스 상태 열거형 """
    INITIALIZED = "initialized"
    RUNNING = "runnung"
    COMPLETED = "completed"
    TERMINATED = "terminated"
    KILLED = "killed"
    ERROR = "error"
    TIMEOUT = "timeout"

class ProcessCreator :
    pass

class ProcessInfo :
    pass

class ProcessCreationError :
    pass

class ProcessNotFoundError :
    pass

class ProcessTimeoutError :
    pass

class ProcessCommunicationError :
    pass
