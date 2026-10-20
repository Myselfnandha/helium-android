#!/usr/bin/env python3
"""
scripts/watch_release.py - Watches GitHub Actions release run reactively until completion.
"""

import sys
import time
import json
import urllib.request

def check_run(run_id):
    url = f"https://api.github.com/repos/Myselfnandha/helium-android/actions/runs/{run_id}"
    req = urllib.request.Request(url, headers={"User-Agent": "Helium-Watcher"})
    try:
        with urllib.request.urlopen(req) as resp:
            data = json.loads(resp.read().decode())
            return data.get("status"), data.get("conclusion"), data.get("html_url")
    except Exception as e:
        return "error", str(e), ""

def main():
    if len(sys.argv) < 2:
        print("Usage: watch_release.py <run_id>")
        sys.exit(1)

    run_id = sys.argv[1]
    print(f"[*] Watching GitHub Actions Release run: {run_id}")

    while True:
        status, conclusion, html_url = check_run(run_id)
        if status == "completed":
            print(f"[✓] Release run {run_id} completed with conclusion: {conclusion}")
            print(f"[*] URL: {html_url}")
            if conclusion == "success":
                sys.exit(0)
            else:
                sys.exit(1)
        elif status == "error":
            print(f"[!] Warning checking run {run_id}: {conclusion}")

        time.sleep(10)

if __name__ == "__main__":
    main()
