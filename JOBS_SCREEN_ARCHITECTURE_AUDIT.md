# 🔍 Jobs Screen Architecture Audit

**Report Type:** READ-ONLY, NON-MODIFYING Analysis  
**Generated:** $(date)  
**Purpose:** Baseline architectural understanding before refactoring to match HomeScreen's Google-grade architecture

---

## 1️⃣ FILE & MODULE MAP

### Core Jobs Screen Files

| File Path | Package | Responsibility | Lines |
|-----------|---------|----------------|-------|
| `ui/screen/main/JobsScreen.kt` | `com.nextserve.serveitpartnernew.ui.screen.main` | Main UI composition, tabs, filtering, job listing | 1,301 |
| `ui/viewmodel/JobsViewModel.kt` | `com.nextserve.serveitpartnernew.ui.viewmodel` | State management, business logic, filtering, sorting | 518 |

### Tab-Level Composables

| File Path | Package | Responsibility |
|-----------|---------|----------------|
| `JobsScreen.kt` (NewJobsTab) | `com.nextserve.serveitpartnernew.ui.screen.main` | New jobs tab composable (private) |
| `JobsScreen.kt` (HistoryTab) | `com.nextserve.serveitpartnernew.ui.screen.main` | History/completed jobs tab composable (private) |

### Item-Level Composables

| File Path | Package | Responsibility |
|-----------|---------|----------------|
| `JobsScreen.kt` (NewJobCard) | `com.nextserve.serveitpartnernew.ui.screen.main` | New job card display (private) |
| `JobsScreen.kt` (CompletedJobCard) | `com.nextserve.serveitpartnernew.ui.screen.main` | Completed job card display (private) |
| `JobsScreen.kt` (StatusBadge) | `com.nextserve.serveitpartnernew.ui.screen.main` | Status badge component (private) |
| `JobsScreen.kt` (OngoingJobBanner) | `com.nextserve.serveitpartnernew.ui.screen.main` | Ongoing job warning banner (private) |
| `JobsScreen.kt` (AcceptJobDialog) | `com.nextserve.serveitpartnernew.ui.screen.main` | Job acceptance dialog (private) |

### Shared Components Used

| File Path | Package | Responsibility |
|-----------|---------|----------------|
| `ui/components/EmptyState.kt` | `com.nextserve.serveitpartnernew.ui.components` | Empty state placeholder |
| `ui/components/ErrorState.kt` | `com.nextserve.serveitpartnernew.ui.components` | Error state with retry |

### Data Layer Files

| File Path | Package | Responsibility |
|-----------|---------|----------------|
| `data/repository/JobsRepository.kt` | `com.nextserve.serveitpartnernew.data.repository` | Firestore data access, job queries |
| `data/model/Job.kt` | `com.nextserve.serveitpartnernew.data.model` | Job data model |
| `data/model/JobInboxEntry.kt` | `com.nextserve.serveitpartnernew.data.model` | Inbox entry model (for optimized job discovery) |

---

## 2️⃣ FEATURE INVENTORY

### Core Features

#### 1. Job Listing
- **Location:** `NewJobsTab`, `HistoryTab`
- **Implementation:** ViewModel-driven
- **Mandatory:** Yes
- **Details:**
  - New Jobs: Real-time via `listenToNewJobs()` Flow
  - History: Paginated via `getCompletedJobs()` with limit
  - Both use LazyColumn for display

#### 2. Tab Navigation
- **Location:** `JobsScreen` composable
- **Implementation:** UI-driven (HorizontalPager + TabRow)
- **Mandatory:** Yes
- **Details:**
  - Two tabs: "New Jobs" (page 0) and "History" (page 1)
  - HorizontalPager for swipe navigation
  - TabRow for tab selection
  - Bidirectional sync: tab selection ↔ pager state

#### 3. Pull-to-Refresh
- **Location:** `NewJobsTab` (wrapped in PullToRefreshBox)
- **Implementation:** UI-driven
- **Mandatory:** Yes (for New Jobs tab)
- **Details:**
  - Only implemented in NewJobsTab
  - Not present in HistoryTab
  - Calls `viewModel.loadNewJobs()` on refresh

#### 4. Pagination / Load More
- **Location:** `HistoryTab`
- **Implementation:** ViewModel-driven
- **Mandatory:** Yes (for History tab)
- **Details:**
  - Auto-loads when scrolled to last item
  - Uses `rememberLazyListState()` to detect scroll position
  - Loads 20 items per page
  - Shows loading indicator and error retry for pagination

