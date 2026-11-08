# COMP5050 Assignment 3 - Testing Report

- **Report date:** 2026-11-04

## Summary

The application compiles with JDK 11. The current automated suite contains 50
JUnit 4 tests across eight test classes. The latest verification run completed
with `OK (50 tests)`. The separate compile command also compiled 17 application
and supplied Java source files.

The tests cover model validation, stream merging, simplified tracker state,
latest-event expiry, latitude and longitude filters, rolling distance windows,
altitude conversion, and Swing component behavior. The GUI tests construct and
exercise Swing components; they do not capture screenshots of a displayed JFrame.
The `run.sh` launch command was smoke-checked and the Java GUI process remained
active, but the available desktop inspection bridge could not attach to the Java
window. Visual inspection and the course demonstration video are therefore not
claimed as completed evidence in this report.

## Environment

| Component | Verified version or value |
| --- | --- |
| Operating system | macOS 26.3, build 25D125 |
| Java runtime/compiler | Microsoft OpenJDK 11.0.32.1 LTS |
| Java source target | `--release 11` |
| Sodium | 1.2.0 |
| Swidget | 1.0.0 upstream Java build, revision `03d0c0f` |
| JUnit | 4.13.2 |
| Hamcrest | 1.3 |

## Reproduction

Set `PROVIDED_DIR` to the directory holding the course starter sources, data,
and local libraries. The commands below were run with the listed local paths;
replace the absolute paths when using another clone.

```bash
./scripts/start-local.sh compile
./scripts/start-local.sh test
```

`compile.sh` and `test.sh` require JDK 11. `test.sh` compiles the application,
compiles every Java test under `test/`, and invokes JUnitCore for all classes
ending in `_Test.java`. `run.sh` compiles and launches `GpsGUI`; it changes its
working directory to `PROVIDED_DIR` because the course service opens `gps.dat`
relative to the process working directory. To run the supplied example instead,
use `MAIN_CLASS=Example ./scripts/run.sh`.

### Starter file placement

`PROVIDED_DIR` must contain the unmodified course files `GpsService.java`,
`GpsEvent.java`, `Example.java`, and `gps.dat`. The required jars are under
`PROVIDED_DIR/lib/`: Sodium, Swidget, JUnit, and Hamcrest. Keep these files in
the local `provided/` area and out of Git; the application scripts receive their
locations through environment variables.

## Automated test results

The actual test command completed successfully with JUnit 4.13.2 and reported
`OK (50 tests)`. For each case below, the input and expected behavior are stated
from the test setup and assertions. The actual column records that those
assertions passed in the full suite; it is not a claim that every case was
manually replayed in a visible window.

### `GpsModel_Test` - 11 tests

| Case | Input and setup | Expected | Actual / result |
| --- | --- | --- | --- |
| M01 `acceptsLatitudeEndpointsAndZero` | Construct points at latitude -90, 0, and 90. | Preserve all legal endpoints and zero. | Values matched; PASS. |
| M02 `rejectsLatitudeOutsideRangeAndNonFiniteValues` | Try latitude -90.000001, 90.000001, NaN, and positive infinity. | Each invalid point throws `IllegalArgumentException`. | All four were rejected; PASS. |
| M03 `acceptsLongitudeEndpointsAndZero` | Construct points at longitude -180, 0, and 180. | Preserve all legal endpoints and zero. | Values matched; PASS. |
| M04 `rejectsLongitudeOutsideRangeAndNonFiniteValues` | Try longitude -180.000001, 180.000001, NaN, and negative infinity. | Each invalid point throws `IllegalArgumentException`. | All four were rejected; PASS. |
| M05 `preservesNegativeCoordinatesAndSnapshotsSimplifiedFields` | Convert a Tracker2 event at (-12.5, -77.25), then mutate its name and coordinates. | The simplified value keeps the original tracker and coordinates. | Original values remained; PASS. |
| M06 `simplifiedModelDoesNotStoreAltitude` | Inspect declared fields on `SimplifiedGps`. | No altitude state exists in the simplified model. | No altitude field found; PASS. |
| M07 `convertsZeroAndOneFootToMetersAndRetainsArrivalTime` | Convert 0 ft and 1 ft samples; use arrival time -42 ns for the latter. | Altitude is 0 m and 0.3048 m; tracker, coordinates, and time are preserved. | Conversion and snapshot assertions passed; PASS. |
| M08 `rejectsNonFiniteAltitudeAtDistanceBoundary` | Convert samples with NaN and positive-infinite altitude. | Both invalid altitudes are rejected. | Both threw `IllegalArgumentException`; PASS. |
| M09 `formatsExactlyFourSourceOrderedCommaSeparatedFields` | Format Tracker3 at (-12.5, -77.25, 1 ft), then mutate the source. | Output remains `Tracker3,-12.5,-77.25,1.0` with four fields. | Exact format and field count matched; PASS. |
| M10 `rejectsInvalidBoundsAndIncludesBothEndpoints` | Try reversed/out-of-range bounds, then probe inclusive endpoints, outside values, and NaN. | Invalid bounds throw; legal endpoints match; invalid points do not. | All assertions passed; PASS. |
| M11 `rejectsTrackerNamesThatCannotBeRepresentedAsOneField` | Try blank and comma-containing tracker names; also construct Tracker4. | Invalid names are rejected; Tracker4 is accepted. | Assertions passed; PASS. |

