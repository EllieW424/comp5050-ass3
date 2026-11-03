# Distance Assumptions

- The five-minute window is inclusive at its lower boundary. A sample exactly
  `newestTimestamp - 5 minutes` is retained; only earlier samples are removed.
- Filter bounds apply when each event arrives. A bounds change affects later
  events only; retained distance history is not recalculated. Events excluded by
  the active filter do not enter the window.
- Samples are ordered by their monotonic `receivedAtNanos` timestamps. A late
  sample is inserted into timestamp order; samples older than the active cutoff
  are discarded during pruning.
- Consecutive accepted samples form the path. A filter-excluded event between
  accepted samples does not break that segment.
- Horizontal movement uses an equirectangular projection with a 6,371,000 m
  Earth radius and a longitude delta normalized to `[-180, 180]`. Vertical
  movement uses altitude converted from feet to meters. The sum is rounded up
  with `ceil`; an empty window has no distance, while one retained sample is 0 m.