#### 5. Job Filtering
- **Location:** ViewModel (`applyFiltersAndSort()`)
- **Implementation:** ViewModel-driven
- **Mandatory:** Optional (user-initiated)
- **Filter Types:**
  - Service type filter (`selectedServiceFilter`)
  - Maximum distance filter (`maxDistanceFilter`)
  - Price range filter (`minPriceFilter`, `maxPriceFilter`)
  - Search query (`searchQuery`) - searches service name, location, booking ID, user name
- **UI:** Filter buttons in header (🔍 search, ⚙️ filters) - declared but dialogs not implemented

#### 6. Job Sorting
- **Location:** ViewModel (`applyFiltersAndSort()`)
- **Implementation:** ViewModel-driven
- **Mandatory:** Optional (user-initiated)
- **Sort Options:**
  - Distance (nearest first)
  - Price (highest first)
  - Time (newest first)
- **UI:** Sort button (⇅) in header cycles through options

#### 7. Accept / Reject Actions
- **Location:** ViewModel + UI
- **Implementation:** ViewModel-driven
- **Mandatory:** Yes
- **Details:**
  - Accept: Shows dialog, calls `viewModel.acceptJob()`
  - Reject: Local only, calls `viewModel.rejectJob()` (no Firestore write)
  - Accept button disabled if `hasOngoingJob == true`
  - Shows loading state during acceptance

#### 8. Navigation to Job Details
- **Location:** UI callback
- **Implementation:** UI-driven
- **Mandatory:** Yes
- **Details:**
  - Callback: `onNavigateToJobDetails(bookingId, customerPhoneNumber, bookingIndex)`
  - For new jobs: `bookingIndex = null`
  - For history: `bookingIndex = null`
  - Navigates to `"jobDetails/{bookingId}/{customerPhoneNumber}/{bookingIndex}"` route

#### 9. Loading State Handling
- **Location:** Both tabs
- **Implementation:** ViewModel-driven
- **Mandatory:** Yes
- **Details:**
  - New Jobs: `isLoadingNewJobs` - shows CircularProgressIndicator
  - History: `isLoadingHistory` - shows CircularProgressIndicator
  - History pagination: `isLoadingMoreHistory` - shows small indicator at bottom

#### 10. Error State Handling
- **Location:** Both tabs
- **Implementation:** ViewModel-driven
- **Mandatory:** Yes
- **Details:**
  - New Jobs: Shows ErrorState if error and no jobs
  - History: Shows error message in LazyColumn item if pagination fails
  - Error messages shown via Snackbar (auto-dismissed)

#### 11. Empty State Handling
- **Location:** Both tabs
- **Implementation:** ViewModel-driven
- **Mandatory:** Yes
- **Details:**
  - New Jobs: "No New Jobs" message
  - History: "No Completed Jobs" message
  - Uses shared `EmptyState` component

#### 12. Ongoing Job Warning
- **Location:** `NewJobsTab`
- **Implementation:** ViewModel-driven
- **Mandatory:** Conditional (only if `hasOngoingJob == true`)
- **Details:**
  - Shows banner: "Complete ongoing job to accept new requests"
  - Disables accept buttons on all job cards

#### 13. Filter Indicator
- **Location:** `NewJobsTab` LazyColumn
- **Implementation:** UI-driven
- **Mandatory:** Conditional (only if filters active)
- **Details:**
  - Shows "Filters applied" card when any filter is active
  - Provides "Clear" button to remove filters

#### 14. Clear Filters Action
- **Location:** ViewModel + UI
- **Implementation:** ViewModel-driven
- **Mandatory:** Optional (user-initiated)
- **Details:**
  - ExtendedFloatingActionButton appears when filters active
  - Calls `viewModel.clearFilters()`
  - Resets all filter state to defaults

### Incomplete Features (Declared but Not Implemented)

#### Search Dialog
- **Status:** UI state declared (`showSearch`), button exists, but no dialog/bottom sheet implemented
- **Location:** Header button (🔍) sets `showSearch = true` but nothing consumes it

#### Filter Dialog
- **Status:** UI state declared (`showFilters`), button exists, but no dialog/bottom sheet implemented
- **Location:** Header button (⚙️) sets `showFilters = true` but nothing consumes it

---

## 3️⃣ COMPOSABLE / VIEW HIERARCHY (TOP → BOTTOM)

### Complete Tree Structure

