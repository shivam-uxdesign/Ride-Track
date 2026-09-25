package com.ridetrack.telemetry

import com.ridetrack.telemetry.state.RideAction
import com.ridetrack.telemetry.state.RideError
import com.ridetrack.telemetry.state.RideState
import com.ridetrack.telemetry.state.reduce
import kotlin.test.Test
import kotlin.test.assertEquals

class RideStateTest {

    private fun run(vararg actions: RideAction): RideState =
        actions.fold(RideState.Idle as RideState) { s, a -> reduce(s, a) }

    @Test
    fun `happy path`() {
        assertEquals(
            RideState.RideComplete("r1"),
            run(
                RideAction.BeginCheck, RideAction.ChecksPassed, RideAction.Start("r1"),
                RideAction.AutoPause, RideAction.AutoResume, RideAction.RequestEnd,
                RideAction.ConfirmEnd, RideAction.Saved,
            ),
        )
    }

    @Test
    fun `cannot start without passing checks`() {
        assertEquals(RideState.Idle, run(RideAction.Start("r1")))
        assertEquals(RideState.PreRideCheck, run(RideAction.BeginCheck, RideAction.Start("r1")))
    }

    @Test
    fun `cancelling end returns to the previous recording state`() {
        assertEquals(
            RideState.Paused("r1"),
            run(RideAction.BeginCheck, RideAction.ChecksPassed, RideAction.Start("r1"), RideAction.AutoPause, RideAction.RequestEnd, RideAction.CancelEnd),
        )
    }

    @Test
    fun `saving twice cannot duplicate`() {
        val saving = run(RideAction.BeginCheck, RideAction.ChecksPassed, RideAction.Start("r1"), RideAction.RequestEnd, RideAction.ConfirmEnd)
        assertEquals(RideState.Saving("r1"), saving)
        assertEquals(RideState.Saving("r1"), reduce(saving, RideAction.ConfirmEnd))
        assertEquals(RideState.RideComplete("r1"), reduce(saving, RideAction.Saved))
    }

    @Test
    fun `cannot start a second ride while recording`() {
        val recording = run(RideAction.BeginCheck, RideAction.ChecksPassed, RideAction.Start("r1"))
        assertEquals(recording, reduce(recording, RideAction.BeginCheck))
        assertEquals(recording, reduce(recording, RideAction.Start("r2")))
    }

    @Test
    fun `errors keep the ride id and can be acknowledged`() {
        val s = reduce(run(RideAction.BeginCheck, RideAction.ChecksPassed, RideAction.Start("r1")), RideAction.Fail(RideError.STORAGE_FAILURE))
        assertEquals(RideState.Error(RideError.STORAGE_FAILURE, "r1"), s)
        assertEquals(RideState.Idle, reduce(s, RideAction.Acknowledge))
    }
}
