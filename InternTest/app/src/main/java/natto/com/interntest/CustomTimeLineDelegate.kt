package natto.com.interntest

import android.database.DataSetObservable
import android.database.DataSetObserver
import com.twitter.sdk.android.core.Callback
import com.twitter.sdk.android.core.Result
import com.twitter.sdk.android.core.TwitterException
import com.twitter.sdk.android.core.models.Identifiable
import com.twitter.sdk.android.tweetui.Timeline
import com.twitter.sdk.android.tweetui.TimelineResult
import java.util.ArrayList

class CustomTimeLineDelegate<T : Identifiable> {
    // once capacity is exceeded, additional items will not be loaded
    val CAPACITY = 1000L
    // timeline that next and previous items are loaded from
    var timeline: Timeline<T>? = null
    // Observable for Adapter DataSetObservers (for ListViews)
    var listAdapterObservable: DataSetObservable?=null
    var timelineStateHolder: CustomTimelineStateHolder? = null
    var itemList: MutableList<T>?=null

    /**
     * Constructs a TimelineDelegate with a timeline for requesting data.
     * @param timeline Timeline source
     * @throws java.lang.IllegalArgumentException if timeline is null
     */
    constructor(timeline: Timeline<T>):this(timeline,null,null)

    constructor(timeline: Timeline<T>?, observable: DataSetObservable?, items: MutableList<T>?){
        if (timeline == null) {
            throw IllegalArgumentException("Timeline must not be null")
        }
        this.timeline = timeline
        this.timelineStateHolder = CustomTimelineStateHolder()
        if (observable == null) {
            listAdapterObservable = DataSetObservable()
        } else {
            listAdapterObservable = observable
        }

        if (items == null) {
            itemList = ArrayList()
        } else {
            itemList = items
        }
    }

    /**
     * Triggers loading the latest items and calls through to the developer callback. If items are
     * received, they replace existing items.
     */
    fun refresh(developerCb: Callback<TimelineResult<T>>) {
        // reset scrollStateHolder cursors to be null, loadNext will get latest items
        timelineStateHolder?.resetCursors()
        // load latest timeline items and replace existing items
        loadNext(timelineStateHolder?.positionForNext(),
                RefreshCallback(developerCb, timelineStateHolder!!))
    }

    /**
     * Triggers loading next items and calls through to the developer callback.
     */
    fun next(developerCb: Callback<TimelineResult<T>>) {
        loadNext(timelineStateHolder?.positionForNext(),
                NextCallback(developerCb, timelineStateHolder!!))
    }

    /**
     * Triggers loading previous items.
     */
    fun previous() {
        loadPrevious(timelineStateHolder?.positionForPrevious(),
                PreviousCallback(timelineStateHolder!!))
    }

    /**
     * Returns the number of items in the data set.
     * @return Count of items.
     */
    fun getCount(): Int {
        return itemList!!.size
    }

    fun getTimeLine(): Timeline<*> {
        return timeline!!
    }

    /**
     * Gets the data item associated with the specified position in the data set.
     * @param position The position of the item within the adapter's data set.
     * @return The data at the specified position.
     */
    fun getItem(position: Int): T {
        if (isLastPosition(position)) {
            previous()
        }
        return itemList!![position]
    }

    /**
     * Gets the row id associated with the specified position in the list.
     * @param position The position of the item within the adapter's data set.
     * @return The id of the item at the specified position.
     */
    fun getItemId(position: Int): Long {
        val item = itemList!![position]
        return item.id
    }

    /**
     * Sets all items in the itemList with the item id to be item. If no items with the same id
     * are found, no changes are made.
     * @param item the updated item to set in the itemList
     */
    fun setItemById(item: T) {
        for (i in itemList?.indices!!) {
            if (item.id == itemList!![i].id) {
                itemList!![i]=item
            }
        }
        notifyDataSetChanged()
    }

    /**
     * Returns true if the itemList size is below the MAX_ITEMS capacity, false otherwise.
     */
    fun withinMaxCapacity(): Boolean {
        return itemList?.size!! < CAPACITY
    }

    /**
     * Returns true if the position is for the last item in itemList, false otherwise.
     */
    fun isLastPosition(position: Int): Boolean {
        return position == itemList?.size!! - 1
    }

    /**
     * Checks the capacity and sets requestInFlight before calling timeline.next.
     */
    fun loadNext(minPosition: Long?, cb: Callback<TimelineResult<T>>) {
        if (withinMaxCapacity()) {
            if (timelineStateHolder!!.startTimelineRequest()) {
                timeline?.next(minPosition, cb)
            } else {
                cb.failure(TwitterException("Request already in flight"))
            }
        } else {
            cb.failure(TwitterException("Max capacity reached"))
        }
    }

