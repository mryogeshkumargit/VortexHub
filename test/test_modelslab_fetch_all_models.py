import requests
import json

API_KEY = "RpcCfnn8RHAROrjkq9SBFHvwqxqCgzEtNb2NFg6gZCeJSxgGRiXDonfGmoun"
ENDPOINT = "https://modelslab.com/api/v4/dreambooth/model_list"

headers = {"Content-Type": "application/json"}

all_models = []
page = 1
while True:
    payload = {"key": API_KEY, "limit": 500, "page": page}
    response = requests.post(ENDPOINT, headers=headers, data=json.dumps(payload))
    if response.status_code != 200:
        print(f"Error: {response.status_code} {response.text}")
        break
    data = response.json()
    if isinstance(data, list):
        print("Unexpected list response:", data)
        break
    if data.get("status") != "success":
        print(f"API Error: {data.get('message')}")
        break
    models = data.get("data", [])
    all_models.extend(models)
    # Check for pagination
    next_page = data.get("next_page") or data.get("next_page_id")
    if not next_page or next_page == page or len(models) == 0:
        break
    page = next_page if isinstance(next_page, int) else page + 1

print(f"Total models fetched: {len(all_models)}")
if all_models:
    print("Sample model IDs:")
    for m in all_models[:10]:
        print(m.get("model_id") or m.get("id") or m) 