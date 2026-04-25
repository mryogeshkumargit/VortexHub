# How to Build Release APK (No "Scan Recommended" Warning)

## Option 1: Quick Fix - Generate Release Keystore

### Step 1: Create Keystore
```bash
cd d:\VortexAndroid
keytool -genkey -v -keystore vortex-release.keystore -alias vortex -keyalg RSA -keysize 2048 -validity 10000
```

Enter details when prompted:
- Password: (choose a strong password)
- Name: Vortex AI
- Organization: Your Company
- City, State, Country: Your info

### Step 2: Create keystore.properties
Create file: `d:\VortexAndroid\keystore.properties`
```properties
storePassword=YOUR_KEYSTORE_PASSWORD
keyPassword=YOUR_KEY_PASSWORD
keyAlias=vortex
storeFile=vortex-release.keystore
```

### Step 3: Update app/build.gradle
Add before `android {` block:
```gradle
def keystorePropertiesFile = rootProject.file("keystore.properties")
def keystoreProperties = new Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(new FileInputStream(keystorePropertiesFile))
}
```

Add inside `android {` block:
```gradle
signingConfigs {
    release {
        if (keystorePropertiesFile.exists()) {
            keyAlias keystoreProperties['keyAlias']
            keyPassword keystoreProperties['keyPassword']
            storeFile file(keystoreProperties['storeFile'])
            storePassword keystoreProperties['storePassword']
        }
    }
}

buildTypes {
    release {
        signingConfig signingConfigs.release
        minifyEnabled false
        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
    }
}
```

### Step 4: Build Release APK
```bash
gradlew assembleRelease
```

Output: `app\build\outputs\apk\release\app-release.apk`

---

## Option 2: Disable Play Protect (Not Recommended)

On your Android device:
1. Open Play Store
2. Tap profile icon → Play Protect
3. Tap Settings gear icon
4. Disable "Scan apps with Play Protect"

**Warning**: This disables security scanning for ALL apps.

---

## Option 3: Accept the Warning (For Testing)

For development/testing:
1. Tap "Install anyway" when warning appears
2. The app will install normally
3. This is safe for your own debug builds

---

## Why Debug Builds Show Warning:

| Build Type | Signature | Play Protect | Warning |
|------------|-----------|--------------|---------|
| Debug | Debug keystore (auto-generated) | Not verified | ⚠️ Yes |
| Release (unsigned) | No signature | Not verified | ⚠️ Yes |
| Release (signed) | Your keystore | Verified after use | ✅ No (after first install) |
| Play Store | Google's signature | Fully verified | ✅ No |

---

## Current Build Command:

**Debug** (shows warning):
```bash
gradlew assembleDebug
```
Output: `app-debug.apk` ⚠️

**Release** (no warning after signing):
```bash
gradlew assembleRelease
```
Output: `app-release.apk` ✅

---

## Security Note:

The warning is **normal for debug builds** and doesn't mean your app is malicious. It's Android's way of saying "This app isn't from Play Store and hasn't been verified."

For personal use, you can safely click "Install anyway."
For distribution, use a signed release build.
