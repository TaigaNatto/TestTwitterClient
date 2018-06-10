package natto.com.interntest

import android.annotation.SuppressLint
import android.content.Context
import android.database.DataSetObserver
import android.graphics.drawable.ColorDrawable
import android.widget.TextView
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.twitter.sdk.android.core.Callback
import com.twitter.sdk.android.core.Result
import com.twitter.sdk.android.core.TwitterException
import com.twitter.sdk.android.core.internal.UserUtils
import com.twitter.sdk.android.core.models.Tweet
import com.twitter.sdk.android.tweetui.SearchTimeline
import com.twitter.sdk.android.tweetui.Timeline
import com.twitter.sdk.android.tweetui.TimelineResult
import com.twitter.sdk.android.tweetui.TweetUtils


class RecyclerAdapter(context: Context,timeline:SearchTimeline,cnt:Int) : RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {

    private var mInflater: LayoutInflater? = null
    private var mContext: Context? = null

    //データはこれで管理
    var timelineDel:CustomTimeLineDelegate<Tweet>?=null
    //読み込んだデータ分のカウント
    var previousCount=0

    init {
        mInflater = LayoutInflater.from(context)
        mContext = context
        previousCount=cnt

        //最初に読み込む
        timelineDel = CustomTimeLineDelegate<Tweet>(timeline)
        timelineDel?.refresh(object : Callback<TimelineResult<Tweet>>() {
            override fun success(result: Result<TimelineResult<Tweet>>) {
                notifyDataSetChanged()
                previousCount = this@RecyclerAdapter.timelineDel!!.getCount()
            }

            override fun failure(exception: TwitterException) {

            }
        })

        //データの追加読込
        val dataSetObserver = object : DataSetObserver() {
            override fun onChanged() {
                super.onChanged()
                if (previousCount == 0) {
                    notifyDataSetChanged()
                } else {
                    notifyItemRangeInserted(previousCount,
                            this@RecyclerAdapter.timelineDel!!.getCount() - previousCount)
                }
                previousCount = this@RecyclerAdapter.timelineDel!!.getCount()
            }

            override fun onInvalidated() {
                notifyDataSetChanged()
                super.onInvalidated()
            }
        }

        timelineDel?.registerDataSetObserver(dataSetObserver)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): RecyclerAdapter.ViewHolder {
        // 表示するレイアウトを設定
        return ViewHolder(mInflater!!.inflate(R.layout.item_tweet, viewGroup, false))
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        // データ表示
        timelineDel?.let {
            if (it.getCount() > i) {
                viewHolder.userNameText?.text = it.getItem(i).user.name
                viewHolder.userIdText?.text = "@${it.getItem(i).user.screenName}"

                var url: String?=null
                it.getItem(i).user?.let {
                    url = UserUtils.getProfileImageUrlHttps(it,
                            UserUtils.AvatarSize.REASONABLY_SMALL)
                }
                Glide.with(mContext).load(url).into(viewHolder.userIconImage)

                viewHolder.contentText?.text = it.getItem(i).text
            }
        }
    }

    override fun getItemCount(): Int {
        return if (timelineDel != null) {
            timelineDel!!.getCount()
        } else {
            0
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var userNameText: TextView? = null
        var userIdText: TextView? = null
        var userIconImage: ImageView? = null
        var contentText: TextView? = null

        init {
            userNameText = itemView.findViewById(R.id.tweet_user_name)
            userIdText = itemView.findViewById(R.id.tweet_user_id)
            userIconImage = itemView.findViewById(R.id.tweet_user_icon)
            contentText = itemView.findViewById(R.id.tweet_content)
        }
    }

}