# Real-time Database Architecture

## Overview

The SPU Cricket Club app uses a hybrid database architecture that combines:

1. **Firebase Realtime Database** for cloud storage and real-time synchronization
2. **Room Database** for local, offline-first data access and caching
3. **Two-way Synchronization** for seamless user experience regardless of network state

## Components

### 1. Room Database (Local)

- **AppDatabase**: Main Room database class that defines entities and DAOs
- **Entities**:
    - `PlayerEntity`: Player profile and statistics
    - `TeamEntity`: Team information and rosters
    - `MatchEntity`: Match details, scores, and state
    - `TournamentEntity`: Tournament information and structure
- **DAOs**:
    - Provide both LiveData (for UI observation) and synchronous methods
    - Support CRUD operations for all entities

### 2. Firebase Realtime Database (Cloud)

- Configured for offline persistence with `setPersistenceEnabled(true)`
- Data structure mirrors Room Entities for easy synchronization
- Real-time listeners provide instant updates from server

### 3. Synchronization Layer

- **DatabaseSyncManager**: Handles bi-directional sync between local and cloud
- **Real-time Listeners**: Update Room when Firebase data changes
- **Background Worker**: Ensures periodic sync even after network disruptions
- **Conflict Resolution**: Uses timestamp-based mechanisms to resolve conflicts

## Data Flow

### Writing Data (Create/Update)

1. Data is first written to Firebase Realtime Database
2. Firebase triggers registered ValueEventListeners
3. Listeners then update the local Room database
4. UI components observe LiveData from Room database for updates

### Reading Data

1. UI always reads from Room database through LiveData
2. Room database is kept up-to-date by Firebase listeners
3. Offline changes are queued and synchronized when connectivity returns

### Handling Network Issues

- **Offline Mode**: Firebase SDK provides offline capabilities automatically
- **Background Sync**: WorkManager schedules sync when network is restored
- **Error Handling**: Failed operations are retried with exponential backoff

## Implementation Details

### Firebase Offline Capabilities

```java
// Enable Firebase offline capabilities
firebaseDb.setPersistenceEnabled(true);
```

### Real-time Sync Listeners

```java
DatabaseReference ref = firebaseDb.getReference("path/to/data");
ref.addValueEventListener(new ValueEventListener() {
    @Override
    public void onDataChange(@NonNull DataSnapshot snapshot) {
        // Update Room database with new data
    }
    
    @Override
    public void onCancelled(@NonNull DatabaseError error) {
        // Handle errors
    }
});
```

### Periodic Background Sync

```java
PeriodicWorkRequest syncWorkRequest = new PeriodicWorkRequest.Builder(
    DatabaseSyncWorker.class, 
    15, 
    TimeUnit.MINUTES)
    .setConstraints(
        new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    )
    .build();

WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "database_sync_work",
    ExistingPeriodicWorkPolicy.KEEP,
    syncWorkRequest
);
```

## Best Practices

1. **Always read from Room**: UI should always read from local database
2. **Write to Firebase first**: Writes should go to Firebase, which then update Room
3. **Use LiveData**: LiveData provides lifecycle-aware data observation
4. **Handle conflicts**: Use server timestamps for conflict resolution
5. **Optimize sync frequency**: Balance freshness with battery and network usage