```
MainAppScreen (Navigation Host)
└── Scaffold (with bottomBar: BottomNavigationBar)
    └── NavHost
        └── JobsScreen [MAIN]
            ├── Scaffold (with topBar, snackbarHost, floatingActionButton)
            │   ├── topBar: Column
            │   │   ├── Surface (Gradient header with title + action buttons)
            │   │   │   ├── Row
            │   │   │   │   ├── Text("Available Jobs")
            │   │   │   │   └── Row (Action buttons)
            │   │   │   │       ├── FilledTonalIconButton (🔍 Search - incomplete)
            │   │   │   │       ├── FilledTonalIconButton (⚙️ Filters - incomplete)
            │   │   │   │       └── FilledTonalIconButton (⇅ Sort)
            │   │   └── TabRow
            │   │       ├── Tab("New Jobs")
            │   │       └── Tab("History")
            │   ├── snackbarHost: SnackbarHost
            │   ├── floatingActionButton: ExtendedFloatingActionButton (conditional - "Clear Filters")
            │   └── content: HorizontalPager
            │       ├── [PAGE 0] NewJobsTab
            │       │   └── Box
            │       │       └── [CONDITIONAL] when:
            │       │           ├── ErrorState (if error && !loading && jobs.isEmpty)
            │       │           ├── CircularProgressIndicator (if loading)
            │       │           ├── EmptyState (if jobs.isEmpty)
            │       │           └── Column (else - normal content)
            │       │               ├── [CONDITIONAL] OngoingJobBanner (if hasOngoingJob)
            │       │               └── PullToRefreshBox
            │       │                   └── LazyColumn
            │       │                       ├── [CONDITIONAL] item: "Filters applied" Card
            │       │                       └── items: NewJobCard (per job)
            │       │                           ├── StatusBadge
            │       │                           ├── Priority indicator (if high priority)
            │       │                           ├── Expiry indicator (if pending)
            │       │                           └── Action buttons (Accept/Reject)
            │       └── [PAGE 1] HistoryTab
            │           └── Box
            │               └── [CONDITIONAL] when:
            │                   ├── CircularProgressIndicator (if loading && jobs.isEmpty)
            │                   ├── EmptyState (if jobs.isEmpty)
            │                   └── LazyColumn (else - normal content)
            │                       ├── items: CompletedJobCard (per job)
            │                       ├── [CONDITIONAL] item: Loading indicator (if isLoadingMore)
            │                       └── [CONDITIONAL] item: Error retry (if pagination error)
            └── [CONDITIONAL] AcceptJobDialog (if showAcceptDialog != null)
```

### Composable Characteristics

#### Stateful Composables
- **JobsScreen** - Collects ViewModel state via `collectAsState()`, manages tab/pager state
- **NewJobsTab** - Receives state as parameters, manages local UI state
- **HistoryTab** - Receives state as parameters, manages `rememberLazyListState()` for pagination

#### Stateless Composables
- **NewJobCard** - Receives all data as parameters
- **CompletedJobCard** - Receives all data as parameters
- **StatusBadge** - Receives status as parameter
- **OngoingJobBanner** - Pure display component
- **AcceptJobDialog** - Receives job and callbacks as parameters

---

## 4️⃣ SCROLL & LAYOUT MODEL (OBSERVATIONAL)

### Scroll Container Architecture

**Primary Containers:**
1. **HorizontalPager** (Main container)
   - Location: `JobsScreen` Scaffold content
   - Purpose: Tab navigation via swipe
   - Pages: 2 (New Jobs, History)
   - Modifier: `fillMaxSize().padding(paddingValues)`

2. **LazyColumn** (NewJobsTab)
   - Location: Inside `PullToRefreshBox` within `NewJobsTab`
   - State: No explicit `rememberLazyListState()` (uses default)
   - Content Padding: `PaddingValues(16.dp)`
   - Vertical Arrangement: `Arrangement.spacedBy(12.dp)`

3. **LazyColumn** (HistoryTab)
   - Location: Directly in `HistoryTab` Box
   - State: `rememberLazyListState()` - created per tab instance
   - Content Padding: `PaddingValues(16.dp)`
   - Vertical Arrangement: `Arrangement.spacedBy(12.dp)`

### Multiple Vertical Scroll Containers

**Count:** 2 LazyColumns (one per tab)  
**Active Simultaneously:** ❌ **NO** - Only one tab visible at a time via HorizontalPager  
**Conditional Rendering:** ✅ **YES** - Tabs are separate pages in HorizontalPager  
**Impact:** Each tab has its own LazyColumn instance, scroll state is not shared

### Padding & Insets Application

