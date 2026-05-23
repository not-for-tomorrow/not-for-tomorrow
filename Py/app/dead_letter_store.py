import json
import os
from datetime import datetime, timezone, timedelta
from typing import Any, Dict, List, Optional, Set


_dead_letter_file = os.getenv("ALERT_DEAD_LETTER_FILE", "").strip()


def append_dead_letter(event: Dict[str, Any]) -> None:
    if not _dead_letter_file:
        return
    os.makedirs(os.path.dirname(_dead_letter_file), exist_ok=True)
    row = {
        "savedAt": datetime.now(timezone.utc).isoformat(),
        "event": event,
    }
    with open(_dead_letter_file, "a", encoding="utf-8") as f:
        f.write(json.dumps(row, ensure_ascii=False) + "\n")


def is_enabled() -> bool:
    return bool(_dead_letter_file)


def read_recent_dead_letters(limit: int = 20) -> List[Dict[str, Any]]:
    if not _dead_letter_file or not os.path.exists(_dead_letter_file):
        return []
    safe_limit = max(1, min(limit, 200))
    rows: List[Dict[str, Any]] = []
    with open(_dead_letter_file, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            try:
                rows.append(json.loads(line))
            except Exception:
                continue
    return rows[-safe_limit:]


def read_recent_unprocessed_dead_letters(limit: int = 20) -> List[Dict[str, Any]]:
    if not _dead_letter_file or not os.path.exists(_dead_letter_file):
        return []
    safe_limit = max(1, min(limit, 200))
    rows: List[Dict[str, Any]] = []
    with open(_dead_letter_file, "r", encoding="utf-8") as f:
        for idx, line in enumerate(f, start=1):
            line = line.strip()
            if not line:
                continue
            try:
                row = json.loads(line)
                if row.get("processedAt"):
                    continue
                row["_line"] = idx
                rows.append(row)
            except Exception:
                continue
    return rows[-safe_limit:]


def mark_processed(line_numbers: List[int]) -> int:
    if not _dead_letter_file or not os.path.exists(_dead_letter_file):
        return 0
    targets: Set[int] = set(int(x) for x in line_numbers if int(x) > 0)
    if not targets:
        return 0
    updated = 0
    output: List[str] = []
    processed_at = datetime.now(timezone.utc).isoformat()
    with open(_dead_letter_file, "r", encoding="utf-8") as f:
        for idx, line in enumerate(f, start=1):
            text = line.strip()
            if not text:
                continue
            try:
                row = json.loads(text)
            except Exception:
                output.append(text)
                continue
            if idx in targets and not row.get("processedAt"):
                row["processedAt"] = processed_at
                updated += 1
            output.append(json.dumps(row, ensure_ascii=False))
    with open(_dead_letter_file, "w", encoding="utf-8") as f:
        for row_text in output:
            f.write(row_text + "\n")
    return updated


def compact_dead_letters(
    remove_processed: bool = True,
    keep_last: int = 1000,
    max_age_hours: Optional[int] = None,
) -> Dict[str, int]:
    if not _dead_letter_file or not os.path.exists(_dead_letter_file):
        return {"before": 0, "after": 0, "removed": 0}

    rows: List[Dict[str, Any]] = []
    with open(_dead_letter_file, "r", encoding="utf-8") as f:
        for line in f:
            text = line.strip()
            if not text:
                continue
            try:
                rows.append(json.loads(text))
            except Exception:
                continue

    before = len(rows)
    filtered = rows

    if remove_processed:
        filtered = [r for r in filtered if not r.get("processedAt")]

    if max_age_hours is not None and max_age_hours > 0:
        cutoff = datetime.now(timezone.utc) - timedelta(hours=max_age_hours)
        kept: List[Dict[str, Any]] = []
        for r in filtered:
            saved_at = r.get("savedAt")
            try:
                ts = datetime.fromisoformat(str(saved_at))
                if ts.tzinfo is None:
                    ts = ts.replace(tzinfo=timezone.utc)
                if ts >= cutoff:
                    kept.append(r)
            except Exception:
                kept.append(r)
        filtered = kept

    safe_keep_last = max(1, keep_last)
    if len(filtered) > safe_keep_last:
        filtered = filtered[-safe_keep_last:]

    with open(_dead_letter_file, "w", encoding="utf-8") as f:
        for row in filtered:
            f.write(json.dumps(row, ensure_ascii=False) + "\n")

    after = len(filtered)
    return {"before": before, "after": after, "removed": max(0, before - after)}
