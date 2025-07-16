# SPU Cricket Club App

## Real-time Database Architecture

The SPU Cricket Club app uses a hybrid database architecture that combines Firebase Realtime
Database and Room for an optimized experience:

### Key Components

1. **Firebase Realtime Database** provides cloud storage and real-time synchronization
2. **Room Database** delivers local, offline-first data access and caching
3. **Two-way Synchronization** ensures seamless user experience regardless of network state

### Data Flow Architecture

- **Write Operations**: Data is written to Firebase first, which then updates Room via listeners
- **Read Operations**: UI always reads from Room database for consistent offline performance
- **Synchronization**: WorkManager handles background sync when network connectivity returns
- **Conflict Resolution**: Server timestamps are used to resolve conflicts

### Technical Benefits

- Offline-first approach guarantees app functionality without network
- Real-time updates display live match data instantaneously
- Efficient battery use through optimized sync schedules
- Automatic conflict resolution between multiple devices

## AI Player Recognition System

The SPU Cricket Club app includes a cutting-edge AI-powered player recognition system that
automatically identifies players during matches and updates statistics in real-time.

### Features

* **Face Recognition**: Identifies players using facial recognition technology
* **Pose Detection**: Analyzes player stance to determine if they are batting or bowling
* **Jersey Number Recognition**: Uses text recognition to identify jersey numbers
* **Auto-Updating Statistics**: When a player is recognized, their stats are automatically updated

### How It Works

1. The system uses ML Kit and TensorFlow Lite to power the AI capabilities
2. The camera feed is processed in real-time to detect faces and poses
3. Player faces are matched against a database of registered player profiles
4. When a player is recognized with high confidence, they are automatically selected in the match
   interface

### Technical Implementation

The player recognition system consists of several components:

* `PlayerRecognitionEngine.java`: Core AI engine that processes camera frames and identifies players
* `PlayerDatabase.java`: Storage system for player face images and recognition data
* Camera integration in `CricketScoringActivity.java` to capture the live feed

### Privacy and Performance

* All player recognition happens on-device for privacy and performance
* The system is optimized to minimize battery consumption
* Recognition processing is throttled to analyze frames at appropriate intervals

### Usage

1. During match scoring, enable the recognition system by tapping the "Enable Recognition" button
2. Point the camera at players on the field
3. When a player is recognized, they will be automatically selected in the interface
4. Continue scoring normally with the added benefit of AI player identification

This feature brings professional broadcast-level technology to grassroots cricket!

### Training the Recognition System

The system needs to be trained with player faces before it can recognize them:

1. Tap "Register Player Faces" button or select it from the main menu
2. Choose an existing player or select "New Player..." to add a new one
3. Position the player's face in the camera frame
4. Tap "Capture" to take a photo
5. Review the captured image and tap "Save" if it's good, or "Cancel" to try again
6. Repeat for all players who will participate in matches

For best results:

- Capture faces in good lighting
- Take multiple photos of each player from different angles
- Update photos periodically as players' appearances may change

### Privacy and Performance

* All player recognition happens on-device for privacy and performance
* The system is optimized to minimize battery consumption
* Recognition processing is throttled to analyze frames at appropriate intervals

## Recent Improvements

The app has been significantly upgraded with:

1. **Robust Database Architecture**
    - Room database implementation for offline data access
    - Entity-relationship model for cricket data
    - Type converters for complex data types
    - Database seeding for initial data population

2. **Improved Data Management**
    - Repository pattern for clean data access
    - Synchronization between Firebase and local storage
    - Background data refresh with WorkManager
    - Efficient data caching strategies

3. **Enhanced Reliability**
    - Firebase Crashlytics integration for crash reporting and analytics
    - Comprehensive error handling throughout the app
    - Network connectivity monitoring
    - Crash recovery mechanisms
    - Error throttling to prevent excessive logging

4. **Performance Optimizations**
    - Background data processing with ExecutorService
    - Efficient database queries
    - Data model mapping for faster processing
    - Memory usage optimizations

5. **Better User Experience**
    - Offline mode for continued functionality without internet
    - Smoother transitions between online and offline states
    - Improved data consistency and state management
    - Real-time synchronization for multi-user scenarios
