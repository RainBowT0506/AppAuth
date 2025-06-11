### README: OAuth Implementation Example

This repository demonstrates an implementation of OAuth 2.0 authentication using the AppAuth library in an Android application. The app is written in Kotlin and uses modern Android tools like Jetpack Compose, ViewModel, DataStore, and Coroutines.

---

#### Features

* **Google Login with Brand Account Selection**: Implements Google OAuth 2.0 login and supports choosing a YouTube brand account during authentication.
* **OAuth 2.0 Authentication**: Uses `net.openid.appauth` for secure and customizable OAuth 2.0 authentication flows.
* **Access Token Management**: Manages token persistence using `DataStore`.
* **Token Refresh**: Supports refreshing tokens before they expire.
* **Token Expiry Check**: Displays the remaining validity period of access tokens.
* **Jetpack Compose UI**: Provides a simple UI to interact with authentication flows.

---

#### Setup
1. Update the `AuthViewModel` with your OAuth provider details:

   ```kotlin
   private val clientId = "YOUR_CLIENT_ID"
   private val redirectUri = Uri.parse("YOUR_REDIRECT_URI")
   private val authEndpoint = Uri.parse("YOUR_AUTHORIZATION_ENDPOINT")
   private val tokenEndpoint = Uri.parse("YOUR_TOKEN_ENDPOINT")
   ```

2. Register the redirect URI in your `AndroidManifest.xml`:

   ```xml
   <intent-filter>
       <action android:name="android.intent.action.VIEW"/>
       <category android:name="android.intent.category.DEFAULT"/>
       <category android:name="android.intent.category.BROWSABLE"/>
       <data
           android:scheme="YOUR_SCHEME"
           android:host="YOUR_HOST"/>
   </intent-filter>
   ```
---

#### AuthStateManager

This project includes an implementation of the `AuthStateManager`, inspired by Google's [FHIR App Examples](https://github.com/google/fhir-app-examples/blob/main/demo/app/src/main/java/com/google/fhir/examples/demo/auth/AuthStateManager.kt). The `AuthStateManager` provides a thread-safe mechanism for managing `AuthState`, including token persistence and updates during the authorization process.

---

#### Dependencies

* [AppAuth Library](https://github.com/openid/AppAuth-Android): OAuth 2.0 client library for Android.
* [DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore): Jetpack library for storing key-value pairs.
* [Jetpack Compose](https://developer.android.com/jetpack/compose): Android's modern UI toolkit.

Add these dependencies to your `build.gradle`:

```gradle
implementation "net.openid:appauth:0.11.1"
implementation "androidx.datastore:datastore-preferences:1.1.0"
```

---

#### How It Works

1. **Authentication Flow**:

   * The user initiates login via the "Login with OAuth" button.
   * AppAuth handles the OAuth flow and redirects back with an authorization code.
   * The app exchanges the code for an access token using the token endpoint.

2. **YouTube Brand Account Selection**:

   * During login, users can select a YouTube brand account if their Google account has associated brand accounts.

3. **Token Persistence**:

   * The access token and its expiration time are stored in `DataStore`.
   * This ensures tokens persist across app restarts.

4. **Token Refresh**:

   * The app refreshes the token when it is about to expire.
   * Users can manually refresh tokens using the "Refresh Access Token" button.

5. **UI Feedback**:

   * Displays the access token and its remaining validity in the UI.
   * Provides error feedback for failed authorization or token exchange.

---

#### Screenshots

Include screenshots of your app in different states:

1. **Login Screen**.
2. **Authenticated Screen** (displaying the access token).
3. **YouTube Brand Account Selection Screen**.
4. **Token Expiry Countdown**.

---