### `SimplifiedStream_Test` - 5 tests

| Case | Input and setup | Expected | Actual / result |
| --- | --- | --- | --- |
| S01 `zeroEventsLeaveAllTenSlotsEmpty` | Create the ten tracker streams and send no events. | Ten named slots exist and each position Cell is empty. | All ten remained empty; PASS. |
| S02 `oneTrackerEventUpdatesOnlyItsSlot` | Send one Tracker4 event with negative, high-precision coordinates. | Only Tracker4 updates with exact values. | Tracker4 alone updated; PASS. |
| S03 `twoTrackerEventsRemainIndependentAndLaterUpdateReplacesOnlyOne` | Send Tracker1 and Tracker7, then replace Tracker1. | Tracker1 holds its latest value; Tracker7 remains unchanged. | Both slots matched; PASS. |
| S04 `tenTrackerEventsPopulateAllSlotsWithExactCoordinates` | Send one generated event for each Tracker0 through Tracker9. | All ten Cells contain their exact coordinates. | Every slot matched; PASS. |
| S05 `simplifiedPositionsPreserveSignAndPrecisionWithoutAltitudeOrFeet` | Send small negative latitude and near-boundary longitude with altitude present. | Coordinates retain sign/precision; model has only tracker, latitude, longitude. | Field and coordinate assertions passed; PASS. |

### `GpsServiceAdapter_Test` - 3 tests

| Case | Input and setup | Expected | Actual / result |
| --- | --- | --- | --- |
| A01 `mergesEveryInputStreamAndCapturesArrivalTime` | Send one event on each of three fake source streams using a clock starting at 100 ns. | All three arrive in source order with timestamps 100, 101, and 102 ns. | Three snapshots and timestamps matched; PASS. |
| A02 `snapshotsMutableSourceEventAndRetainsRawAltitude` | Send Tracker4 at (-12.5, -77.25, 600 ft), then mutate the source object. | Received snapshot retains original fields, altitude, and 77 ns timestamp. | Original snapshot values remained; PASS. |
| A03 `handlesEmptyInputWithoutCreatingAServiceInTestMode` | Construct the test adapter with an empty stream array. | No service is created and an event stream is still exposed. | Null service and non-null stream asserted; PASS. |

### `LatestEvent_Test` - 4 tests

| Case | Input and setup | Expected | Actual / result |
| --- | --- | --- | --- |
| L01 `displaysAnEventImmediatelyAndReplacesItWithTheNextEvent` | Send Tracker1 then Tracker2 into one model. | Latest text changes immediately to each exact four-field event. | Both values and two timeout requests matched; PASS. |
| L02 `secondEventRemainsVisibleUntilItsOwnThreeSecondDeadline` | Send Tracker1, advance 2 s, send Tracker2, then advance 2.9 s and 0.1 s. | Tracker2 remains until its own 3 s deadline, then clears. | Virtual timer boundary assertions passed; PASS. |
| L03 `delayedTimeoutFromOlderEventCannotClearTheNewerEvent` | Send two events and fire the older generation token before the current token. | Old token has no effect; current token clears the text. | Generation assertions passed; PASS. |
| L04 `timeoutTicksForOneDisplayDoNotClearTheOtherDisplay` | Send independent recent and filtered events, then tick each display separately. | A timeout clears only its own display. | Both independent states matched; PASS. |

