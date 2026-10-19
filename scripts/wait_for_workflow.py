#!/usr/bin/env python3
import os, sys, time, urllib.parse, urllib.request, json

def get_token():
    creds_file = os.path.expanduser("~/.git-credentials")
    if not os.path.exists(creds_file):
        return None
    with open(creds_file) as f:
        for line in f:
            if "github.com" in line:
                return urllib.parse.urlparse(line.strip()).password
    return None

def main():
    token = get_token()
    if not token:
        print("Error: No GitHub token found in ~/.git-credentials")
        sys.exit(1)

    repo = "Myselfnandha/helium-android"
    headers = {
        "Authorization": f"Bearer {token}",
        "Accept": "application/vnd.github.v3+json",
        "User-Agent": "Helium-Monitor"
    }

    # Identify the target run ID (default to latest run if not provided)
    run_id = sys.argv[1] if len(sys.argv) > 1 else None
    if not run_id:
        req = urllib.request.Request(f"https://api.github.com/repos/{repo}/actions/runs?per_page=1", headers=headers)
        with urllib.request.urlopen(req) as resp:
            data = json.loads(resp.read().decode())
            runs = data.get("workflow_runs", [])
            if not runs:
                print("No workflow runs found.")
                sys.exit(1)
            run_id = runs[0]["id"]

    print(f"Monitoring GitHub Actions Workflow Run: {run_id}")
    print(f"URL: https://github.com/{repo}/actions/runs/{run_id}")
    sys.stdout.flush()

    start_time = time.time()
    max_wait = 900  # 15 minutes max
    poll_interval = 15

    while True:
        try:
            req = urllib.request.Request(f"https://api.github.com/repos/{repo}/actions/runs/{run_id}", headers=headers)
            with urllib.request.urlopen(req) as resp:
                run_data = json.loads(resp.read().decode())

            status = run_data.get("status")
            conclusion = run_data.get("conclusion")
            elapsed = int(time.time() - start_time)

            print(f"[{elapsed}s] Status: {status} | Conclusion: {conclusion}")
            sys.stdout.flush()

            if status == "completed":
                print("\n=== Workflow Run Completed! ===")
                print(f"Conclusion: {conclusion}")
                
                # Fetch Job details
                jobs_req = urllib.request.Request(f"https://api.github.com/repos/{repo}/actions/runs/{run_id}/jobs", headers=headers)
                with urllib.request.urlopen(jobs_req) as resp:
                    jobs_data = json.loads(resp.read().decode())

                for job in jobs_data.get("jobs", []):
                    j_name = job.get("name")
                    j_status = job.get("status")
                    j_conc = job.get("conclusion")
                    print(f"\nJob: {j_name} -> {j_status} ({j_conc})")
                    for s in job.get("steps", []):
                        s_name = s.get("name")
                        s_status = s.get("status")
                        s_conc = s.get("conclusion")
                        print(f"  * {s_name}: {s_status} -> {s_conc}")

                # Fetch Artifacts
                art_req = urllib.request.Request(f"https://api.github.com/repos/{repo}/actions/runs/{run_id}/artifacts", headers=headers)
                with urllib.request.urlopen(art_req) as resp:
                    art_data = json.loads(resp.read().decode())
                    artifacts = art_data.get("artifacts", [])
                    print(f"\nTotal Artifacts Generated: {len(artifacts)}")
                    for art in artifacts:
                        print(f"  - Artifact: {art.get('name')} ({art.get('size_in_bytes')} bytes)")

                sys.exit(0 if conclusion == "success" else 1)

            if elapsed > max_wait:
                print(f"Timeout: Workflow run {run_id} did not finish within {max_wait}s.")
                sys.exit(2)

        except Exception as e:
            print(f"Warning during polling: {e}")
            sys.stdout.flush()

        time.sleep(poll_interval)

if __name__ == "__main__":
    main()
