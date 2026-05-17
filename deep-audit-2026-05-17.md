# Deep Audit Snapshot - CreateWalkableTracks - 2026-05-17

Scope: all
Date: 2026-05-17
Git SHA: 03064b0
Verdict: 🟢 GREEN

Snapshot from one audit pass. Findings reflect the code as of the SHA above; line numbers and severities will drift as fixes land. Do not treat this as a living spec - re-run `/deep-audit` for a current view.

## CRITICAL

None.

## HIGH

None.

## MEDIUM

- [ ] **[TrackBlockEntityMixin.java:97-103]** Crossing/parallel-track removal deletes shared lateral cells, leaving silent holes in the walkable surface of any nearby curve.
- [ ] **[TrackBlockEntityMixin.java:75-87]** Lateral cells use centerline `result.y` instead of the rail's actual Y at the lateral offset; banked curves get slightly wrong pad heights laterally.
- [ ] **[TrackBlockEntityMixin.java:114-117]** `cwt$resolveFakeTrackBlock()` never retries after a failed lookup; theoretical only because Create is a mandatory dep.
- [ ] **[FakeTrackBlockEntityMixin.java:25-29]** `cwt$padSet` cannot be cleared once set; stale pad data persists until `keepAlive` random-ticks remove the block.

## Recommended next actions

1. Fix crossing-tracks deletion: skip `level.removeBlock` for cells claimed by other nearby `TrackBlockEntity` connections, or just drop pad data and let `keepAlive` GC unowned blocks.
2. Replace horizontal-only perpendicular with the proper rail cross-section vector (`faceNormal.cross(derivative).normalize()`, same as `BezierConnection.Bezierator`) so banked-curve lateral cells get correct Y.
3. Add a `cwt$clearPad()` path so pads can be reset when the owning curve no longer covers the cell.
4. Drop the redundant `blockEntity instanceof FakeTrackBlockEntity` check in `TrackVisualizer.renderChunkBlockEntities`.
5. Move `RENDER_RANGE_BLOCKS` / `BEZIER_SAMPLE_COUNT` into a config if you expect users to tweak visualizer behavior.