**Scaffold Hierarchy:**
1. **MainAppScreen Scaffold** (with `bottomBar`)
   - Provides `paddingValues` (includes bottom navigation bar padding)
   - **NOT PASSED** to JobsScreen (unlike HomeScreen)

2. **JobsScreen Scaffold** (with `topBar`, `floatingActionButton`)
   - Provides `paddingValues` (includes top app bar padding)
   - Applied to HorizontalPager: `modifier = Modifier.fillMaxSize().padding(paddingValues)`

**Observation:** JobsScreen does NOT receive parent padding from MainAppScreen. Only uses its own Scaffold padding.

**LazyColumn Padding:**
- NewJobsTab: `contentPadding = PaddingValues(16.dp)` - fixed internal padding
- HistoryTab: `contentPadding = PaddingValues(16.dp)` - fixed internal padding
- No bottom navigation padding added to contentPadding

### Content Fitting Within Scaffold

**Scaffold Structure:**
- TopBar: Custom gradient header + TabRow (variable height)
- Content: HorizontalPager fills remaining space
- FloatingActionButton: Conditional (only when filters active)
- BottomNavigationBar: Handled by parent Scaffold (MainAppScreen)

**LazyColumn Constraints:**
- NewJobsTab: Inside PullToRefreshBox, inside Column, inside Box
- HistoryTab: Directly in Box
- Both: Width: `fillMaxWidth()` (implicit), Height: Constrained by parent containers

---

## 5️⃣ STATE FLOW ANALYSIS

### ViewModel Architecture

**ViewModel:** `JobsViewModel`  
**State Holder:** `StateFlow<JobsUiState>`  
**State Collection:** `viewModel.uiState.collectAsState()` in JobsScreen composable

### State Model: JobsUiState

```kotlin
data class JobsUiState(
    val newJobs: List<Job> = emptyList(),                    // Raw new jobs from repository
    val filteredJobs: List<Job> = emptyList(),               // Filtered and sorted jobs
    val inboxEntries: List<JobInboxEntry> = emptyList(),     // Inbox entries (optimized method)
    val completedJobs: List<Job> = emptyList(),              // History jobs (paginated)
    val isLoadingNewJobs: Boolean = false,                   // New jobs loading
    val isLoadingHistory: Boolean = false,                   // History initial load
    val isLoadingMoreHistory: Boolean = false,               // History pagination loading
    val hasOngoingJob: Boolean = false,                     // Ongoing job flag
    val errorMessage: String? = null,                        // Error display
    val rejectedJobIds: Set<String> = emptySet(),            // Local rejected jobs
    val acceptingJobId: String? = null,                      // Job acceptance tracking
    val hasMoreHistory: Boolean = true,                      // Pagination flag
    val isOffline: Boolean = false,                          // Network status
    val useInbox: Boolean = false,                           // Inbox method flag (disabled)
    // Filter and sort state
    val selectedServiceFilter: String? = null,                // Service type filter
    val maxDistanceFilter: Double? = null,                   // Max distance filter
    val minPriceFilter: Double? = null,                     // Min price filter
    val maxPriceFilter: Double? = null,                      // Max price filter
    val sortBy: String = "distance",                         // Sort option
    val searchQuery: String = "",                            // Search text
    val acceptanceRate: Double? = null                       // Provider acceptance rate
)
```

### State Collection Pattern

**Location:** `JobsScreen.kt` line 127

```kotlin
val uiState by viewModel.uiState.collectAsState()
```

**Observation:** State is collected once at top level. Individual fields are accessed directly from `uiState` throughout the composable. Any change to `JobsUiState` triggers recomposition of entire JobsScreen.

### State → UI Mapping

| State Field | UI Section | Conditional Logic |
|-------------|------------|-------------------|
| `isLoadingNewJobs` | NewJobsTab loading | `if (isLoading)` → CircularProgressIndicator |
| `isLoadingHistory` | HistoryTab loading | `if (isLoading && jobs.isEmpty())` → CircularProgressIndicator |
| `isLoadingMoreHistory` | HistoryTab pagination | `if (isLoadingMore)` → Loading indicator item |
| `errorMessage` | NewJobsTab error | `if (errorMessage != null && !isLoading && jobs.isEmpty())` → ErrorState |
| `errorMessage` | HistoryTab pagination error | `if (!isLoadingMore && hasMore && errorMessage != null)` → Error item |
| `newJobs` / `filteredJobs` | NewJobsTab content | `uiState.filteredJobs.ifEmpty { uiState.newJobs }` |
| `completedJobs` | HistoryTab content | Direct list display |
| `hasOngoingJob` | NewJobsTab banner + button state | `if (hasOngoingJob)` → OngoingJobBanner, disables accept buttons |
| `acceptingJobId` | NewJobCard button state | `isAccepting = acceptingJobId == job.bookingId` |
| `selectedServiceFilter`, `maxDistanceFilter`, etc. | Filter indicator | Conditional "Filters applied" card |
| `selectedServiceFilter`, etc. | FloatingActionButton | Conditional "Clear Filters" FAB |
| `searchQuery` | Filter indicator | Included in filter check |
| `hasMoreHistory` | HistoryTab pagination | Controls load more trigger |

