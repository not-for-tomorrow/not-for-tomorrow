import json
import time
from typing import Any, Dict, Optional
from urllib import request


def post_json_with_retry(
    url: str,
    payload: Dict[str, Any],
    timeout_seconds: int = 5,
    max_attempts: int = 3,
    base_backoff_seconds: float = 0.4,
) -> Dict[str, Any]:
    body = json.dumps(payload).encode("utf-8")
    last_error: Optional[str] = None

    for attempt in range(1, max_attempts + 1):
        req = request.Request(
            url,
            data=body,
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        try:
            with request.urlopen(req, timeout=timeout_seconds) as resp:
                ok = 200 <= resp.status < 300
                if ok:
                    return {
                        "delivered": True,
                        "httpStatus": resp.status,
                        "attempts": attempt,
                    }
                last_error = f"http_status={resp.status}"
        except Exception as ex:
            last_error = str(ex)

        if attempt < max_attempts:
            sleep_seconds = base_backoff_seconds * (2 ** (attempt - 1))
            time.sleep(sleep_seconds)

    return {
        "delivered": False,
        "reason": last_error or "unknown delivery error",
        "attempts": max_attempts,
    }
