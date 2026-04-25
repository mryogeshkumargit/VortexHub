# ElevenLabs TTS Tester

A comprehensive GUI application for testing ElevenLabs Text-to-Speech API with all available models and voices in English and Hindi.

## Features

- **Complete TTS Model Support**: All ElevenLabs TTS models including:
  - eleven_multilingual_v2
  - eleven_turbo_v2_5
  - eleven_turbo_v2
  - eleven_monolingual_v1
  - eleven_multilingual_v1

- **Extensive Voice Library**:
  - **50 English Voices**: Rachel, Drew, Clyde, Paul, Domi, Dave, Fin, Sarah, Antoni, Thomas, Charlie, Emily, Elli, Callum, Patrick, Harry, Liam, Dorothy, Josh, Arnold, Charlotte, Alice, Matilda, James, Joseph, Jeremy, Michael, Ethan, Gigi, Freya, Grace, Daniel, Lily, Serena, Adam, Nicole, Bill, Jessie, Sam, Glinda, Giovanni, Mimi, Brian, Chris, Eric, Aria, Roger, Laura, Will, Scarlett
  - **20 Hindi Voices**: Prabhat, Abhishek, Aditi, Arjun, Ayush, Dhruv, Garima, Kavya, Kiara, Kunal, Meera, Nisha, Priya, Raghav, Ravi, Rohit, Shreya, Simran, Suresh, Tanvi

- **Advanced Features**:
  - API key management with secure storage in .env file
  - Real-time API request and response logging
  - Audio playback functionality
  - Progress indicators
  - Error handling and validation

## Installation & Usage

### Automatic Setup (Recommended)
1. Double-click `run_elevenlabs_gui.bat`
2. The script will automatically:
   - Check Python installation
   - Install dependencies if needed
   - Launch the application

### Manual Setup
1. Install Python 3.7+ if not already installed
2. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```
3. Run the application:
   ```bash
   python elevenlabs_tts_gui.py
   ```

## Configuration

1. **API Key Setup**:
   - Enter your ElevenLabs API key in the application
   - Click "Save API Key" to store it securely in the .env file
   - The key will be automatically loaded on future launches

2. **Usage**:
   - Select your preferred TTS model
   - Choose language (English or Hindi)
   - Select a voice from the available options
   - Enter text to convert to speech
   - Click "Generate Speech" to create audio
   - Use "Play Audio" to listen to the generated speech

## API Integration

The application integrates with ElevenLabs API endpoints:
- **Text-to-Speech**: `https://api.elevenlabs.io/v1/text-to-speech/{voice_id}`
- **Authentication**: Uses xi-api-key header
- **Audio Format**: MP3 output

## Logging

The application provides comprehensive logging:
- **Request Log**: Shows all API requests with headers and data
- **Response Log**: Displays API responses, status codes, and error messages
- **Real-time Updates**: Logs are updated in real-time during API calls

## Dependencies

- `requests==2.31.0`: HTTP library for API calls
- `pygame==2.5.2`: Audio playback functionality
- `tkinter`: GUI framework (included with Python)

## File Structure

```
ElevenLabs TTS Tester/
├── elevenlabs_tts_gui.py      # Main application
├── requirements.txt           # Python dependencies
├── run_elevenlabs_gui.bat    # Auto-launcher script
├── .env                      # API key storage
└── README.md                 # This file
```

## Error Handling

The application includes robust error handling for:
- Missing API keys
- Network connectivity issues
- Invalid API responses
- Audio playback errors
- File system operations

## Security

- API keys are stored securely in .env file
- Keys are masked in request logs
- Temporary audio files are properly managed