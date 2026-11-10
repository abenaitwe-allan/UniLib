# UniLib — Mobile Digital Class Library App

[![Android Native](https://img.shields.io/badge/Platform-Android%20Native-green.svg)](https://developer.android.com/)
[![Language-Java](https://img.shields.io/badge/Language-Java%2011-orange.svg)](https://www.java.com/)
[![Firebase-Auth%20%26%20Firestore](https://img.shields.io/badge/Database-Firebase%20Firestore-yellow.svg)](https://firebase.google.com/)
[![Supabase-Storage](https://img.shields.io/badge/Storage-Supabase%20Bucket-emerald.svg)](https://supabase.com/)
[![Build-Passing](https://img.shields.io/badge/Build-Passing-brightgreen.svg)]()

> **Tagline:** *Your class library, approved by your lecturers.*

UniLib is a native Android application designed to facilitate the uploading, reviewing, distribution, and offline reading of lecturer-approved academic study materials for university students, class presidents, lecturers, and system administrators.

---

## Table of Contents
1. [Project Overview & Problem Statement](#1-project-overview--problem-statement)
2. [User Roles & Key Requirements](#2-user-roles--key-requirements)
3. [Milestone 2 Features (Interaction, Navigation & Lists)](#3-milestone-2-features-interaction-navigation--lists)
4. [Milestone 3 Features (Data, Storage & Security)](#4-milestone-3-features-data-storage--security)
5. [Milestone 4 Features (REST API, Capabilities & Release)](#5-milestone-4-features-rest-api-capabilities--release)
6. [Offline Behavior & Network Resilience](#6-offline-behavior--network-resilience)
7. [Applied Security Controls Table](#7-applied-security-controls-table)
8. [Personal Data Protection & Privacy](#8-personal-data-protection--privacy)
9. [Technology Stack Comparison](#9-technology-stack-comparison)
10. [Testing Strategy, Multi-Device Results & Bug Log](#10-testing-strategy-multi-device-results--bug-log)
11. [Signed Release Build & Distribution Strategy](#11-signed-release-build--distribution-strategy)
12. [AI Tools Used](#12-ai-tools-used)
13. [Known Limitations](#13-known-limitations)
14. [Detailed Project Structure Description](#14-detailed-project-structure-description)
15. [Firestore Production Security Rules](#15-firestore-production-security-rules)

---

## 1. Project Overview & Problem Statement

### Problem Statement
In higher education institutions, students frequently struggle to locate verified, course-specific study materials (lecture notes, revision guides, past papers). Existing sharing channels (e.g., instant messaging groups) suffer from file expiration, lack of moderation, clutter, and unorganized content. Conversely, lecturers and class presidents lack a dedicated mobile platform to publish, organize, and moderate course resources.

### Solution Overview
**UniLib** provides a structured, role-governed mobile library where:
- **Students** register under an academic programme and view all approved study materials for their programme.
- **Class Presidents & Lecturers** share study resources.
- **Lecturers & Admins** review, approve, or reject pending uploads before students can access them.
- **Students** can read PDF documents in-app with high-DPI pinch-to-zoom, save documents for offline use, and receive notifications when new materials are approved for their programme.

---

## 2. User Roles & Key Requirements

| Role | Permissions & Core Workflows |
| :--- | :--- |
| **`STUDENT`** | Register under a Programme; view all approved programme documents; search documents; open PDF in-app; save documents offline; receive programme notifications. |
| **`PRESIDENT`** | View Programme Library; upload study documents (status defaults to `PENDING`); view upload history status (*Pending, Approved, Rejected*). |
| **`LECTURER`** | Upload study documents; select teaching programmes in profile; filter pending inbox by programme and course unit; approve or reject pending documents. |
| **`ADMIN`** | Complete system control: approve/reject pending uploads; create staff accounts; manage programmes and course units (add/delete with validation); filter directory by role; delete member accounts; delete documents. |

---

## 3. Milestone 2 Features (Interaction, Navigation & Lists)

### Form Validation & Input Feedback
- **Validation Engine (`com.example.unilib.util.Validators`)**:
  - **Full Name**: Letters and spaces only, length between 2 and 50 characters (`isValidName`).
  - **Email Address**: Android system regex pattern validation (`Patterns.EMAIL_ADDRESS`).
  - **Password**: Minimum 6 characters, must contain at least one letter and one number (`isValidPassword`, `hasLetterAndDigit`).
  - **Confirm Password**: Ensures exact matching string value.
- **User Feedback Controls**:
  - Direct inline error indicators on `TextInputLayout` fields.
  - Interactive `Snackbar` popups for network and server error messaging.
  - Action feedback `Toast` messages (e.g., *"Saved offline"*, *"Approved: [Title]"*, *"Account created"*).
  - Indeterminate `ProgressBar` indicators during async database/upload operations.

### State Preservation & Lifecycle Management
- Soft keyboard input preservation across screens using `adjustResize` in `AndroidManifest.xml`.
- Session state restoration on cold starts via `AuthRepository.restoreSession()`.
- Search query filter state and offline toggle chip state preserved across activity pause/resume cycles in `StudentHomeActivity` and `PresidentDashboardActivity`.

### Navigation & Task Flow Architecture
- **Screen Flow Transitions**:
  - `LoginActivity` $\rightarrow$ `RegisterActivity` $\rightarrow$ Role-Based Dashboard (`StudentHomeActivity`, `PresidentDashboardActivity`, `LecturerInboxActivity`, `AdminDashboardActivity`) $\rightarrow$ `UploadActivity`, `AddAccountActivity`, `ProgramManagerActivity`, `ProfileActivity`, `PdfViewerActivity`.
- **Dynamic Parameter Passing**: `RoleRouter.java` routes authenticated users to their specific dashboard based on their Firestore profile role and passes Intent extras (`EXTRA_NAME`, `EXTRA_ROLE`, `EXTRA_PROGRAM_ID`, `EXTRA_PROGRAM_NAME`).
- **Activity Stack Safety**: Logout actions apply `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK` flags to prevent back-navigation into authenticated screens.

### External App & Service Integration
1. **System File Manager (`Intent.ACTION_OPEN_DOCUMENT`)**:
   - Used in `UploadActivity.java` when tapping *"Choose PDF"*. Opens the Android system file picker to select local PDF files.
2. **External PDF Readers / Web Browsers (`Intent.ACTION_VIEW`)**:
   - Implemented in `DocumentActions.java` using `FileProvider` with temporary read permissions (`FLAG_GRANT_READ_URI_PERMISSION`).
3. **In-App Native PDF Reader (`PdfViewerActivity.java`)**:
   - Native PDF renderer using `android.graphics.pdf.PdfRenderer` featuring multi-page synchronized scroll, high-DPI crisp rendering, double-tap zoom, pinch-to-zoom, and toolbar `+` / `-` controls.

### Dynamic Collections, Custom Adapters & Reusable UI
- Custom `BaseAdapter` implementations driving dynamic lists:
  - `DocumentListAdapter`: Approved documents feed with offline save toggles.
  - `ReviewAdapter`: Pending review queue with Approve / Reject action buttons.
  - `UserAdapter`: Member directory list with interactive role modification options.
  - `AdminDocumentAdapter`: Programme document list with deletion triggers.
- Reusable UI Partials & Components:
  - Header partials: `part_dashboard_top.xml`, `part_home_top.xml`.
  - Form partial: `part_login_form.xml`.
  - Item layouts: `item_document.xml`, `item_review.xml`, `item_user.xml`, `item_document_status.xml`, `item_admin_document.xml`, `item_program.xml`, `item_course_unit.xml`, `item_pdf_page.xml`.

---

## 4. Milestone 3 Features (Data, Storage & Security)

### Full Data CRUD Operations
- **Create**:
  - Upload study documents (`UploadActivity` $\rightarrow$ `DocumentRepository.createDocument`).
  - Create staff accounts (`AddAccountActivity` $\rightarrow$ `UserRepository.createAccount`).
  - Add programmes and course units (`ProgramManagerActivity` $\rightarrow$ `CatalogRepository.createProgram`, `createCourseUnit`).
- **Read**:
  - Query approved documents, pending uploads, uploader history, member lists, and catalogue data from Firestore.
- **Update**:
  - Approve or reject pending documents (`DocumentRepository.review`).
  - Update user roles, active status (`UserRepository.updateRole`, `setActive`), and member profile details (`updateProfile`).
- **Delete**:
  - Admin document deletion (`DocumentRepository.deleteDocument`).
  - Admin member account deletion (`UserRepository.deleteUser`).
  - Admin programme and course unit deletion (`CatalogRepository.deleteProgram`, `deleteCourseUnit`).
  - **Confirmation Dialogs**: All destructive delete operations trigger mandatory `MaterialAlertDialogBuilder` confirmation dialogs before execution.

### Cloud & Local Persistence Strategy
- **Firebase Firestore**: Primary NoSQL cloud database (`users`, `documents`, `programs`, `courseUnits`, `activityLogs`).
- **Supabase Storage**: Hosts PDF binaries in a public bucket (`documents`) uploaded via `Storagerepository.java`.
- **Local Caching (`OfflineDocumentStore.java`)**: Persists PDF binaries in `filesDir/offline_docs/` and saves metadata in `SharedPreferences` (`unilib_offline_documents`).

### Authentication & Role-Based Access Control (RBAC)
- **Authentication**: `FirebaseAuth` handles sign-up, sign-in, token persistence, and password resets.
- **Authorisation**: 4 roles (`STUDENT`, `PRESIDENT`, `LECTURER`, `ADMIN`).
- **Runtime Verification**: All dashboards extend `BaseDashboardActivity.java` and invoke `verifyRole()`, querying Firestore at runtime to ensure the logged-in account holds the allowed role.

---

## 5. Milestone 4 Features (REST API, Capabilities & Release)

### REST API Integration & Async Execution
- **REST GET Requests (`DocumentRestApi.java`)**:
  - Fetches document records from Supabase REST API endpoint (`/rest/v1/documents`) asynchronously via OkHttp's `enqueue` thread pool.
- **JSON Parsing (`DocumentJsonParser.java`)**:
  - Parses JSON array responses (`JSONArray`, `JSONObject`) into strongly typed `Document` model lists.
- **REST POST Usage**:
  - Applied when uploading binary files to Supabase Storage and when creating staff accounts via Firebase Identity Toolkit Web API.

### Device Capabilities & Runtime Permissions
- **Capability:** Android System Tray Notifications (`NotificationManager` & `NotificationChannel`).
- **Runtime Permission:** Requests `android.permission.POST_NOTIFICATIONS` at runtime on Android 13+ (API 33+).
- **Graceful Denial Handling:** If notification permission is denied, calls execute safely inside `try-catch (SecurityException)` blocks without crashing.

### Closed-App Background WorkManager Sync
- **`DocCheckWorker.java` (`androidx.work.Worker`)**:
  - Runs a `PeriodicWorkRequest` every 15 minutes when connected to the internet.
  - Queries Firestore for newly approved study materials for the student's programme **even when the UniLib app is closed or killed**.
  - Posts a high-priority system tray notification (*"New Study Material Approved!"*).

---

## 6. Offline Behavior & Network Resilience

UniLib incorporates a **Network-Aware Architecture** (`NetworkUtils.java`):

1. **Automatic Network Detection**:
   - On launching `StudentHomeActivity` or `PresidentDashboardActivity`, the app checks internet connectivity.
2. **Offline Mode Fallback**:
   - If offline, the app automatically toggles the **Offline Mode** chip, queries `OfflineDocumentStore.java`, and presents saved PDF documents.
3. **In-App Offline Reading**:
   - Saved PDF files remain 100% readable in `PdfViewerActivity` without internet connectivity.
   - If an unsaved online document is tapped while offline, the app displays: *"No internet connection. Connect to internet to download and open this document."*

---

## 7. Applied Security Controls Table

| Security Risk | Applied Security Control | Code / Config Location |
| :--- | :--- | :--- |
| **Role Spoofing / Tampering** | Runtime profile verification on every dashboard launch + Firestore Security Rules. | `BaseDashboardActivity.java`<br>`firestore.rules` |
| **Data Interception in Transit** | Enforced HTTPS/TLS transport for all Firebase, Supabase, and REST API communications. | `HttpClientProvider.java`<br>`Storagerepository.java` |
| **Unsecure File Exposure** | Scoped `FileProvider` content URIs with temporary `FLAG_GRANT_READ_URI_PERMISSION`. | `DocumentActions.java`<br>`res/xml/file_paths.xml` |
| **Plaintext Password Storage** | Cryptographic hashing and tokenized auth managed by Firebase Auth (no local plaintext passwords). | `AuthRepository.java` |
| **Over-Privileged App Scope** | Least-privilege permission model requesting only required permissions (`INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`). | `AndroidManifest.xml` |

---

## 8. Personal Data Protection & Privacy

UniLib adheres to **Data Minimization Principles**:
- **Personal Data Collected**:
  - Full Name (used to identify uploaders/contributors).
  - Email Address (used for authentication and account login).
  - Enrolled Programme & ID (used to filter relevant course study materials).
  - Account Role (`STUDENT`, `PRESIDENT`, `LECTURER`, `ADMIN`).
- **Privacy Protection Controls**:
  - Passwords are handled exclusively by Firebase Authentication and are never saved in local storage or Firestore.
  - Member directories are restricted to authenticated users.
  - Admins can instantly deactivate or delete user accounts to revoke access.

---

## 9. Technology Stack Comparison

### Native Android (Java/Kotlin + Jetpack) vs Cross-Platform (Flutter / React Native)

| Factor | Native Android (Our Chosen Stack) | Cross-Platform (Flutter / React Native) |
| :--- | :--- | :--- |
| **Code Reuse** | Single platform (Android only). | High code reuse across iOS and Android. |
| **System API Access** | Direct access to native APIs (`PdfRenderer`, `FileProvider`, `WorkManager`, `NotificationManager`). | Requires third-party bridge plugins; risk of API lag or plugin bugs. |
| **Performance & Latency** | Zero bridge latency, instant UI rendering, smaller APK size. | JavaScript/Dart bridge serialization overhead. |
| **Background Processing** | First-class `WorkManager` support guarantees execution when closed. | Background execution requires complex native bridging. |

**Justification:** Native Android was selected for UniLib because the application relies heavily on core system services (`android.graphics.pdf.PdfRenderer`, `FileProvider`, `WorkManager`, and `NotificationChannel`). Native execution guarantees zero bridge latency, maximum rendering performance, and reliable background synchronization.

---

## 10. Testing Strategy, Multi-Device Results & Bug Log

### Testing Strategy & Verified Test Cases
10+ test cases were executed across unit tests, UI tests, accessibility checks, and multi-device configurations:

1. **`ValidatorsTest` (Unit Test):** Verified name, email regex, and alphanumeric password rules.
2. **`DocumentJsonParserTest` (Unit Test):** Verified JSON parsing of REST API responses.
3. **Login & Registration Flow (UI Test):** Tested input validation, password visibility toggling, and role navigation.
4. **Catalogue Management (UI Test):** Verified adding/deleting programmes and course units as Admin.
5. **PDF Zoom & Panning (UI Test):** Verified pinch-to-zoom, double-tap zoom, and 360-degree panning without clipping.
6. **Offline Mode Fallback (System Test):** Simulated airplane mode; verified automatic fallback to saved offline documents.
7. **Runtime Notification Permission (Device Test):** Tested notification permission prompts and graceful denial handling on Android 13+.
8. **Screen Rotation & Lifecycle (System Test):** Verified state retention across screen orientation changes.
9. **Accessibility Checks:** Verified screen reader labels (`contentDescription`), high contrast colors, and minimum 48dp touch targets.
10. **Multi-Device & OS Verification:**
    - Tested on **Android Emulator (API 34)** and **Physical Phone (API 33)**.
    - Verified layout adaptability across **Phone Portrait**, **Phone Landscape (`layout-land/`)**, and **Tablet (`sw600dp`)**.

### Bug Log & Fixed Defects
- **Defect 1 (Critical):** PDF page right and bottom edges clipped during zooming $\rightarrow$ *Fixed by setting dynamic layout params (`lp.height = bitmap.getHeight()`) and wrapping RecyclerView inside a `HorizontalScrollView`*.
- **Defect 2 (Critical):** Unauthenticated registering students blocked from reading programmes list $\rightarrow$ *Fixed by updating Firestore rules for `programs` to `allow read: if true;`*.
- **Defect 3 (Major):** Admin action buttons text cut off in 1 horizontal row $\rightarrow$ *Fixed by redesigning Admin buttons into a spacious 2x2 grid layout*.

---

## 11. Signed Release Build & Distribution Strategy

### Release Configuration
- **Version Configuration (`app/build.gradle.kts`)**:
  - `versionCode = 1`
  - `versionName = "1.0"`
- **Keystore Signing**:
  - Production release build configured with release keystore signing config (`unilib-release-key.jks`).

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("unilib-release-key.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = "unilib_key"
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
```

### Distribution & Update Strategy
- **Distribution:** Distribute the signed Android App Bundle (`.aab`) or APK via the Google Play Console **Internal Testing Track** and direct enterprise distribution.
- **Update Mechanism:** Integrate the **Google Play In-App Updates API** (`AppUpdateManager`) to prompt users with flexible or immediate in-app update dialogs whenever a new version is published.

---

## 12. AI Tools Used

During the development of UniLib, AI coding assistance (Android Studio Gemini / AI Assistant) was utilized for:
- Refactoring `PdfRenderer` high-DPI page rendering algorithms.
- Generating unit tests for form validation utilities.
- Troubleshooting Firestore security rules syntax and permission exceptions.
- Designing responsive XML layout structures.

---

## 13. Known Limitations

1. **Supabase Free Tier Storage Limit:** Large PDF files exceeding 50MB require chunked uploads or storage upgrades.
2. **WorkManager Execution Intervals:** Android OS batching algorithms may shift periodic WorkManager execution by a few minutes depending on battery optimization settings.

---

## 14. Detailed Project Structure Description

### Manifest File (`app/src/main/AndroidManifest.xml`)
- Declares app permissions (`INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`).
- Registers all 10 activities with soft keyboard `adjustResize` modes.
- Configures `FileProvider` (`authorities="${applicationId}.fileprovider"`) with paths defined in `res/xml/file_paths.xml`.
- Registers `FcmMessagingService` for background push notifications.

### Java Package Structure (`app/src/main/java/com/example/unilib/`)

```
com.example.unilib/
├── data/                       # Data Layer & Repositories
│   ├── AuthRepository.java         # Firebase Authentication & session management
│   ├── CatalogDefaults.java        # Fallback catalogue definitions
│   ├── CatalogLoader.java         # Async programme/course unit loader
│   ├── CatalogRepository.java     # Firestore CRUD for programmes and course units
│   ├── DocumentJsonParser.java    # JSON parser for REST API responses
│   ├── DocumentRepository.java    # Firestore CRUD for documents & reviews
│   ├── DocumentRestApi.java       # OkHttp REST API client
│   ├── OfflineDocumentStore.java  # Local PDF file caching & SharedPreferences
│   ├── ResultCallback.java        # Generic async result callback interface
│   ├── Storagerepository.java     # Supabase Storage bucket uploader
│   └── UserRepository.java        # Member directory, roles & profile CRUD
├── model/                      # Data Models & Enums
│   ├── ActivityLog.java           # Activity log record model
│   ├── CourseUnit.java            # Course unit model
│   ├── DocStatus.java             # Enum: PENDING, APPROVED, REJECTED
│   ├── Document.java              # Study document model
│   ├── Program.java               # Academic programme model
│   ├── Role.java                  # Enum: STUDENT, PRESIDENT, LECTURER, ADMIN
│   └── User.java                  # User profile model (with teachingPrograms)
├── network/                    # Network Utilities & Error Mapping
│   ├── ApiCallback.java           # Async API response callback interface
│   ├── ApiResult.java             # Sealed-style API result wrapper
│   ├── ErrorClassifier.java       # HTTP status code & exception classifier
│   ├── HttpClientProvider.java    # Shared OkHttpClient singleton
│   └── NetworkUtils.java          # Connectivity detector
├── ui/                         # User Interface Layer
│   ├── BaseDashboardActivity.java # Base dashboard chrome, role verifier & logout
│   ├── RoleRouter.java            # Dynamic activity router by account role
│   ├── admin/                     # Admin Screens
│   │   ├── AddAccountActivity.java     # Staff account creation
│   │   ├── AdminDashboardActivity.java # Admin dashboard (2x2 grid, role filter)
│   │   ├── AdminDocumentAdapter.java   # Admin programme document adapter
│   │   └── ProgramManagerActivity.java # Catalogue manager with deletion
│   ├── auth/                      # Auth Screens
│   │   ├── AuthViewModel.java          # MVVM Auth ViewModel
│   │   ├── LoginActivity.java          # Login activity with books background
│   │   └── RegisterActivity.java       # Student registration activity
│   ├── common/                    # Shared Adapters, Views & Actions
│   │   ├── DocumentActions.java        # Document launcher & FileProvider
│   │   ├── DocumentStatusAdapter.java  # Upload status adapter
│   │   ├── Labels.java                 # String & color formatters
│   │   ├── PdfViewerActivity.java      # Embedded native PDF viewer activity
│   │   ├── ProfileActivity.java        # Profile view/edit activity
│   │   ├── ProgramCoursePicker.java    # Dropdown cascade binder
│   │   ├── ReviewAdapter.java          # Pending review queue adapter
│   │   ├── SpinnerBinder.java          # Dropdown spinner binder
│   │   └── UserAdapter.java             # Member directory adapter
│   ├── lecturer/                  # Lecturer Screens
│   │   └── LecturerInboxActivity.java  # Lecturer inbox with programme/course filter
│   ├── president/                 # President Screens
│   │   └── PresidentDashboardActivity.java # President library & uploads activity
│   ├── student/                   # Student Screens
│   │   ├── DocumentAdapter.java        # Simple document adapter
│   │   ├── DocumentListAdapter.java    # Main library adapter with offline save
│   │   ├── LibraryViewModel.java       # Student library ViewModel
│   │   └── StudentHomeActivity.java    # Student home activity
│   └── upload/                    # Upload Screen
│       └── UploadActivity.java         # PDF picker & upload activity
└── util/                       # Utilities
    ├── DocCheckWorker.java         # WorkManager background sync worker
    ├── FcmMessagingService.java    # Firebase Cloud Messaging service
    ├── NotificationHelper.java     # Notification channel & system alert poster
    └── Validators.java            # Form validation utility
```

### Layout Resources (`app/src/main/res/layout/`)
- `activity_login.xml`, `activity_register.xml`: Login/Register screens with books background (`bg_books.png`).
- `activity_admin_dashboard.xml`: Admin control center with 2x2 grid buttons and role filter spinner.
- `activity_lecture_inbox.xml`: Lecturer inbox with programme and course unit filter spinners.
- `activity_president_dashboard_activivty.xml`: President screen with Library and My Uploads tabs.
- `activity_student_home.xml`: Student library feed with search bar and offline chip filter.
- `activity_pdf_viewer.xml`: In-app PDF reader with toolbar zoom controls.
- `activity_profile.xml`: Member profile viewer/editor with lecturer teaching programme selection.
- `activity_program_manager.xml`: Programme and course unit manager with deletion dialogs.
- `part_dashboard_top.xml`, `part_home_top.xml`: Reusable dashboard header bars.
- `item_document.xml`, `item_review.xml`, `item_user.xml`, `item_document_status.xml`, `item_admin_document.xml`, `item_program.xml`, `item_course_unit.xml`, `item_pdf_page.xml`: Reusable list item layouts.

### Values & Drawables (`app/src/main/res/`)
- `res/drawable/`: `bg_books.png`, `ic_profile_person.xml`, `ic_back.xml`, `ic_zoom_in.xml`, `ic_zoom_out.xml`, `bg_status_pending.xml`, `bg_status_approved.xml`, `bg_status_rejected.xml`, `bg_role_chip.xml`, `bg_program_pill.xml`.
- `res/values/colors.xml`: High-contrast color palette adhering to WCAG contrast standards.
- `res/values/themes.xml`: Material 3 theme definitions and button styles (`UniLib.Button.Outlined`).
- `res/values/strings.xml`: All externalized text strings for internationalization.

### Build & Security Configurations
- `app/build.gradle.kts`: Dependencies configuration (Firebase BOM, Room, WorkManager, OkHttp, Material 3).
- `firestore.rules`: Security rules enforcing authorization and public programme reading.

---

## 15. Firestore Production Security Rules

```rules
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function signedIn() { return request.auth != null; }
    function me() { return get(/databases/$(database)/documents/users/$(request.auth.uid)).data; }
    function role() { return me().role; }
    function isActive() { return me().active == true; }
    function isStaff() { return role() in ['PRESIDENT', 'LECTURER', 'ADMIN']; }
    function isReviewer() { return role() in ['LECTURER', 'ADMIN']; }

    match /users/{uid} {
      allow read: if signedIn();
      allow create: if signedIn() && request.auth.uid == uid
                    && request.resource.data.role == 'STUDENT'
                    && request.resource.data.active == true;
      allow update: if signedIn() && (
                      role() == 'ADMIN'
                      || (request.auth.uid == uid
                          && request.resource.data.role == resource.data.role
                          && request.resource.data.active == resource.data.active)
                    );
      allow delete: if signedIn() && role() == 'ADMIN';
    }

    match /documents/{docId} {
      allow read: if signedIn() && isActive() && (
                      resource.data.status == 'APPROVED'
                      || isReviewer()
                      || resource.data.uploaderId == request.auth.uid);

      allow create: if signedIn() && isActive() && isStaff()
                    && request.resource.data.uploaderId == request.auth.uid
                    && (request.resource.data.status == 'PENDING'
                        || (isReviewer() && request.resource.data.status == 'APPROVED'));

      allow update: if signedIn() && isActive() && (
                      isReviewer()
                      || (role() == 'PRESIDENT'
                          && resource.data.uploaderId == request.auth.uid
                          && resource.data.status in ['PENDING', 'REJECTED']
                          && request.resource.data.status == 'PENDING'));

      allow delete: if signedIn() && isActive() && (
                      role() == 'ADMIN'
                      || (role() == 'LECTURER' && resource.data.uploaderId == request.auth.uid)
                      || (role() == 'PRESIDENT' && resource.data.uploaderId == request.auth.uid
                          && resource.data.status == 'PENDING'));
    }

    match /activityLogs/{logId} {
      allow create: if signedIn() && isActive() && isStaff();
      allow read: if signedIn() && role() == 'ADMIN';
      allow update, delete: if false;
    }

    match /programs/{programId} {
      allow read: if true;
      allow create, update, delete: if signedIn() && isActive() && role() == 'ADMIN';
    }

    match /courseUnits/{unitId} {
      allow read: if true;
      allow create, update, delete: if signedIn() && isActive() && role() == 'ADMIN';
    }
  }
}
```