### `GpsFilterModel_Test` - 3 tests

| Case | Input and setup | Expected | Actual / result |
| --- | --- | --- | --- |
| F01 `latitudeOnlyBoundsLeaveLongitudeUnrestrictedThenCombineWithLongitude` | Apply latitude 10..20, then longitude -10..10, then clear latitude; send matching and nonmatching points. | Latitude-only accepts any longitude; combined bounds require both; clearing latitude retains longitude. | Accepted tracker sequence matched; PASS. |
| F02 `longitudeOnlyBoundsLeaveLatitudeUnrestricted` | Apply longitude -30..30; send (85,25) and (0,30.5). | High latitude is accepted when longitude matches; out-of-range longitude is rejected. | Only Tracker6 accepted; PASS. |
| F03 `invalidSubmissionKeepsLastValidBoundsAndValidSubmissionClearsError` | Apply 10..20, submit nonnumeric, reversed/out-of-range, half-empty, then blank bounds. | Prior valid bounds remain on errors; errors are exposed and cleared by a valid clear action. | Bounds and error assertions passed; PASS. |

### `DistanceCalculation_Test` - 6 tests

| Case | Input and setup | Expected | Actual / result |
| --- | --- | --- | --- |
| D01 `emptyWindowHasNoDistanceAndOnePositionHasZeroMeters` | Inspect an empty window, then append one sample. | Empty is `Optional.empty`; one sample is 0 m. | Both optional values matched; PASS. |
| D02 `identicalPositionsHaveZeroDistance` | Append the same coordinates and altitude twice. | Segment and window distance are zero. | Both distances were 0; PASS. |
| D03 `horizontalDistanceUsesTheDocumentedEquirectangularProjection` | Move 1 degree longitude at the equator with zero altitude change. | Distance equals 6,371,000 m times one degree in radians; output uses ceiling. | Calculation and rounded value matched; PASS. |
| D04 `altitudeOnlyMovementConvertsFeetAndRoundsUp` | Keep coordinates fixed and move from 0 ft to 1 ft. | Vertical movement is 0.3048 m and display distance rounds to 1 m. | Conversion and rounded value matched; PASS. |
| D05 `windowAddsAdjacentSegmentsAcrossThreeOrMorePositions` | Travel from longitude 0 to 1 and back to 0 at the equator. | Total is the sum of both adjacent segments, rounded upward. | Route total matched; PASS. |
| D06 `longitudeDeltaCrossesTheDateLineByTheShortDirection` | Move from longitude 179.5 to -179.5. | Use the normalized 1-degree path across the date line. | One-degree distance matched; PASS. |

### `FilteredDistance_Test` - 8 tests

| Case | Input and setup | Expected | Actual / result |
| --- | --- | --- | --- |
| W01 `distanceIsEmptyBeforeDataAndZeroUntilTwoPositionsAreKnown` | Inspect before events; send one sample and then an identical second sample. | No data before input; one point and identical positions display 0 m. | Optional and zero results matched; PASS. |
| W02 `distanceWindowIncludesTheFiveMinuteBoundaryAndExpiresOlderSamples` | Send points at 0 and exactly 5 min; then another at 5 min plus 1 ns. | Exact cutoff is retained; the older point expires after the next nanosecond. | Boundary route then 0 m matched; PASS. |
| W03 `trackersAccumulateDistanceIndependently` | Interleave Tracker0 and Tracker1 starts and finishes. | Each tracker accumulates its own route without changing the other. | Both independent totals matched; PASS. |
| W04 `latitudeFilterExcludesOutsidePointsFromDistance` | Use latitude -1..1; send an outside point between two accepted points. | Outside point does not enter the route. | Accepted-endpoint distance matched; PASS. |
| W05 `longitudeFilterExcludesOutsidePointsFromDistance` | Use longitude -1..1; send an outside longitude between accepted points. | Outside point does not enter the route. | Accepted-endpoint distance matched; PASS. |
| W06 `combinedFilterRequiresBothLatitudeAndLongitudeToMatch` | Use latitude 10..20 and longitude 30..40; send points failing each axis and one passing point. | Only points within both axes contribute. | Route through accepted points matched; PASS. |
| W07 `changingBoundsAffectsFutureEventsWithoutRefilteringHistory` | Tighten one fixture's bounds and widen another fixture's bounds after accepted history. | Existing history stays; later events follow the current bounds only. | Both route totals matched the documented policy; PASS. |
| W08 `outOfOrderEventsAreInsertedIntoTimestampOrder` | Send timestamps 0, 20 s, then 10 s with distinct coordinates. | Distance follows timestamp order 0, 10, 20 s. | Ordered route total matched; PASS. |

