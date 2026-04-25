# Modelslab Image Editing API Tester

A Python GUI application to test the Modelslab image editing API endpoint.

## Features

- **Image Upload**: Support for two images (init_image and optional init_image_2)
- **Model Selection**: Choose from available Modelslab models (flux-kontext-dev, flux-dev, etc.)
- **Parameter Control**: Full control over all API parameters
- **Real-time Logging**: View API requests and responses
- **Image Display**: Preview generated images directly in the app
- **Save Results**: Save generated images to disk

## API Endpoint

- **URL**: `https://modelslab.com/api/v6/images/img2img`
- **Method**: POST
- **Content-Type**: application/json

## Parameters

- `init_image`: Base64 data URL of the first image (required)
- `init_image_2`: Base64 data URL of the second image (optional, can be empty string)
- `prompt`: Text description for the image editing
- `negative_prompt`: What to avoid in the generation
- `model_id`: Model to use (default: flux-kontext-dev)
- `num_inference_steps`: Number of denoising steps (default: 28)
- `strength`: How much to change the original image (default: 0.7)
- `scheduler`: Sampling scheduler (default: DPMSolverMultistepScheduler)
- `guidance`: Guidance scale (default: 2.5)
- `enhance_prompt`: Whether to enhance the prompt (default: false)
- `base64`: Return base64 encoded image (default: "no")
- `key`: Your Modelslab API key

## Setup

1. Install dependencies:
   ```
   pip install -r requirements.txt
   ```

2. Add your API keys to the `.env` file:
   ```
   MODELSLAB_API_KEY=your_modelslab_api_key_here
   IMGBB_API_KEY=your_imgbb_api_key_here
   ```
   
   Get a free imgbb API key from: https://api.imgbb.com/

3. Run the application:
   ```
   python modelslab_image_edit_gui.py
   ```
   
   Or use the batch file:
   ```
   run_modelslab_gui.bat
   ```

## Usage

1. Enter your Modelslab API key and save it
2. Select the required init image (first image)
3. Optionally select a second image
4. Configure the prompt and parameters
5. Click "Generate Image" to test the API
6. View the result and save if needed
7. Check the Logs tab for detailed API communication

## Image Upload Process

1. **Local images are processed**: Resized to meet Modelslab requirements (max 1500x1500px)
2. **Uploaded to imgbb**: Free cloud hosting to get public URLs
3. **URLs sent to Modelslab**: API receives actual image URLs, not base64
4. **Base64 response**: Generated image returned as base64 and displayed

## Notes

- The `init_image_2` parameter is optional and can be left empty
- Images are automatically resized and uploaded to cloud storage
- You need both Modelslab and imgbb API keys
- All API communication is logged for debugging
- Generated images can be saved in PNG or JPEG format