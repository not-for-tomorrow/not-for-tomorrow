from typing import Any, Dict, List


def build_summary(channel: str, payload: Dict[str, Any]) -> str:
    status = str(payload.get("status", "unknown")).upper()
    alerts = payload.get("alerts") or []
    if not isinstance(alerts, list):
        alerts = []
    first = alerts[0] if alerts else {}
    labels = first.get("labels") or {}
    annotations = first.get("annotations") or {}
    alert_name = labels.get("alertname", "unknown-alert")
    severity = labels.get("severity", "unknown")
    summary = annotations.get("summary", "No summary")
    count = len(alerts)
    return f"[{channel}] {status} | {alert_name} | severity={severity} | alerts={count} | {summary}"


def build_details(payload: Dict[str, Any], max_items: int = 3) -> str:
    alerts = payload.get("alerts") or []
    if not isinstance(alerts, list):
        alerts = []
    lines: List[str] = []
    for item in alerts[:max_items]:
        labels = item.get("labels") or {}
        annotations = item.get("annotations") or {}
        name = labels.get("alertname", "unknown-alert")
        instance = labels.get("instance", "-")
        summary = annotations.get("summary", "-")
        lines.append(f"- {name} @ {instance}: {summary}")
    if not lines:
        return "No alert details."
    return "\n".join(lines)