### Local UI State (Non-ViewModel)

**Location:** `JobsScreen.kt` lines 132-136

```kotlin
var selectedTabIndex by remember { mutableIntStateOf(0) }      // Tab selection
var showAcceptDialog by remember { mutableStateOf<Job?>(null) } // Dialog visibility
var showRejectDialog by remember { mutableStateOf<Job?>(null) } // Dialog visibility (unused)
var showSearch by remember { mutableStateOf(false) }            // Search dialog (unused)
var showFilters by remember { mutableStateOf(false) }           // Filter dialog (unused)
```

**Observation:** Tab state is managed locally but synced with HorizontalPager state. Dialog states are local. Search and filter dialogs are declared but not implemented.

---

## 6️⃣ UI RENDERING LOGIC

### Conditional Rendering Strategy

#### NewJobsTab Rendering Priority

**Location:** `NewJobsTab` composable, lines 408-504

1. **Error State** (Highest Priority)
   - Condition: `errorMessage != null && !isLoading && jobs.isEmpty()`
   - Renders: `ErrorState` with `Modifier.fillMaxSize()`
   - Early return: None (uses `when` expression)

2. **Loading State** (Second Priority)
   - Condition: `isLoading == true`
   - Renders: `Box` with `CircularProgressIndicator` centered
   - Modifier: `Modifier.fillMaxSize()`

3. **Empty State** (Third Priority)
   - Condition: `jobs.isEmpty()`
   - Renders: `EmptyState` with `Modifier.fillMaxSize()`

4. **Normal Content** (Default)
   - Condition: All above conditions false
   - Renders: `Column` containing:
     - Conditional `OngoingJobBanner` (if `hasOngoingJob`)
     - `PullToRefreshBox` containing:
       - `LazyColumn` with:
         - Conditional filter indicator item
         - Job card items

#### HistoryTab Rendering Priority

**Location:** `HistoryTab` composable, lines 532-607

1. **Loading State** (Highest Priority)
   - Condition: `isLoading && jobs.isEmpty()`
   - Renders: `Box` with `CircularProgressIndicator` centered
   - Modifier: `Modifier.fillMaxSize()`

2. **Empty State** (Second Priority)
   - Condition: `jobs.isEmpty()`
   - Renders: `EmptyState` with `Modifier.fillMaxSize()`

3. **Normal Content** (Default)
   - Condition: All above conditions false
   - Renders: `LazyColumn` with:
     - Job card items
     - Conditional loading indicator (if `isLoadingMore`)
     - Conditional error retry (if pagination error)

### UI Decision Source

**ViewModel-Driven:**
- Loading states (both tabs)
- Error states (both tabs)
- Empty states (both tabs)
- Job list content
- Filter/sort state
- Button enabled states
- Pagination state

**UI-Driven:**
- Tab selection (local state, synced with pager)
- Dialog visibility (`showAcceptDialog`, `showRejectDialog`)
- Search/Filter dialog visibility (declared but unused)
- Pager state synchronization

### Section Rendering Order (When All Visible)

**NewJobsTab:**
1. OngoingJobBanner (if `hasOngoingJob`)
2. Filter indicator card (if filters active)
3. NewJobCard items (in LazyColumn)

**HistoryTab:**
1. CompletedJobCard items (in LazyColumn)
2. Loading indicator (if `isLoadingMore`)
3. Error retry (if pagination error)

---

## 7️⃣ USER INTERACTIONS & CALLBACK FLOW

### User Actions Documented

#### 1. Tab Selection
- **Action:** Click tab or swipe HorizontalPager
- **Handler:** Local state (`selectedTabIndex`)
- **Side Effects:**
  - Syncs with pager state (bidirectional)
  - Triggers history load if tab 1 selected and empty

