package natto.com.interntest

import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.View
import com.twitter.sdk.android.tweetui.*

class MainActivity : AppCompatActivity() {

    var mRecyclerView: RecyclerView? = null
    var adapter:RecyclerAdapter?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<View>(R.id.tool_bar) as Toolbar
        toolbar.title= getString(R.string.app_name)
        toolbar.setTitleTextColor(Color.parseColor("#ffffff"))
        setSupportActionBar(toolbar)

        mRecyclerView = findViewById<RecyclerView>(R.id.timeline)
        //縦向きレイアウトの指定
        mRecyclerView?.layoutManager = LinearLayoutManager(this)

        //timelineの取得
        val timeline = SearchTimeline.Builder()
                .query("IQON")
                .maxItemsPerRequest(1000)
                .languageCode("ja")
                .resultType(SearchTimeline.ResultType.RECENT)
                .build()

        adapter = RecyclerAdapter(this@MainActivity,timeline,0)
        mRecyclerView?.adapter = adapter
    }
}
