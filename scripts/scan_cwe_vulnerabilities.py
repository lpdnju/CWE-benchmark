#!/usr/bin/env python3
"""
CWE Vulnerability Scanner using GitHub Copilot / Models API.

Reads CWE rules from cwe-security-rules.json and scans all Java files in src/
by sending each file's content to the GitHub Models API for analysis.
Results are saved per-file as JSON and aggregated into a summary report.

Prerequisites:
  - GitHub CLI installed and authenticated: gh auth login
  - Python 3.8+
  - requests: pip install requests

Usage:
  python scan_cwe_vulnerabilities.py
  python scan_cwe_vulnerabilities.py --max-files 50 --resume
  python scan_cwe_vulnerabilities.py --cwe-filter CWE-22 CWE-89 CWE-79
  python scan_cwe_vulnerabilities.py --help
"""

import argparse
import json
import os
import subprocess
import sys
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

try:
    import requests
except ImportError:
    print("Error: 'requests' package is required. Install it with: pip install requests")
    sys.exit(1)

# ─── Constants ────────────────────────────────────────────────────────────────

GITHUB_MODELS_API_URL = "https://models.inference.ai.azure.com/chat/completions"
SUMMARY_FILE_NAME = "_summary-report.json"
ERROR_LOG_FILE_NAME = "_errors.log"


# ─── Helper Functions ─────────────────────────────────────────────────────────

def get_github_token() -> str:
    """Retrieve the GitHub token from gh CLI."""
    try:
        result = subprocess.run(
            ["gh", "auth", "token"],
            capture_output=True, text=True, check=True,
        )
        token = result.stdout.strip()
        if not token:
            raise RuntimeError("Empty token returned")
        return token
    except (subprocess.CalledProcessError, FileNotFoundError, RuntimeError):
        print(
            "Failed to retrieve GitHub token. Ensure you have:\n"
            "  1. GitHub CLI installed: https://cli.github.com/\n"
            "  2. Authenticated: gh auth login\n"
            "  3. GitHub Copilot / Models access enabled on your account",
            file=sys.stderr,
        )
        sys.exit(1)


def load_cwe_rules(rules_file: str, cwe_filter: list[str] | None = None) -> list[dict]:
    """Parse CWE rules from the JSON file, optionally filtering by ID."""
    path = Path(rules_file)
    if not path.exists():
        print(f"CWE rules file not found: {rules_file}", file=sys.stderr)
        sys.exit(1)

    with open(path, encoding="utf-8") as f:
        data = json.load(f)

    rules = data.get("rules", [])

    if cwe_filter:
        rules = [r for r in rules if r["id"] in cwe_filter]
        print(f"  Filtered to {len(rules)} CWE rules: {', '.join(cwe_filter)}")

    return rules


def format_cwe_rules_for_prompt(rules: list[dict]) -> str:
    """Format CWE rules into a concise prompt-friendly string."""
    lines = []
    for r in rules:
        desc = r.get("description", "")
        if len(desc) > 150:
            desc = desc[:150] + "..."
        lines.append(f"- {r['id']} [{r.get('severity', 'Unknown')}]: {r['name']} — {desc}")
    return "\n".join(lines)


def invoke_copilot_analysis(
    file_content: str,
    file_path: str,
    cwe_prompt: str,
    token: str,
    model: str,
) -> str:
    """Send a file's content to the GitHub Models API for CWE analysis."""

    system_prompt = (
        "You are an expert security code auditor. Your task is to analyze Java source code for CWE\n"
        "(Common Weakness Enumeration) vulnerabilities. Be precise: only report real, actionable findings\n"
        "with specific line numbers. Do not report speculative or low-confidence issues.\n\n"
        "Respond ONLY with valid JSON in this exact format (no markdown fences, no extra text):\n"
        "{\n"
        '  "findings": [\n'
        "    {\n"
        '      "cwe_id": "CWE-XXX",\n'
        '      "severity": "Critical|High|Medium|Low",\n'
        '      "line_start": 0,\n'
        '      "line_end": 0,\n'
        '      "code_snippet": "the vulnerable code",\n'
        '      "description": "what the vulnerability is",\n'
        '      "suggestion": "how to fix it"\n'
        "    }\n"
        "  ]\n"
        "}\n\n"
        'If no vulnerabilities are found, respond with: {"findings": []}'
    )

    user_prompt = (
        f"Analyze the following Java file for these CWE vulnerabilities:\n\n"
        f"{cwe_prompt}\n\n"
        f"--- FILE: {file_path} ---\n"
        f"```java\n{file_content}\n```\n\n"
        f"Report all CWE issues found. Only report findings you are confident about."
    )

    payload = {
        "model": model,
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ],
        "temperature": 0.1,
        "max_tokens": 4096,
    }

    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json",
    }

    resp = requests.post(GITHUB_MODELS_API_URL, headers=headers, json=payload, timeout=120)
    resp.raise_for_status()
    return resp.json()["choices"][0]["message"]["content"]


