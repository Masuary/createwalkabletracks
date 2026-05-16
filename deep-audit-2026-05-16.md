# Deep Audit Snapshot - CreateWalkableTracks - 2026-05-16

Scope: "It's almost there, tracks are however 3 blocks wide, only the middle one is solid, also would be nice to have a visual debugger to showcase you if there are missing spots etc"
Date: 2026-05-16
Git SHA: not a git repo
Verdict: 🟡 YELLOW

Snapshot from one audit pass. Findings reflect the code as of the SHA above; line numbers and severities will drift as fixes land. Do not treat this as a living spec - re-run `/deep-audit` for a current view.

## HIGH

- [ ] **[TrackBlockEntityMixin.java:58-60]** Bezier sampled only at the centerline; rail's perpendicular extent ignored.
    why: `Mth.floor(below.x/z)` of a single point per `t` step. No perpendicular displacement. Diagonal/curved Create tracks have a rail-footprint AABB spanning up to 3x3 cells; only the centerline cell gets binned. The neighbor cells the rail visually covers receive no pad data.

- [ ] **[TrackBlockEntityMixin.java:82-85]** Perpendicular cells have no FakeTrackBlock, so even if sampling were fixed, `level.getBlockEntity(...)` would return null and the cell would be silently skipped.
    why: Create's `manageFakeTracksAlong` only places centerline FakeTrackBlocks. To make neighbor cells walkable the mixin must also place FakeTrackBlocks itself (mirror Create's `setBlock(..., FAKE_TRACK, 3)` + `FakeTrackBlock.keepAlive(...)`), and handle removal in the `remove == true` branch at line 28-30, which currently early-returns.

## MEDIUM

- [ ] [FakeTrackBlockMixin.java:25-35] Pad fills the full (x,z) cell footprint regardless of where the rail crosses the cell. Player can stand on empty air to the side of a diagonal rail wherever the centerline cell extends beyond the actual rail.
- [ ] [TrackBlockEntityMixin.java:48] Fixed `Math.max(segCount * 4, 32)` sample density is rigid; once perpendicular sampling is added, lateral coverage gaps become visible holes -- adaptive sampling by curve length is more robust.
- [ ] [TrackBlockEntityMixin.java:27] `@Inject(..., remap = false)` has no `require`, so a Create rename in a future version silently disables the mod instead of failing at load.

## Recommended next actions

1. Fix centerline-only sampling AND extend FakeTrackBlock placement to perpendicular cells. Both changes must land together; either alone is a no-op. Use `VecHelper.bezierDerivative` to get the tangent, build perpendicular, sample `result ± perpendicular*k` for k in {0.25, 0.5}. Place missing FakeTrackBlocks via `level.setBlock(..., FAKE_TRACK.getDefaultState(), 3)` + `FakeTrackBlock.keepAlive(...)`. Mirror removal in the `remove == true` branch.
2. Add a client-side visualizer keybind. Render pad wireframes (green = set, yellow = unset) plus the parent `BezierConnection`'s polyline. Gaps between curve and wireframes = missing spots. ~150 lines, one new client class.
3. Add `require = 1` to the @Inject so target rename in future Create versions fails loud at load.
4. Switch to adaptive sample density by `bc.getLength()` instead of segment count.
5. Decide AI pathing: leave `DAMAGE_OTHER` (mobs avoid rails) or open passable once collision exists.
