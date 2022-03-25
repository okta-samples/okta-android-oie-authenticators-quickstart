# Embedded Auth with Okta OIE Android SDK - Kotlin Sample Application

> :grey_exclamation: The sample uses an SDK that requires usage of the Okta Identity Engine. This functionality is in general availability but is being gradually rolled out to customers. If you want
to request to gain access to the Okta Identity Engine, please reach out to your account manager. If you do not have an account manager, please reach out to oie@okta.com for more information.

This app supports (tested)

* Login with username/password
* Registration with Google authenticator

## Prerequisites

Before running this sample, you will need the following:

- [The Okta CLI Tool](https://github.com/okta/okta-cli#installation)
- An Okta Developer Account (create one using `okta register`, or configure an existing one with `okta login`). You can also register for a free account at [developer.okta.com](https://developer.okta.com/). Select **Create Free Account** and fill in the forms to complete the registration process. Once you are done and logged in, you will see your Okta Developer Console.
- Android Studio
- A virtual device emulator or a real device connected to work with Android Studio
- Java 11

## Installation & Running The App

To create an app in your Okta org, run `okta apps create` and use the settings below.

- Application type: **Native App (mobile)**
- Login Redirect URI: `com.okta.sample.android:/login`
- Logout Redirect URI: `com.okta.sample.android:/logout`

The **issuer** is the URL of the authorization server that will perform authentication.  
All Developer Accounts have a "default" authorization server.  
The issuer is a combination of your Org URL (found in the upper right of the console home page) and `/oauth2/default`. For example, `https://dev-133337.okta.com/oauth2/default`.

## Get the Code

```shell
git clone https://github.com/okta-samples/okta-android-oie-authenticators-quickstart.git
cd okta-android-oie-authenticators-quickstart
```

Update `okta.properties` file with your Okta Issuer URL and client ID.

## Run the App

Run the app on emulator or hardware device from Android Studio (**shift+F10** on Linux/Windows |
**Ctrl + R** on macOS)

If you see a home page with a login screen, then things are working! Clicking the Login button will start the IDX authentication flow

You can sign in with the same account that you created when signing up for your developer org, or you can use a known username and password from your Okta Directory.

To test MFA-authenticators, create a new user in Okta admin console and try log in in with that user so that you get a chance to enroll MFA