def parse_findings_json(raw_response: str) -> dict[str, Any]:
    """Attempt to parse the AI response as JSON, handling common issues."""
    import re

    clean = re.sub(r"```json\s*", "", raw_response)
    clean = re.sub(r"```\s*", "", clean).strip()

    try:
        parsed = json.loads(clean)
        if "findings" not in parsed:
            return {"findings": [], "raw": raw_response, "parseError": "Missing 'findings' key"}
        return parsed
    except json.JSONDecodeError as e:
        return {"findings": [], "raw": raw_response, "parseError": str(e)}


def result_file_name(source_file: str, base_dir: str) -> str:
    """Generate a unique result filename from the source file path."""
    relative = os.path.relpath(source_file, base_dir)
    safe_name = relative.replace(os.sep, "_").replace("/", "_")
    if safe_name.endswith(".java"):
        safe_name = safe_name[: -len(".java")]
    return safe_name + ".json"


# ─── Colour helpers (ANSI) ────────────────────────────────────────────────────

class C:
    """ANSI colour codes for terminal output."""
    RESET = "\033[0m"
    RED = "\033[91m"
    GREEN = "\033[92m"
    YELLOW = "\033[93m"
    CYAN = "\033[96m"
    GRAY = "\033[90m"
    DARK_RED = "\033[31m"
    DARK_YELLOW = "\033[33m"

    @staticmethod
    def supports_color() -> bool:
        return hasattr(sys.stdout, "isatty") and sys.stdout.isatty()


def cprint(msg: str, color: str = "") -> None:
    """Print with optional ANSI colour."""
    if color and C.supports_color():
        print(f"{color}{msg}{C.RESET}")
    else:
        print(msg)


# ─── Main ─────────────────────────────────────────────────────────────────────