#### 2. Job Card Click
- **Action:** Click on NewJobCard or CompletedJobCard
- **Handler:** `onJobClick` callback
- **Flow:** UI → `onNavigateToJobDetails` → Navigation

#### 3. Accept Job
- **Action:** Click "Accept" button on NewJobCard
- **Handler:** `onAcceptClick` callback
- **Flow:**
  - UI → Sets `showAcceptDialog = job`
  - Dialog → `viewModel.acceptJob()`
  - ViewModel → Repository → Firestore
  - Success → Navigation to job details
  - Error → Snackbar

#### 4. Reject Job
- **Action:** Click "Reject" button on NewJobCard
- **Handler:** `onRejectClick` callback
- **Flow:**
  - UI → `viewModel.rejectJob(job)`
  - ViewModel → Updates local state (no Firestore write)
  - Job removed from list immediately

#### 5. Pull-to-Refresh
- **Action:** Pull down on NewJobsTab
- **Handler:** `PullToRefreshBox.onRefresh`
- **Flow:** UI → `viewModel.loadNewJobs()`

#### 6. Sort Toggle
- **Action:** Click sort button (⇅) in header
- **Handler:** Direct ViewModel call
- **Flow:** UI → `viewModel.setSortBy(nextSort)` → `applyFiltersAndSort()`

#### 7. Clear Filters
- **Action:** Click "Clear Filters" FAB or filter indicator "Clear"
- **Handler:** Direct ViewModel call
- **Flow:** UI → `viewModel.clearFilters()` → `applyFiltersAndSort()`

#### 8. Load More (History)
- **Action:** Auto-triggered when scrolled to last item
- **Handler:** `LaunchedEffect(listState)` detects scroll position
- **Flow:** UI → `viewModel.loadMoreHistory()`

#### 9. Search (Incomplete)
- **Action:** Click search button (🔍)
- **Handler:** Sets `showSearch = true` (no dialog implemented)

#### 10. Filters (Incomplete)
- **Action:** Click filter button (⚙️)
- **Handler:** Sets `showFilters = true` (no dialog implemented)

### Callback Architecture

**Parent → Child:**
- Callbacks passed as parameters (`onJobClick`, `onAcceptClick`, `onRejectClick`)
- No direct ViewModel access in child composables (NewJobCard, CompletedJobCard)

**Child → ViewModel:**
- Direct ViewModel calls from parent composables (JobsScreen, NewJobsTab)
- Example: `viewModel.clearFilters()`, `viewModel.setSortBy()`

**Observation:** Mixed pattern - some actions use callbacks, others call ViewModel directly from parent composables.

---

## 8️⃣ DATA → UI PIPELINE

### Complete Data Flow

```
Firestore Database
  ↓
JobsRepository
  ├── listenToNewJobs(providerId): Flow<List<Job>>
  ├── listenToNewJobsFromInbox(providerId): Flow<List<JobInboxEntry>> (disabled)
  ├── getCompletedJobs(providerId, limit, lastDocument): Result<Pair<List<Job>, DocumentSnapshot?>>
  ├── acceptJob(bookingId, providerId): Result<Unit>
  └── hasOngoingJob(providerId): Boolean
  ↓
JobsViewModel
  ├── loadNewJobs()
  │   ├── listenToNewJobs() or listenToNewJobsFromInbox()
  │   ├── filterRejectedJobs()
  │   └── update _uiState.newJobs
  │   └── applyFiltersAndSort() → _uiState.filteredJobs
  ├── loadHistory()
  │   ├── getCompletedJobs()
  │   └── update _uiState.completedJobs
  ├── loadMoreHistory()
  │   ├── getCompletedJobs() with lastDocument
  │   └── append to _uiState.completedJobs
  ├── acceptJob()
  │   ├── jobsRepository.acceptJob()
  │   ├── remove from newJobs (optimistic)
  │   └── refresh new jobs
  ├── rejectJob()
  │   └── update local state (no repository call)
  └── applyFiltersAndSort()
      ├── Filter by service, distance, price, search
      ├── Sort by distance/price/time
      └── update _uiState.filteredJobs
  ↓
JobsScreen (UI)
  ├── collectAsState()
  └── Pass to tabs
  ↓
NewJobsTab / HistoryTab
  ├── Receive jobs list
  └── Render LazyColumn items
  ↓
NewJobCard / CompletedJobCard
  └── Display job data
```

### Transformation Points

**Repository → ViewModel:**
- Raw Firestore documents → `Job` objects
- Multiple bookings per document → Flattened job list
- Filtering: Available jobs only (`isAvailable(providerId)`)
- Location: `JobsRepository.extractJobsFromDocument()`

