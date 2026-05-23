# QuizifyAI

QuizifyAI is a minimal Android MVP for students and teachers to upload academic PDFs, extract readable text, generate multiple-choice quizzes with Gemini, and save quiz history in Firebase.

## Stack

- Android, Kotlin, Jetpack Compose, Material 3
- Navigation Compose, ViewModel, StateFlow, Coroutines
- Firebase Authentication, Firestore, Storage, Cloud Functions v2
- Gemini API through Retrofit and Kotlin serialization
- Gradle Kotlin DSL with version catalog

## Project Structure

```text
app/src/main/java/com/quizifyai/
|-- data/
|   |-- remote/
|   |-- repository/
|   `-- model/
|-- domain/
|   |-- model/
|   |-- repository/
|   `-- usecase/
|-- presentation/
|   |-- screens/
|   |-- components/
|   |-- navigation/
|   `-- viewmodel/
|-- utils/
`-- MainActivity.kt

functions/
|-- index.js
`-- package.json
```

## App Flow

1. User logs in or registers with Firebase email/password auth.
2. User picks a PDF from device storage.
3. The app validates the file as a PDF.
4. The app extracts embedded PDF text with a lightweight extractor and `PdfRenderer` validation.
5. The PDF is uploaded to Firebase Storage under `pdfs/{userId}/`.
6. Gemini generates 10 multiple-choice questions as structured JSON.
7. The quiz is saved in Firestore under `quizzes/`, with an entry in `history/`.
8. Home shows the user's previous generated quizzes.

The Android MVP generates quizzes directly through Gemini using the key in `local.properties`. The Cloud Function is also included for a safer server-side generation flow. The app currently uploads PDFs with `generateQuiz=false` metadata to avoid duplicate quizzes if the function is deployed. For production, prefer the Cloud Function path because API keys packaged into Android apps can be extracted.

## Firebase Setup

1. Create a Firebase project.
2. Add an Android app with package name:

```text
com.quizifyai
```

3. Download `google-services.json` and place it in:

```text
app/google-services.json
```

4. Enable Firebase Authentication:

```text
Authentication > Sign-in method > Email/Password
```

5. Create Firestore in production or test mode.
6. Create Firebase Storage.
7. Deploy rules:

```bash
firebase deploy --only firestore:rules,storage
```

## Gemini Setup

Copy the example properties file:

```bash
cp local.properties.example local.properties
```

Add your Gemini API key:

```properties
GEMINI_API_KEY=your_key_here
```

Do not commit `local.properties`.

## Cloud Functions Setup

Install function dependencies:

```bash
cd functions
npm install
cd ..
```

Set the Gemini secret for Cloud Functions:

```bash
firebase functions:secrets:set GEMINI_API_KEY
```

Deploy the function:

```bash
firebase deploy --only functions
```

The function triggers on PDF uploads, extracts text, sends it to Gemini, and writes the generated quiz to Firestore. To make the app use the server-side flow as the main path, change the upload metadata in `GenerateQuizFromPdfUseCase` to `generateOnServer = true` and remove the direct Gemini generation call in that use case.

## Emulator Testing

Start Firebase emulators:

```bash
firebase emulators:start
```

The configured ports are:

- Auth: `9099`
- Firestore: `8080`
- Functions: `5001`
- Storage: `9199`
- Emulator UI: `4000`

The Android app is not hardwired to emulator hosts yet. Add emulator connection calls in `AppContainer` if you want local-only Android testing.

## Run The Android App

Requirements:

- Android Studio with JDK 17
- Android SDK 36
- `app/google-services.json`
- `local.properties` with `GEMINI_API_KEY`

Build debug APK:

```bash
./gradlew assembleDebug
```

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

Install on a connected device or emulator:

```bash
./gradlew installDebug
```

## Generate APK

Debug APK:

```bash
./gradlew assembleDebug
```

Output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Release APK:

```bash
./gradlew assembleRelease
```

Add proper signing config before shipping a release build.

## Notes

- Scanned image-only PDFs will not produce text without OCR.
- PDF extraction is capped to keep Gemini usage predictable.
- Quiz list loading is limited to 25 documents for free-tier friendliness.
- Firebase KTX artifacts are intentionally not used because current Firebase BoM versions removed the old KTX modules.
- AdMob is not implemented yet, but the architecture leaves room to add it later.
