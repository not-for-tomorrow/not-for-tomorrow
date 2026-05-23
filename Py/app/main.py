from datetime import datetime, timezone
import os
from typing import Any, Dict, List
from fastapi import FastAPI
from app.alert_formatter import build_details, build_summary
from app.dead_letter_store import (
    append_dead_letter,
    compact_dead_letters,
    is_enabled as dead_letter_file_enabled,
    mark_processed,
    read_recent_dead_letters,
    read_recent_unprocessed_dead_letters,
)
from app.notifier_client import post_json_with_retry

app = FastAPI(title="superapp-py")
_alert_events: List[Dict[str, Any]] = []
_dead_letters: List[Dict[str, Any]] = []
_notifier_mode = os.getenv("ALERT_NOTIFIER_MODE", "log").strip().lower()
_slack_webhook_url = os.getenv("SLACK_WEBHOOK_URL", "").strip()
_teams_webhook_url = os.getenv("TEAMS_WEBHOOK_URL", "").strip()


@app.get("/health")
def health():
    return {"status": "ok"}


def _send_to_notifier(channel: str, payload: Dict[str, Any]) -> Dict[str, Any]:
    if _notifier_mode == "log":
        return {"mode": "log", "delivered": True}

    if _notifier_mode == "slack":
        if not _slack_webhook_url:
            return {"mode": "slack", "delivered": False, "reason": "missing SLACK_WEBHOOK_URL"}
        text = build_summary(channel, payload)
        result = post_json_with_retry(
            _slack_webhook_url,
            {"text": text},
            timeout_seconds=5,
            max_attempts=3,
            base_backoff_seconds=0.4,
        )
        result["mode"] = "slack"
        return result

    if _notifier_mode == "teams":
        if not _teams_webhook_url:
            return {"mode": "teams", "delivered": False, "reason": "missing TEAMS_WEBHOOK_URL"}
        title = build_summary(channel, payload)
        details = build_details(payload)
        result = post_json_with_retry(
            _teams_webhook_url,
            {
                "@type": "MessageCard",
                "@context": "https://schema.org/extensions",
                "summary": "SuperApp Alert",
                "themeColor": "E81123" if "CRITICAL" in title else "0078D7",
                "title": title,
                "text": details,
            },
            timeout_seconds=5,
            max_attempts=3,
            base_backoff_seconds=0.4,
        )
        result["mode"] = "teams"
        return result

    return {"mode": _notifier_mode, "delivered": False, "reason": "unsupported mode"}


@app.post("/alerts/default")
def alerts_default(payload: Dict[str, Any]):
    notify = _send_to_notifier("default", payload)
    event = {
        "channel": "default",
        "receivedAt": datetime.now(timezone.utc).isoformat(),
        "payload": payload,
        "notify": notify,
    }
    _alert_events.append(event)
    if len(_alert_events) > 200:
        del _alert_events[0]
    if not notify.get("delivered"):
        _dead_letters.append(event)
        if len(_dead_letters) > 200:
            del _dead_letters[0]
        append_dead_letter(event)
    return {"ok": True, "stored": len(_alert_events), "notify": notify}


@app.post("/alerts/critical")
def alerts_critical(payload: Dict[str, Any]):
    notify = _send_to_notifier("critical", payload)
    event = {
        "channel": "critical",
        "receivedAt": datetime.now(timezone.utc).isoformat(),
        "payload": payload,
        "notify": notify,
    }
    _alert_events.append(event)
    if len(_alert_events) > 200:
        del _alert_events[0]
    if not notify.get("delivered"):
        _dead_letters.append(event)
        if len(_dead_letters) > 200:
            del _dead_letters[0]
        append_dead_letter(event)
    return {"ok": True, "stored": len(_alert_events), "notify": notify}


@app.get("/alerts/recent")
def alerts_recent(limit: int = 20):
    safe_limit = max(1, min(limit, 100))
    return {"items": list(reversed(_alert_events[-safe_limit:]))}


@app.get("/alerts/notifier")
def notifier_status():
    return {
        "mode": _notifier_mode,
        "slackConfigured": bool(_slack_webhook_url),
        "teamsConfigured": bool(_teams_webhook_url),
    }


@app.get("/alerts/dead-letter")
def dead_letters(limit: int = 20):
    safe_limit = max(1, min(limit, 100))
    return {"items": list(reversed(_dead_letters[-safe_limit:]))}


@app.get("/alerts/dead-letter/file")
def dead_letters_file(limit: int = 20):
    rows = read_recent_dead_letters(limit)
    return {"items": list(reversed(rows))}


@app.post("/alerts/dead-letter/replay")
def replay_dead_letters(limit: int = 10, source: str = "memory", markSuccess: bool = True):
    safe_limit = max(1, min(limit, 50))
    src = (source or "memory").strip().lower()
    if src == "file":
        file_rows = list(reversed(read_recent_unprocessed_dead_letters(safe_limit)))
        candidates = [row.get("event") or {} for row in file_rows]
        candidate_lines = [row.get("_line") for row in file_rows]
    else:
        candidates = list(reversed(_dead_letters[-safe_limit:]))
        candidate_lines = []

    replayed = 0
    failed = 0
    success_lines: List[int] = []
    for idx, event in enumerate(candidates):
        channel = event.get("channel", "default")
        payload = event.get("payload") or {}
        notify = _send_to_notifier(channel, payload)
        if notify.get("delivered"):
            replayed += 1
            if src == "file":
                line_no = candidate_lines[idx] if idx < len(candidate_lines) else None
                if isinstance(line_no, int) and line_no > 0:
                    success_lines.append(line_no)
        else:
            failed += 1
    marked = 0
    if src == "file" and markSuccess and success_lines:
        marked = mark_processed(success_lines)
    return {
        "source": src,
        "requested": len(candidates),
        "replayed": replayed,
        "failed": failed,
        "markedProcessed": marked,
        "deadLetterFileEnabled": dead_letter_file_enabled(),
    }


@app.post("/alerts/dead-letter/compact")
def compact_dead_letter_file(
    removeProcessed: bool = True,
    keepLast: int = 1000,
    maxAgeHours: int = 0,
):
    max_age = maxAgeHours if maxAgeHours > 0 else None
    summary = compact_dead_letters(
        remove_processed=removeProcessed,
        keep_last=keepLast,
        max_age_hours=max_age,
    )
    return {
        "deadLetterFileEnabled": dead_letter_file_enabled(),
        "removeProcessed": removeProcessed,
        "keepLast": keepLast,
        "maxAgeHours": maxAgeHours,
        "summary": summary,
    }