**ViewModel → Filtered State:**
- `newJobs` → `filteredJobs` via `applyFiltersAndSort()`
- Filtering: Service, distance, price, search query
- Sorting: Distance, price, or time
- Location: `JobsViewModel.applyFiltersAndSort()`

**ViewModel → UI Display:**
- `filteredJobs.ifEmpty { newJobs }` - fallback to raw jobs if no filters
- `completedJobs` - direct display (no filtering)
- Location: `NewJobsTab` receives jobs parameter

### Data Loading Triggers

**Initial Load:**
- ViewModel `init` block calls `loadNewJobs()` and `checkOngoingJob()`
- History loads lazily when tab 1 is selected (first time)

**Refresh:**
- User-initiated: Pull-to-refresh in NewJobsTab
- After job acceptance: `loadNewJobs()` called automatically

**Real-time Updates:**
- New Jobs: Firestore snapshot listener emits continuously
- History: One-time fetch, pagination on scroll

**Pagination:**
- Auto-triggered: `LaunchedEffect(listState)` detects scroll to last item
- Manual retry: Button in error state

---

## 9️⃣ NAVIGATION & HOST CONTEXT

### Navigation Host

**Parent:** `MainAppScreen`  
**Navigation System:** Jetpack Navigation Compose  
**NavController:** `rememberNavController()` in MainAppScreen

### JobsScreen Hosting

**Route:** `BottomNavItem.Jobs.route` ("jobs")  
**Location:** `MainAppScreen.kt` lines 104-116  
**ViewModel Creation:** Default factory (not scoped to navigation entry)

### Scaffold Hierarchy

```
MainAppScreen Scaffold
├── bottomBar: BottomNavigationBar
└── content: NavHost
    └── Jobs route composable
        └── JobsScreen Scaffold
            ├── topBar: Custom header + TabRow
            ├── snackbarHost: SnackbarHost
            ├── floatingActionButton: ExtendedFloatingActionButton (conditional)
            └── content: HorizontalPager
                ├── NewJobsTab (LazyColumn)
                └── HistoryTab (LazyColumn)
```

**Observation:** Nested Scaffolds - MainAppScreen provides bottom navigation, JobsScreen provides top app bar. JobsScreen does NOT receive parent padding values.

### BottomNavigation Interaction

**BottomNavigationBar:**
- Defined in `MainAppScreen` Scaffold
- Always visible (not conditional)
- Provides padding via Scaffold's `paddingValues`

**JobsScreen Response:**
- Does NOT receive `parentPaddingValues` from MainAppScreen
- Only uses its own Scaffold padding (for top bar)
- HorizontalPager uses `padding(paddingValues)` from JobsScreen Scaffold
- LazyColumns use fixed `contentPadding = PaddingValues(16.dp)`
- **Observation:** Bottom navigation padding is NOT applied to content

### Navigation Callbacks

**From JobsScreen:**
- `onNavigateToJobDetails: (String, String, Int?) -> Unit` → Navigate to job details
- `onJobAccepted: (String) -> Unit` → Post-acceptance navigation (currently no-op in MainAppScreen)

**Navigation Routes:**
- Job Details: `"jobDetails/{bookingId}/{customerPhoneNumber}/{bookingIndex}"`

---

## 🔟 CODE STYLE & COMPOSITION PATTERNS

### Composable Sizes

**Large Composables (>500 lines):**
- `JobsScreen.kt`: 1,301 lines (entire file, multiple composables)

**Medium Composables (100-500 lines):**
- `NewJobCard`: ~350 lines
- `CompletedJobCard`: ~160 lines
- `NewJobsTab`: ~100 lines
- `HistoryTab`: ~90 lines

**Small Composables (<100 lines):**
- `AcceptJobDialog`: ~30 lines
- `OngoingJobBanner`: ~20 lines
- `StatusBadge`: ~65 lines

### Responsibility Distribution

**JobsScreen (Main):**
- State collection
- Scaffold setup
- Tab/pager management
- Dialog management
- Header with action buttons
- Delegates content to tabs

**NewJobsTab:**
- Conditional rendering logic (error/loading/empty/content)
- Pull-to-refresh setup
- LazyColumn orchestration
- Filter indicator display

**HistoryTab:**
- Conditional rendering logic (loading/empty/content)
- Pagination detection
- LazyColumn orchestration
- Error handling for pagination

**Item Composables:**
- Pure presentation
- No state
- No side effects
- Receive callbacks for interactions