### `GpsGUILayout_Test` - 10 tests

| Case | Input and setup | Expected | Actual / result |
| --- | --- | --- | --- |
| G01 `laysOutEveryRequiredPanelTableAndFilterControl` | Construct dashboard with no events and locate all named components. | Required sections, ten-row tables, columns, four fields, two buttons, and two text areas exist. | Component and table assertions passed; PASS. |
| G02 `trackerTableReflectsCellUpdatesWithoutBlockingTheSendingThread` | Send a Tracker6 event with negative coordinates from the test thread. | Tracker6 row updates and Tracker5 stays blank after EDT dispatch. | Exact row values matched; PASS. |
| G03 `trackerTableShowsZeroOneTwoAndTenActiveSlotsWithoutAltitude` | Inspect empty slots, send Tracker0, then Tracker1, then Tracker2 through Tracker9. | The display progresses from 0 to 1, 2, and 10 active slots; only tracker, latitude, and longitude columns appear. | All ten names and coordinates matched; PASS. |
| G04 `distanceTableTracksFilteredDistanceCellsForTheMatchingTracker` | Filter latitude 10..20; send excluded Tracker7, then accepted 100 ft and 101 ft samples, followed by 512 updates. | Show No data, then 0, then 1 m; excluded input has no effect; Tracker6 stays No data. | Tracker7 values and other row matched; PASS. |
| G05 `latestEventDisplaysReplaceEventsAndClearAfterTheSwingTimeout` | Send two events, wait 2.2 s, send a third, then wait past the older deadline and through the latest event deadline. | New events replace old text; older timer cannot clear current text; both areas clear after 3 s idle. | Replacement, stale deadline, and clear assertions passed; PASS. |
| G06 `filterButtonsUpdateRangeLabelsAndShowRecoverableValidationErrors` | Set latitude 10..20, then submit reversed 30..20. | Valid label remains; invalid input shows a recoverable error and updates accessible descriptions. | Labels and error assertions passed; PASS. |
| G07 `filteredEventDisplayTracksOnlyEventsInsideTheCurrentBounds` | Set latitude 10..20; send inside, outside, change range to 20..30, then send a new in-range event. | Recent area follows all input; filtered area changes only on accepted input. | Both text areas matched; PASS. |
| G08 `latitudeLongitudeAndCombinedFiltersAreReflectedInTheGui` | Exercise latitude-only, clear latitude, longitude-only, and both-axis ranges with passing and failing points. | Each axis works alone; combined mode requires both; range labels reflect active bounds. | Accepted/rejected events and labels matched; PASS. |
| G09 `distanceTableDisplaysIndependentFiveMinuteWindowsForEachTracker` | Give Tracker0 and Tracker1 points at 0 and 5 min; advance Tracker0 to 5 min plus 1 ns. | Both totals appear; only Tracker0 expires to 0 m while Tracker1 retains its route. | Independent table values matched; PASS. |
| G10 `sectionLayoutRemainsDistinctAtMinimumAndDesktopSizes` | Lay out dashboard at 1200x900 and 960x680. | All five sections have nonzero size and do not overlap. | Both size assertions passed; PASS. |

## Evidence descriptions

The following descriptions are backed by named automated tests. They are not
screenshots. The separate video is expected to provide visible, in-process proof.

### Tracker display and altitude removal

At zero events, all ten fixed rows show `-` for latitude and longitude. After a
Tracker0 event, only row 0 has coordinates; after Tracker1, two rows are active;
after Tracker2 through Tracker9, all rows contain exact coordinates. The table
has only `Tracker`, `Latitude`, and `Longitude` columns. Evidence: `S01` through
`S05`, `G02`, and `G03`.

### Latitude, longitude, and combined filtering

