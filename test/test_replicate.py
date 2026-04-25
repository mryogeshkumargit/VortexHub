import httpx
import asyncio
import base64
from dotenv import load_dotenv
import os

load_dotenv()
REPLICATE_API_KEY = os.getenv("REPLICATE_API_KEY")

async def test_replicate():
    async with httpx.AsyncClient(timeout=300.0) as client:
        # Get model version
        print("Getting model version...")
        model_response = await client.get(
            "https://api.replicate.com/v1/models/bytedance/seedream-4",
            headers={"Authorization": f"Token {REPLICATE_API_KEY}"}
        )
        model_response.raise_for_status()
        model_data = model_response.json()
        version = model_data["latest_version"]["id"]
        print(f"Version: {version}")
        
        # Create prediction
        print("\nCreating prediction...")
        input_data = {
            "prompt": "a beautiful india woman on traditional indian lahanga and choli",
            "size": "2K",
            "enhance_prompt": True,
            "sequential_image_generation": "disabled",
            "max_images": 1
        }
        
        response = await client.post(
            "https://api.replicate.com/v1/predictions",
            headers={
                "Authorization": f"Token {REPLICATE_API_KEY}",
                "Content-Type": "application/json"
            },
            json={"version": version, "input": input_data}
        )
        response.raise_for_status()
        data = response.json()
        prediction_id = data["id"]
        print(f"Prediction ID: {prediction_id}")
        print(f"Status: {data['status']}")
        
        # Poll for result
        print("\nPolling for result...")
        for i in range(120):
            await asyncio.sleep(2)
            status_response = await client.get(
                f"https://api.replicate.com/v1/predictions/{prediction_id}",
                headers={"Authorization": f"Token {REPLICATE_API_KEY}"}
            )
            status_data = status_response.json()
            print(f"Attempt {i+1}: {status_data['status']}")
            
            if status_data["status"] == "succeeded":
                output_url = status_data["output"][0]
                print(f"\nSuccess! Image URL: {output_url}")
                
                # Download image
                img_response = await client.get(output_url)
                with open("test_output.png", "wb") as f:
                    f.write(img_response.content)
                print("Image saved to test_output.png")
                return
            elif status_data["status"] == "failed":
                print(f"Failed: {status_data.get('error')}")
                return
        
        print("Timeout")

if __name__ == "__main__":
    asyncio.run(test_replicate())
