# Notification Usage Examples

## 1. Email Notifications

### When to trigger:
- User registers/logs in
- Important app updates
- Account changes

### Example in AuthRepository:
```kotlin
// After successful registration
if (emailNotificationsEnabled) {
    emailNotificationService.sendEmailNotification(
        to = user.email,
        subject = "Welcome to Vortex AI",
        body = "Thank you for registering! Your account is ready."
    )
}
```

## 2. Local Push Notifications

### When to trigger:
- New chat message received (app in background)
- Image generation completed
- Character created/updated

### Example in ChatViewModel:
```kotlin
// After AI responds (if app is in background)
if (pushNotificationsEnabled && !isAppInForeground) {
    notificationManager.sendPushNotification(
        title = character.name,
        message = aiResponse.take(100) // First 100 chars
    )
}
```

### Example in ImageGenerationViewModel:
```kotlin
// After image generation completes
if (pushNotificationsEnabled) {
    notificationManager.sendPushNotification(
        title = "Image Ready",
        message = "Your image has been generated"
    )
}
```

## 3. How to Enable/Disable

Users control notifications in Settings → Profile & Account → Notifications:
- Toggle "Push Notifications" ON/OFF
- Toggle "Email Notifications" ON/OFF

Settings are saved in DataStore and persist across app restarts.

## 4. Checking if Enabled

```kotlin
// In any ViewModel
val preferences = dataStore.data.first()
val pushEnabled = preferences[booleanPreferencesKey("push_notifications")] ?: false
val emailEnabled = preferences[booleanPreferencesKey("email_notifications")] ?: false

if (pushEnabled) {
    // Send push notification
}

if (emailEnabled) {
    // Send email notification
}
```

## 5. Future Enhancements

For true remote push notifications (when app is closed):
- Integrate Firebase Cloud Messaging (FCM)
- Set up backend server to send notifications
- Register device tokens

For automatic emails:
- Integrate email service (SendGrid, AWS SES)
- Set up backend API endpoint
- Send emails from server