- Latitude-only range 10..20 accepts (15, 100) and rejects (25, 35).
- With latitude cleared, longitude range 30..40 accepts (85, 35) and rejects
  (85, 50).
- With latitude 10..20 and longitude 30..40 active together, (15, 35) is
  accepted; (15, 50) fails longitude and (25, 35) fails latitude.
- The all-input event area still shows excluded events; the filtered event area
  retains its last accepted event.

Evidence: `F01` through `F03`, `W04` through `W07`, and `G06` through `G08`.

### Five-minute distance, filter, and altitude conversion

The five-minute window retains a sample exactly at the cutoff and removes one
that is older by 1 ns. Separate Tracker0 and Tracker1 totals are maintained.
Latitude, longitude, and combined-filter tests confirm excluded positions do not
enter the distance route. One foot of vertical movement is 0.3048 m and rounds
up to 1 m; the distance table shows `No data`, `0`, or the integer meters value.

Evidence: `D01` through `D06`, `W01` through `W08`, and `G04` and `G09`.

### Frequent updates and latest event

`G04` sends 512 rapid distance updates from a non-EDT test thread and verifies
that the Tracker7 table cell eventually reflects the latest distance while the
Tracker6 cell remains unchanged. `G05` exercises actual Swing timers. The model
deadline behavior is independently tested with a virtual clock in `L02` through
`L04`.

## Rubric traceability

| Guide requirement | Automated evidence | Result | Remaining visible evidence |
| --- | --- | --- | --- |
| GUI contains ten tracker rows, recent event, filters, and distance display | `G01`, `G03`, `G04` | PASS | Show the actual window and controls in the video. |
| Terminal compile and launch with JDK 11 | `compile.sh`, `test.sh`; `run.sh` launch smoke check | PASS for compile and process launch | Record terminal and visible window in the video. |
| Ten simplified tracker displays, including 0, 1, 2, and 10 active trackers | `S01` through `S05`, `G03` | PASS | Capture the staged display in the video. |
| Altitude is removed from simplified positions | `M06`, `S05`, `G03` | PASS | Point out the three displayed columns in the video. |
| Latest event replaces prior event and clears after 3 s idle | `L01` through `L04`, `G05` | PASS | Demonstrate a visible idle timeout in the video. |
| Latitude, longitude, and combined filtering | `F01` through `F03`, `G06` through `G08` | PASS | Show controls and accepted/rejected events in the video. |
| Independent 5-minute distance per tracker | `W01` through `W03`, `W08`, `G09` | PASS | Show live totals and cutoff behavior in the video. |
| Filtered distance includes only accepted events | `W04` through `W07`, `G04`, `G09` | PASS | Demonstrate one excluded point and resulting total in the video. |
| Feet-to-meters conversion and distance rounding | `M07`, `D04`, `G04` | PASS | Explain 1 ft = 0.3048 m and the displayed ceiling in the video. |
| Sodium/Swidget FRP and responsive frequent updates | `A01`, `G02`, `G04` | PASS in automated checks | Demonstrate running UI under repeated events in the video. |

## Assumptions and boundaries

- `GpsEvent` has no timestamp. The adapter snapshots mutable source fields and
  attaches a local monotonic arrival time in nanoseconds.
- Latitude and longitude bounds are inclusive. When both axes are set, an event
  must satisfy both ranges. Invalid submissions leave the last valid range active.
- A bounds change affects future events only. Retained distance history is not
  refiltered. A rejected event is ignored and does not break the segment between
  neighboring accepted samples.
- Each tracker has its own timestamp-ordered five-minute window. The lower
  cutoff is inclusive; only samples strictly older than newest time minus five
  minutes are removed. One retained point displays 0 m; an empty window has no
  distance.
- Horizontal distance uses an equirectangular projection with Earth radius
  6,371,000 m and longitude delta normalized to [-180, 180] degrees. Vertical
  distance converts altitude feet to meters using 0.3048 m/ft. Three-dimensional
  segment lengths are summed and rounded upward with `ceil`.
- The simplified display omits altitude; latest-event text preserves the four
  source fields in order: tracker, latitude, longitude, altitude.
- The course service exposes no stop API and owns repeating timers. Terminate
  the launched application process to stop playback; the provided source is not
  modified.

## Video
https://github.com/EllieW424/comp5050-ass3/blob/main/demo.mp4


