package natto.com.interntest

import android.app.Application
import android.util.Log
import com.twitter.sdk.android.core.Twitter
import com.twitter.sdk.android.core.TwitterAuthConfig
import com.twitter.sdk.android.core.DefaultLogger
import com.twitter.sdk.android.core.TwitterConfig

class FirstApplication : Application() {

    private val CONSUMER_KEY = "dwfUZ78WdLBJwYickAl3KQFNi"
    private val CONSUMER_SECRET = "9kQGQtDeMARrbGAaUgy6SPbiIg792X5bSa9XFeUUKwvbK0kUBl"

    override fun onCreate() {
        super.onCreate()

        val config = TwitterConfig.Builder(this)
                .logger(DefaultLogger(Log.DEBUG))
                .twitterAuthConfig(TwitterAuthConfig(CONSUMER_KEY, CONSUMER_SECRET))
                .debug(true)
                .build()
        Twitter.initialize(config)
    }

}