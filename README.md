Enterprise-grade Android starter kit implementing modern IAM standards (OAuth2/OIDC) via Auth0 Universal Login, Kotlin Jetpack Compose, and JWT-backed FastAPI sessions

Overview

Android SaaS foundation with:

- Auth0 authentication
- JWT protected FastAPI backend
- Retrofit networking
- Persistent sessions
- Compose UI
- MVVM architecture
- Modular scalable structure

The project was built as a hands-on implementation of modern identity standards (OAuth2 / OpenID Connect), simulating a real-world authentication layer used in enterprise mobile applications.

🧠 Key Features
🔐 Login with Google OAuth2 via Auth0 Universal Login
🪪 JWT token handling for session management
🔁 Secure authentication callback using Deep Links
📱 Android-native integration with Auth0 SDK
🧭 Stable navigation flow after authentication
🧪 Tested on physical Android devices
🧩 Architecture

The authentication flow follows a standard identity provider pattern:

User → Auth0 (Identity Provider) → Google OAuth → JWT Token → Android App → Protected Session

Key components:

Identity Provider (IdP): Auth0 / Google
Client Application: Android App
Token Format: JWT (JSON Web Token)
Callback Mechanism: Deep Links (Intent Filters)

🔐 Security Concepts Implemented
OAuth2 Authorization Flow
OpenID Connect (OIDC) authentication layer
JWT signature validation (server-side responsibility)
Secure redirect handling via Android intent filters

🛠 Tech Stack
Kotlin
Android Studio
Auth0 SDK
Google OAuth2
JWT
Gradle

📲 How it works
User clicks login button
Redirects to Auth0 Universal Login
Auth0 delegates authentication to Google (if selected)
JWT is issued after successful authentication
Android app receives callback via deep link
Session is established inside the app

📦 Project Status

✔ Functional authentication flow
✔ Google OAuth integration
✔ JWT handling implemented

⚙️ Future improvements:

Role-based access control (RBAC)
Refresh token management
Backend API protection layer
Multi-factor authentication (MFA)

🧠 Learning Purpose

This project was developed to understand and implement:

Modern identity management systems
Mobile authentication flows
Enterprise-grade login architecture
Real-world OAuth2/OIDC integration patterns

Looking for custom mobile development or enterprise architecture consulting? Reach out via isanchf@gmail.com



📌 Notes
This project is a practical implementation of identity and access management concepts commonly used in enterprise systems such as Okta, Auth0, Azure AD.
