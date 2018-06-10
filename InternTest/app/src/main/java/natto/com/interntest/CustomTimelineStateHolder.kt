package natto.com.interntest

import com.twitter.sdk.android.tweetui.TimelineCursor
import java.util.concurrent.atomic.AtomicBoolean

class CustomTimelineStateHolder {
    // cursor for Timeline 'next' calls
    private var nextCursor: TimelineCursor? = null
    // cursor for Timeline 'previous' calls
    private var previousCursor: TimelineCursor? = null
    // true while a request is in flight, false otherwise
    val requestInFlight = AtomicBoolean(false)

    constructor(){}

    /* for testing */
    constructor(nextCursor: TimelineCursor, previousCursor: TimelineCursor):this() {
        this.nextCursor = nextCursor
        this.previousCursor = previousCursor
    }

    /**
     * Nulls the nextCursor and previousCursor
     */
    fun resetCursors() {
        nextCursor = null
        previousCursor = null
    }

    /**
     * Returns the position to use for the subsequent Timeline.next call.
     */
    fun positionForNext(): Long? {
        return if (nextCursor == null) null else nextCursor!!.maxPosition
    }

    /**
     * Returns the position to use for the subsequent Timeline.previous call.
     */
    fun positionForPrevious(): Long? {
        return if (previousCursor == null) null else previousCursor!!.minPosition
    }

    /**
     * Updates the nextCursor
     */
    fun setNextCursor(timelineCursor: TimelineCursor) {
        nextCursor = timelineCursor
        setCursorsIfNull(timelineCursor)
    }

    /**
     * Updates the previousCursor.
     */
    fun setPreviousCursor(timelineCursor: TimelineCursor) {
        previousCursor = timelineCursor
        setCursorsIfNull(timelineCursor)
    }

    /**
     * If a nextCursor or previousCursor is null, sets it to timelineCursor. Should be called by
     * setNextCursor and setPreviousCursor to handle the very first timeline load which sets
     * both cursors.
     */
    fun setCursorsIfNull(timelineCursor: TimelineCursor) {
        if (nextCursor == null) {
            nextCursor = timelineCursor
        }
        if (previousCursor == null) {
            previousCursor = timelineCursor
        }
    }

    /**
     * Returns true if a timeline request is not in flight, false otherwise. If true, a caller
     * must later call finishTimelineRequest to remove the requestInFlight lock.
     */
    fun startTimelineRequest(): Boolean {
        return requestInFlight.compareAndSet(false, true)
    }

    /**
     * Unconditionally sets requestInFlight to false.
     */
    fun finishTimelineRequest() {
        requestInFlight.set(false)
    }
}