### Reusability Patterns

**Reusable Components:**
- `EmptyState`, `ErrorState` - Shared with HomeScreen
- `NewJobCard`, `CompletedJobCard` - Job-specific cards
- `StatusBadge` - Status display
- `OngoingJobBanner` - Warning banner

**Helper Functions:**
- `formatRelativeTime(timestamp)` - Time formatting
- `formatDate(timestamp)` - Date formatting (legacy)

### Naming Consistency

**Composable Naming:**
- Screen-level: `JobsScreen`
- Tab-level: `NewJobsTab`, `HistoryTab`
- Component-level: `NewJobCard`, `CompletedJobCard`
- Dialog-level: `AcceptJobDialog`

**File Naming:**
- Screen: `JobsScreen.kt`
- ViewModel: `JobsViewModel.kt`
- State: `JobsUiState` (data class in ViewModel file)

**Variable Naming:**
- State: `uiState`, `newJobs`, `filteredJobs`, `completedJobs`
- Callbacks: `on*` prefix (`onJobClick`, `onAcceptClick`)
- Flags: `has*`, `is*` prefix (`hasOngoingJob`, `isLoading`)

### Composition Patterns Observed

**State Hoisting:**
- ViewModel state collected at top level
- Passed down as parameters to tabs
- No state lifting from child composables

**Callback Pattern:**
- Job card interactions use callback functions
- Callbacks passed from parent to child
- Some direct ViewModel calls from parent (filters, sort)

**Conditional Composition:**
- `when` expressions for state-based rendering
- Priority-based rendering (error > loading > empty > content)
- Conditional items in LazyColumn (filter indicator, pagination UI)

**Tab Pattern:**
- HorizontalPager for swipe navigation
- TabRow for tab selection
- Bidirectional sync between pager and tabs

---

## 🔚 SUMMARY

### Architecture Characteristics

**State Management:**
- Single source of truth: `JobsViewModel` with `StateFlow<JobsUiState>`
- State collected at top level, passed down as parameters
- Local UI state for tabs, dialogs, search/filter (incomplete)

**UI Composition:**
- HorizontalPager for tab navigation
- Two separate LazyColumns (one per tab)
- Conditional rendering with `when` expressions
- Pull-to-refresh in NewJobsTab only

**Data Flow:**
- Repository → ViewModel → UI (unidirectional)
- Real-time updates via Firestore snapshot listeners (New Jobs)
- Pagination for history (one-time fetches)
- Filtering and sorting in ViewModel layer

**Navigation:**
- Nested Scaffolds (parent for bottom nav, child for top bar)
- ViewModel NOT scoped to navigation entry (default factory)
- Navigation callbacks passed from parent

**Code Organization:**
- All composables in single file (1,301 lines)
- Clear separation: UI / State / Data layers
- Reusable components for common patterns
- Helper functions for formatting

### Key Observations

1. **Two LazyColumns:** One per tab, separate scroll states
2. **State-Driven UI:** Most UI decisions driven by ViewModel state
3. **Tab Navigation:** HorizontalPager + TabRow with bidirectional sync
4. **Nested Scaffolds:** Two-level Scaffold hierarchy
5. **Broad Recomposition:** Entire JobsScreen recomposes on any state change
6. **Real-time Updates:** Firestore listeners for new jobs
7. **Pagination:** Auto-load more on scroll for history
8. **Filtering/Sorting:** ViewModel-driven, applied to `filteredJobs`
9. **Incomplete Features:** Search and filter dialogs declared but not implemented
10. **Padding Issue:** Does NOT receive parent padding from MainAppScreen (unlike HomeScreen)

### Differences from HomeScreen Architecture

| Aspect | HomeScreen | JobsScreen |
|--------|------------|------------|
| Scroll Containers | Single LazyColumn | Two LazyColumns (one per tab) |
| Tab Navigation | None | HorizontalPager + TabRow |
| Parent Padding | Receives from MainAppScreen | Does NOT receive |
| State Scoping | Scoped to nav entry | Default factory |
| Loading State | Skeleton items in LazyColumn | Full-screen CircularProgressIndicator |
| Error State | Item in LazyColumn | Full-screen ErrorState |
| Empty State | Item in LazyColumn | Full-screen EmptyState |
| Pull-to-Refresh | None | Present in NewJobsTab |
| Filtering | None | ViewModel-driven filtering |
| Pagination | None | Present in HistoryTab |

---

**Report End**

*This is a baseline architectural audit. No recommendations or refactoring suggestions are provided.*