def main() -> None:
    script_dir = Path(__file__).resolve().parent

    parser = argparse.ArgumentParser(
        description="Scan Java source files for CWE vulnerabilities using GitHub Copilot / Models API.",
    )
    parser.add_argument("--rules-file", default=str(script_dir / "cwe-security-rules.json"),
                        help="Path to the CWE security rules JSON file (default: cwe-security-rules.json)")
    parser.add_argument("--src-dir", default=str(script_dir / "src"),
                        help="Path to the source directory to scan (default: src)")
    parser.add_argument("--output-dir", default=str(script_dir / "cwe-scan-results"),
                        help="Directory to store scan results (default: cwe-scan-results)")
    parser.add_argument("--model", default="openai/gpt-4o",
                        help="AI model to use via GitHub Models API (default: openai/gpt-4o)")
    parser.add_argument("--max-file-size-kb", type=int, default=100,
                        help="Skip files larger than this size in KB (default: 100)")
    parser.add_argument("--delay-ms", type=int, default=1000,
                        help="Delay in milliseconds between API calls (default: 1000)")
    parser.add_argument("--resume", action="store_true",
                        help="Skip files that already have results in the output directory")
    parser.add_argument("--file-filter", default="*.java",
                        help="Glob pattern to filter source files (default: *.java)")
    parser.add_argument("--max-files", type=int, default=0,
                        help="Maximum number of files to process, 0 = no limit (default: 0)")
    parser.add_argument("--cwe-filter", nargs="*", default=[],
                        help="CWE IDs to check, e.g. CWE-22 CWE-89 (default: all)")
    args = parser.parse_args()

    print()
    cprint("═══════════════════════════════════════════════════════════", C.CYAN)
    cprint("  CWE Vulnerability Scanner (GitHub Copilot / Models API)", C.CYAN)
    cprint("═══════════════════════════════════════════════════════════", C.CYAN)
    print()

    # Step 1: Authenticate
    cprint("[1/5] Authenticating with GitHub...", C.YELLOW)
    token = get_github_token()
    cprint("  ✓ GitHub token retrieved", C.GREEN)

    # Step 2: Load CWE rules
    cprint(f"[2/5] Loading CWE rules from {args.rules_file}...", C.YELLOW)
    cwe_rules = load_cwe_rules(args.rules_file, args.cwe_filter or None)
    cwe_prompt_text = format_cwe_rules_for_prompt(cwe_rules)
    cprint(f"  ✓ Loaded {len(cwe_rules)} CWE rules", C.GREEN)

    # Step 3: Discover source files
    cprint(f"[3/5] Discovering source files in {args.src_dir}...", C.YELLOW)
    src_path = Path(args.src_dir)
    source_files = sorted(src_path.rglob(args.file_filter))

    if not source_files:
        print(f"No files matching '{args.file_filter}' found in {args.src_dir}", file=sys.stderr)
        sys.exit(1)
    cprint(f"  ✓ Found {len(source_files)} files", C.GREEN)

    # Step 4: Prepare output directory
    cprint("[4/5] Preparing output directory...", C.YELLOW)
    output_path = Path(args.output_dir)
    output_path.mkdir(parents=True, exist_ok=True)
    error_log_path = output_path / ERROR_LOG_FILE_NAME
    cprint(f"  ✓ Output: {output_path}", C.GREEN)

    # Apply MaxFiles limit
    if args.max_files > 0 and len(source_files) > args.max_files:
        cprint(f"  ℹ Limiting to first {args.max_files} files (of {len(source_files)})", C.CYAN)
        source_files = source_files[: args.max_files]

    # Step 5: Scan files
    cprint("[5/5] Scanning files for CWE vulnerabilities...", C.YELLOW)
    cprint(f"  Model: {args.model} | Rate limit delay: {args.delay_ms}ms", C.GRAY)
    print()

    total_files = len(source_files)
    processed_count = 0
    skipped_count = 0
    error_count = 0
    total_findings = 0
    all_results: list[dict[str, Any]] = []
    start_time = time.monotonic()

    for file in source_files:
        processed_count += 1
        relative_path = str(file.relative_to(src_path))
        res_fname = result_file_name(str(file), str(src_path))
        res_fpath = output_path / res_fname

        pct = round(processed_count / total_files * 100)
        progress_bar = f"[{'█' * (pct // 5)}{'░' * (20 - pct // 5)}] {pct}%"
        print(f"\r  {progress_bar}  {processed_count}/{total_files}", end="", flush=True)

        # Resume: skip if result already exists
        if args.resume and res_fpath.exists():
            skipped_count += 1
            print()
            cprint(f"  [{processed_count}/{total_files}] SKIP (cached): {relative_path}", C.GRAY)
            try:
                cached = json.loads(res_fpath.read_text(encoding="utf-8"))
                total_findings += len(cached.get("findings", []))
                all_results.append({
                    "file": relative_path,
                    "findings": cached.get("findings", []),
                    "status": "cached",
                })
            except Exception:
                pass
            continue

        # Read file content
        try:
            content = file.read_text(encoding="utf-8", errors="replace")
        except OSError:
            content = ""

        if not content.strip():
            skipped_count += 1
            print()
            cprint(f"  [{processed_count}/{total_files}] SKIP (empty): {relative_path}", C.GRAY)
            continue

        # Check file size limit
        file_size_kb = round(len(content.encode("utf-8")) / 1024, 1)
        if file_size_kb > args.max_file_size_kb:
            skipped_count += 1
            print()
            cprint(
                f"  [{processed_count}/{total_files}] SKIP ({file_size_kb}KB > {args.max_file_size_kb}KB): {relative_path}",
                C.GRAY,
            )
            continue

        # Call the API
        try:
            raw_response = invoke_copilot_analysis(
                file_content=content,
                file_path=relative_path,
                cwe_prompt=cwe_prompt_text,
                token=token,
                model=args.model,
            )

            parsed = parse_findings_json(raw_response)
            findings_count = len(parsed.get("findings", []))
            total_findings += findings_count

            result: dict[str, Any] = {
                "file": relative_path,
                "scannedAt": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
                "model": args.model,
                "findingsCount": findings_count,
                "findings": parsed["findings"],
            }

            if "parseError" in parsed:
                result["parseWarning"] = parsed["parseError"]
                result["rawResponse"] = parsed.get("raw", "")

            # Save individual result
            res_fpath.write_text(json.dumps(result, indent=2, ensure_ascii=False), encoding="utf-8")

            all_results.append(result)

            print()
            if findings_count > 0:
                cprint(
                    f"  [{processed_count}/{total_files}] ⚠ {findings_count} issue(s): {relative_path}",
                    C.RED,
                )
                for f in parsed["findings"]:
                    cprint(
                        f"      → {f.get('cwe_id', '?')} [L{f.get('line_start', '?')}]: {f.get('description', '')}",
                        C.DARK_YELLOW,
                    )
            else:
                cprint(f"  [{processed_count}/{total_files}] ✓ Clean: {relative_path}", C.GREEN)

        except Exception as exc:
            error_count += 1
            err_msg = f"[{processed_count}/{total_files}] ERROR processing {relative_path}: {exc}"
            print()
            cprint(f"  {err_msg}", C.RED)
            with open(error_log_path, "a", encoding="utf-8") as log:
                log.write(err_msg + "\n")

            all_results.append({
                "file": relative_path,
                "findings": [],
                "status": "error",
                "error": str(exc),
            })

        # Rate limiting delay (skip on last file)
        if processed_count < total_files:
            time.sleep(args.delay_ms / 1000.0)

    print()

    # ─── Generate Summary Report ──────────────────────────────────────────────

    elapsed = time.monotonic() - start_time
    hours, remainder = divmod(int(elapsed), 3600)
    minutes, seconds = divmod(remainder, 60)
    duration_str = f"{hours:02d}:{minutes:02d}:{seconds:02d}"

    files_with_findings = sum(1 for r in all_results if len(r.get("findings", [])) > 0)

    # Aggregate findings by CWE ID
    findings_by_cwe: dict[str, dict[str, Any]] = {}
    for r in all_results:
        for f in r.get("findings", []):
            cwe_id = f.get("cwe_id", "Unknown")
            if cwe_id not in findings_by_cwe:
                findings_by_cwe[cwe_id] = {"count": 0, "files": []}
            findings_by_cwe[cwe_id]["count"] += 1
            if r["file"] not in findings_by_cwe[cwe_id]["files"]:
                findings_by_cwe[cwe_id]["files"].append(r["file"])

    # Aggregate findings by severity
    findings_by_severity: dict[str, int] = {}
    for r in all_results:
        for f in r.get("findings", []):
            sev = f.get("severity", "Unknown")
            findings_by_severity[sev] = findings_by_severity.get(sev, 0) + 1

    summary = {
        "scanDate": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "duration": duration_str,
        "model": args.model,
        "cweRulesChecked": len(cwe_rules),
        "totalFilesFound": total_files,
        "filesScanned": processed_count - skipped_count,
        "filesSkipped": skipped_count,
        "filesCached": sum(1 for r in all_results if r.get("status") == "cached") if args.resume else 0,
        "filesWithErrors": error_count,
        "filesWithFindings": files_with_findings,
        "totalFindings": total_findings,
        "findingsByCwe": findings_by_cwe,
        "findingsBySeverity": findings_by_severity,
        "configuration": {
            "rulesFile": args.rules_file,
            "srcDir": args.src_dir,
            "outputDir": args.output_dir,
            "maxFileSizeKB": args.max_file_size_kb,
            "fileFilter": args.file_filter,
            "cweFilter": args.cwe_filter if args.cwe_filter else "all",
        },
    }

    summary_path = output_path / SUMMARY_FILE_NAME
    summary_path.write_text(json.dumps(summary, indent=2, ensure_ascii=False), encoding="utf-8")

    # ─── Print Summary ────────────────────────────────────────────────────────

    print()
    cprint("═══════════════════════════════════════════════════════════", C.CYAN)
    cprint("  SCAN COMPLETE", C.CYAN)
    cprint("═══════════════════════════════════════════════════════════", C.CYAN)
    print()
    print(f"  Duration:           {duration_str}")
    print(f"  Files scanned:      {processed_count - skipped_count} / {total_files}")
    print(f"  Files skipped:      {skipped_count}")
    print(f"  Errors:             {error_count}")
    print()

    if total_findings > 0:
        cprint(f"  ⚠ FINDINGS:         {total_findings} issue(s) in {files_with_findings} file(s)", C.RED)
        print()

        if findings_by_severity:
            cprint("  By Severity:", C.YELLOW)
            severity_colors = {
                "Critical": C.RED,
                "High": C.DARK_RED,
                "Medium": C.YELLOW,
                "Low": C.DARK_YELLOW,
            }
            for sev in ["Critical", "High", "Medium", "Low", "Unknown"]:
                if sev in findings_by_severity:
                    color = severity_colors.get(sev, C.GRAY)
                    cprint(f"    {sev}: {findings_by_severity[sev]}", color)
            print()

        if findings_by_cwe:
            cprint("  By CWE ID:", C.YELLOW)
            for cwe_id, info in sorted(findings_by_cwe.items(), key=lambda x: x[1]["count"], reverse=True):
                print(f"    {cwe_id}: {info['count']} occurrence(s) in {len(info['files'])} file(s)")
            print()
    else:
        cprint("  ✓ No CWE vulnerabilities found!", C.GREEN)
        print()

    cprint(f"  Results:  {output_path}", C.CYAN)
    cprint(f"  Summary:  {summary_path}", C.CYAN)
    if error_count > 0:
        cprint(f"  Errors:   {error_log_path}", C.RED)
    print()


if __name__ == "__main__":
    main()