    /**
     * Checks the capacity and sets requestInFlight before calling timeline.previous.
     */
    fun loadPrevious(maxPosition: Long?, cb: Callback<TimelineResult<T>>) {
        if (withinMaxCapacity()) {
            if (timelineStateHolder!!.startTimelineRequest()) {
                timeline?.previous(maxPosition, cb)
            } else {
                cb.failure(TwitterException("Request already in flight"))
            }
        } else {
            cb.failure(TwitterException("Max capacity reached"))
        }
    }

    /**
     * TimelineDelegate.DefaultCallback is a Callback which handles setting requestInFlight to
     * false on both success and failure and calling through to a wrapped developer Callback.
     * Subclass methods must call through to the parent method after their custom implementation.
     */
    internal open inner class DefaultCallback(val developerCallback: Callback<TimelineResult<T>>?,
                                              val timelineStateHolder: CustomTimelineStateHolder) : Callback<TimelineResult<T>>() {

        override fun success(result: Result<TimelineResult<T>>) {
            timelineStateHolder.finishTimelineRequest()
            developerCallback?.success(result)
        }

        override fun failure(exception: TwitterException) {
            timelineStateHolder.finishTimelineRequest()
            developerCallback?.failure(exception)
        }
    }

    /**
     * Handles receiving next timeline items. Prepends received items to listItems, updates the
     * scrollStateHolder nextCursor, and calls notifyDataSetChanged.
     */
    internal open inner class NextCallback(developerCb: Callback<TimelineResult<T>>,
                                           timelineStateHolder: CustomTimelineStateHolder) : DefaultCallback(developerCb, timelineStateHolder) {

        override fun success(result: Result<TimelineResult<T>>) {
            if (result.data.items.size > 0) {
                val receivedItems = ArrayList(result.data.items)
                receivedItems.addAll(itemList as List<T>)
                itemList = receivedItems
                notifyDataSetChanged()
                timelineStateHolder.setNextCursor(result.data.timelineCursor)
            }
            // do nothing when zero items are received. Subsequent 'next' call does not change.
            super.success(result)
        }
    }

    /**
     * Handles receiving latest timeline items. If timeline items are received, clears listItems,
     * sets received items, updates the scrollStateHolder nextCursor, and calls
     * notifyDataSetChanged. If the results have no items, does nothing.
     */
    internal inner class RefreshCallback(developerCb: Callback<TimelineResult<T>>,
                                         timelineStateHolder: CustomTimelineStateHolder) : NextCallback(developerCb, timelineStateHolder) {

        override fun success(result: Result<TimelineResult<T>>) {
            if (result.data.items.size > 0) {
                itemList?.clear()
            }
            super.success(result)
        }
    }

    /**
     * Handles appending listItems and updating the scrollStateHolder previousCursor.
     */
    internal inner class PreviousCallback(timelineStateHolder: CustomTimelineStateHolder) : DefaultCallback(null, timelineStateHolder) {

        override fun success(result: Result<TimelineResult<T>>) {
            if (result.data.items.size > 0) {
                itemList?.addAll(result.data.items)
                notifyDataSetChanged()
                timelineStateHolder.setPreviousCursor(result.data.timelineCursor)
            }
            // do nothing when zero items are received. Subsequent 'next' call does not change.
            super.success(result)
        }
    }

    /* Support Adapter DataSetObservers, based on BaseAdapter */

    /**
     * Registers an observer that is called when changes happen to the managed data items.
     * @param observer The object that will be notified when the data set changes.
     */
    fun registerDataSetObserver(observer: DataSetObserver) {
        listAdapterObservable?.registerObserver(observer)
    }

    /**
     * Unregister an observer that has previously been registered via
     * registerDataSetObserver(DataSetObserver).
     * @param observer The object to unregister.
     */
    fun unregisterDataSetObserver(observer: DataSetObserver) {
        listAdapterObservable?.unregisterObserver(observer)
    }

    /**
     * Notifies the attached observers that the underlying data has been changed and any View
     * reflecting the data set should refresh itself.
     */
    fun notifyDataSetChanged() {
        listAdapterObservable?.notifyChanged()
    }

    /**
     * Notifies the attached observers that the underlying data is not longer valid or available.
     * Once invoked, this adapter is no longer valid and should not report further data set changes.
     */
    fun notifyDataSetInvalidated() {
        listAdapterObservable?.notifyInvalidated()
    }
}