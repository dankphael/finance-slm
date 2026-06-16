#!/usr/bin/env python3
"""Create the finance-slm GitHub repo and push the scaffold."""
import re, urllib.request, json, os, subprocess

creds_file = os.path.expanduser('~/.git-credentials')
with open(creds_file) as f:
    line = f.read().strip()

match = re.search(r'https://[^:]+:([^@]+)@github\.com', line)
if not match:
    print("No token found")
    exit(1)

token = match.group(1)

data = json.dumps({"name": "finance-slm", "description": "Finance SLM app scaffold", "private": False}).encode()

req = urllib.request.Request(
    'https://api.github.com/user/repos',
    data=data,
    headers={
        'Authorization': f'token {token}',
        'Accept': 'application/vnd.github.v3+json',
        'Content-Type': 'application/json'
    },
    method='POST'
)

try:
    with urllib.request.urlopen(req) as resp:
        result = json.loads(resp.read())
        print(f"Created repo: {result.get('html_url', 'unknown')}")
        clone_url = result.get('clone_url', '')
        # Set remote and push
        os.chdir('/home/raphael-lee/finance-slm')
        subprocess.run(['git', 'remote', 'add', 'origin', clone_url], check=False)
        result = subprocess.run(['git', 'push', '-u', 'origin', 'main'], capture_output=True, text=True)
        print(result.stdout)
        if result.returncode != 0:
            print(result.stderr)
except urllib.error.HTTPError as e:
    print(f"HTTP Error {e.code}: {e.read().decode()}")
except Exception as e:
    print(f"Error: {e}")
