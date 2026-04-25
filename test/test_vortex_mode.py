#!/usr/bin/env python3
"""
Simple test script to verify Vortex Mode implementation
This script checks if all the necessary files and components are in place
"""

import os
import re

def check_file_exists(filepath, description):
    """Check if a file exists and print result"""
    if os.path.exists(filepath):
        print(f"[OK] {description}: {filepath}")
        return True
    else:
        print(f"[FAIL] {description}: {filepath} - NOT FOUND")
        return False

def check_file_contains(filepath, pattern, description):
    """Check if a file contains a specific pattern"""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
            if re.search(pattern, content, re.MULTILINE | re.DOTALL):
                print(f"[OK] {description}")
                return True
            else:
                print(f"[FAIL] {description} - PATTERN NOT FOUND")
                return False
    except Exception as e:
        print(f"[FAIL] {description} - ERROR: {e}")
        return False

def main():
    print("Vortex Mode Implementation Verification")
    print("=" * 50)
    
    base_path = "d:/VortexAndroid/app/src/main/java/com/vortexai/android"
    
    # Check core files exist
    files_to_check = [
        (f"{base_path}/ui/screens/chat/VortexImageGenerator.kt", "VortexImageGenerator class"),
        (f"{base_path}/ui/screens/settings/managers/SettingsDataStore.kt", "SettingsDataStore"),
        (f"{base_path}/ui/screens/chat/ChatViewModel.kt", "ChatViewModel"),
        (f"{base_path}/ui/screens/chat/components/ChatOptionsMenu.kt", "ChatOptionsMenu"),
        (f"{base_path}/ui/screens/chat/ChatScreen.kt", "ChatScreen"),
        (f"{base_path}/di/RepositoryModule.kt", "RepositoryModule"),
    ]
    
    all_files_exist = True
    for filepath, description in files_to_check:
        if not check_file_exists(filepath, description):
            all_files_exist = False
    
    if not all_files_exist:
        print("\n[FAIL] Some required files are missing!")
        return
    
    print("\nChecking implementation details...")
    
    # Check specific implementations
    checks = [
        (f"{base_path}/ui/screens/settings/managers/SettingsDataStore.kt", 
         r"VORTEX_MODE_ENABLED_KEY.*=.*booleanPreferencesKey", 
         "Vortex Mode setting key in SettingsDataStore"),
        
        (f"{base_path}/ui/screens/chat/ChatViewModel.kt", 
         r"val vortexModeEnabled.*StateFlow<Boolean>", 
         "Vortex Mode StateFlow in ChatViewModel"),
        
        (f"{base_path}/ui/screens/chat/ChatViewModel.kt", 
         r"fun setVortexModeEnabled", 
         "Vortex Mode toggle function in ChatViewModel"),
        
        (f"{base_path}/ui/screens/chat/ChatViewModel.kt", 
         r"private val vortexImageGenerator.*VortexImageGenerator", 
         "VortexImageGenerator injection in ChatViewModel"),
        
        (f"{base_path}/ui/screens/chat/components/ChatOptionsMenu.kt", 
         r"isVortexModeEnabled.*Boolean", 
         "Vortex Mode parameter in ChatOptionsMenu"),
        
        (f"{base_path}/ui/screens/chat/components/ChatOptionsMenu.kt", 
         r"onVortexModeToggle.*->.*Unit", 
         "Vortex Mode toggle callback in ChatOptionsMenu"),
        
        (f"{base_path}/ui/screens/chat/ChatScreen.kt", 
         r"val isVortexModeEnabled.*collectAsStateWithLifecycle", 
         "Vortex Mode state collection in ChatScreen"),
        
        (f"{base_path}/ui/screens/chat/ChatScreen.kt", 
         r"uiState\.vortexImageError", 
         "Vortex image error handling in ChatScreen"),
        
        (f"{base_path}/di/RepositoryModule.kt", 
         r"fun provideVortexImageGenerator", 
         "VortexImageGenerator provider in DI"),
        
        (f"{base_path}/ui/screens/chat/VortexImageGenerator.kt", 
         r"suspend fun generateVortexImage", 
         "Main generation function in VortexImageGenerator"),
    ]
    
    all_checks_passed = True
    for filepath, pattern, description in checks:
        if not check_file_contains(filepath, pattern, description):
            all_checks_passed = False
    
    print("\n" + "=" * 50)
    if all_checks_passed:
        print("[SUCCESS] All Vortex Mode implementation checks PASSED!")
        print("[OK] The feature should be ready for testing")
    else:
        print("[FAIL] Some implementation checks FAILED!")
        print("[INFO] Please review the failed items above")
    
    print("\nNext Steps:")
    print("1. Build the Android project to check for compilation errors")
    print("2. Test the Vortex Mode toggle in chat options menu")
    print("3. Verify image generation works with AI responses")
    print("4. Test error handling with invalid API keys")

if __name__ == "__main__":
    main()