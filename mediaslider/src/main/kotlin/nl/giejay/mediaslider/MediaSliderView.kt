package nl.giejay.mediaslider

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import androidx.viewpager.widget.ViewPager
import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import com.zeuskartik.mediaslider.R
import com.zeuskartik.mediaslider.SliderItemType
import com.zeuskartik.mediaslider.SliderItemViewHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.lang.reflect.Field
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class MediaSliderView(context: Context) : ConstraintLayout(context) {
    // view elements
    private var playButton: View
    private var mainHandler: Handler
    private var mPager: ViewPager
//    private var sliderMediaNumber: TextView
//
//    private var subtitleLeft: TextView
//    private var subtitleRight: TextView
//    private var dateLeft: TextView
//    private var dateRight: TextView
//    private var titleLeft: TextView
//    private var titleRight: TextView
//    private var textViewDescriptions: List<TextView>
//
//    private var clock: TextView

    // config
    private lateinit var config: MediaSliderConfiguration

    /// internal
    private var currentPlayerInScope: ExoPlayer? = null
    private var currentPlayerView: PlayerView? = null
    private var defaultExoFactory = DefaultHttpDataSource.Factory()
    private var slideShowPlaying = false
    private val goToNextAssetRunnable = Runnable { this.goToNextAsset() }
    private var pagerAdapter: ScreenSlidePagerAdapter? = null
    private var loading = false
    private val ioScope = CoroutineScope(Job() + Dispatchers.IO)
    private val transformResults = mutableMapOf<Int, String>()
    private var currentToast: Toast? = null

    init {
        inflate(getContext(), R.layout.slider, this)
        val listViewLeft = findViewById<ListView>(R.id.metadata_view_left)
        listViewLeft.divider = null
        listViewLeft.adapter = MetaDataAdapter(context, listOf(MetaDataItem("test!", R.layout.metadata_item_left), MetaDataItem("test 2!", R.layout.metadata_item_left)))

        val listViewRight = findViewById<ListView>(R.id.metadata_view_right)
        listViewRight.divider = null
        listViewRight.adapter = MetaDataAdapter(context, listOf(MetaDataItem("test 1!", R.layout.metadata_item_right), MetaDataItem("test 3!", R.layout.metadata_item_right)))

        playButton = findViewById(R.id.playPause)
        playButton.setOnClickListener { toggleSlideshow(true) }
//        clock = findViewById(R.id.clock)
//        titleRight = findViewById(R.id.title_right)
//        titleLeft = findViewById(R.id.title_left)
//        subtitleRight = findViewById(R.id.subtitle_right)
//        subtitleLeft = findViewById(R.id.subtitle_left)
//        dateRight = findViewById(R.id.date_right)
//        dateLeft = findViewById(R.id.date_left)
//        textViewDescriptions = listOf(titleRight, titleLeft, dateRight, dateLeft, subtitleRight, subtitleLeft)

//        sliderMediaNumber = findViewById(R.id.number)
//        val left = findViewById<ImageView>(R.id.left_arrow)
//        val right = findViewById<ImageView>(R.id.right_arrow)
        mPager = findViewById(R.id.pager)
        mainHandler = Handler(Looper.getMainLooper())
    }

    @OptIn(UnstableApi::class)
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (mPager.adapter == null) {
            return super.dispatchKeyEvent(event)
        }
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (context is MediaSliderListener && (context as MediaSliderListener).onButtonPressed(event)) {
                return false
            } else if ((event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER || event.keyCode == KeyEvent.KEYCODE_ENTER || event.keyCode == KeyEvent.KEYCODE_MEDIA_PLAY || event.keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)) {
                if (currentItemType() == SliderItemType.IMAGE) {
                    toggleSlideshow(true)
                } else if (currentPlayerView != null) {
                    currentPlayerView!!.showController()
                    return super.dispatchKeyEvent(event)
                }
                return false
            } else if (slideShowPlaying) {
                if (event.keyCode != KeyEvent.KEYCODE_DPAD_RIGHT) {
                    toggleSlideshow(true)
                } else {
                    // remove all current callbacks to prevent multiple runnables
                    mainHandler.removeCallbacks(goToNextAssetRunnable)
                }
                // Go to next photo if dpad right is clicked or just stop
                return super.dispatchKeyEvent(event)
            } else if (event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (currentItemType() == SliderItemType.IMAGE || currentPlayerView?.isControllerFullyVisible == false) {
                    goToNextAsset()
                } else if(currentPlayerView?.isControllerFullyVisible == true){
                    return super.dispatchKeyEvent(event)
                }
                return false
            } else if (event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (currentItemType() == SliderItemType.IMAGE || currentPlayerView?.isControllerFullyVisible == false) {
                    goToPreviousAsset()
                    return false
                }
                return super.dispatchKeyEvent(event)
            }
        }
        return if (currentItemType() == SliderItemType.IMAGE) false else super.dispatchKeyEvent(event)
    }

    private fun goToPreviousAsset() {
        mPager.setCurrentItem((if (0 == mPager.currentItem) mPager.adapter!!.count else mPager.currentItem) - 1,
            config.enableSlideAnimation())
    }

    fun loadMediaSliderView(config: MediaSliderConfiguration) {
        this.config = config
        val listener: Player.Listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && slideShowPlaying) {
                    goToNextAsset()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val playPauseButton = findViewById<ImageButton>(R.id.exo_pause)
                playPauseButton?.setImageResource(if (isPlaying) R.drawable.exo_legacy_controls_pause else R.drawable.exo_legacy_controls_play)
            }

            override fun onPlayerError(error: PlaybackException) {
                if (slideShowPlaying) {
                    goToNextAsset()
                }
            }
        }
        initViewsAndSetAdapter(listener)
    }

    private fun setStartPosition() {
        if (config.startPosition >= 0) {
            if (config.startPosition > config.items.size) {
                mPager.currentItem = (config.items.size - 1)
                return
            }
            mPager.currentItem = config.startPosition
        } else {
            mPager.currentItem = 0
        }
        mPager.offscreenPageLimit = 1
    }

    fun toggleSlideshow(togglePlayButton: Boolean) {
        slideShowPlaying = !slideShowPlaying
        if (slideShowPlaying) {
            // do not start timers for videos, they will continue in the player listener
            if (currentItemType() == SliderItemType.IMAGE) {
                startTimerNextAsset()
            }
            if (context is Activity) {
                // view is being triggered from main app, prevent app going to sleep
                (context as Activity).window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        } else {
            clearKeepScreenOnFlags()
            mainHandler.removeCallbacks(goToNextAssetRunnable)
        }
        if (togglePlayButton) {
            togglePlayButton()
        }
    }

    private fun togglePlayButton() {
        playButton.visibility = VISIBLE
        playButton.setBackgroundResource(if (slideShowPlaying) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause)
        mainHandler.postDelayed({
            playButton.visibility = GONE
        }, 2000)
    }

    private fun startTimerNextAsset() {
        mainHandler.removeCallbacks(goToNextAssetRunnable)
        mainHandler.postDelayed(goToNextAssetRunnable, (config.interval * 1000).toLong())
    }

    private fun goToNextAsset() {
        if (mPager.currentItem < mPager.adapter!!.count - 1) {
            mPager.setCurrentItem(mPager.currentItem + 1, config.enableSlideAnimation())
        } else {
            mPager.setCurrentItem(0, config.enableSlideAnimation())
        }
    }

    private fun initViewsAndSetAdapter(listener: Player.Listener) {
//        val statusLayoutLeft = findViewById<RelativeLayout>(R.id.status_holder_left)
//        if (config.isGradiantOverlayVisible) {
//            statusLayoutLeft.setBackgroundResource(R.drawable.gradient_overlay)
//        }

        pagerAdapter = ScreenSlidePagerAdapter(
            context, config.items,
            defaultExoFactory,
            config,
            { result, position -> transformResults[position] = result },
            {
                when (it) {
                    R.id.exo_rewind -> goToPreviousAsset()
                    R.id.exo_forward -> goToNextAsset()
                }
            }
        )

        try {
            if (config.enableSlideAnimation() && config.animationSpeedMillis > 0) {
                val mScroller: Field = ViewPager::class.java.getDeclaredField("mScroller")
                mScroller.isAccessible = true
                val scroller = FixedSpeedScroller(mPager.context, DecelerateInterpolator(0.75F), config.animationSpeedMillis)
                // scroller.setFixedDuration(5000);
                mScroller.set(mPager, scroller)
            }
        } catch (e: Exception) {
            Timber.e(e, "Could not set scroller")
        }

        mPager.setAdapter(pagerAdapter)
        setStartPosition()
        if (config.isClockVisible) {
//            clock.visibility = VISIBLE
        }
        if (config.isTitleVisible) {
//            titleLeft.visibility = VISIBLE
//            titleRight.visibility = VISIBLE
        }
        if (config.isSubtitleVisible) {
//            subtitleRight.visibility = VISIBLE
//            subtitleLeft.visibility = VISIBLE
        }
        if (config.isDateVisible) {
//            dateRight.visibility = VISIBLE
//            dateLeft.visibility = VISIBLE
        }
        if (config.isMediaCountVisible) {
//            sliderMediaNumber.visibility = VISIBLE
            updateMediaCount()
        }
//        if (config.isNavigationVisible) {
//            left.visibility = VISIBLE
//            right.visibility = VISIBLE
//            left.setOnClickListener {
//                val i = mPager.currentItem
//                mPager.setCurrentItem(i - 1)
//                updateMediaCount()
//            }
//            right.setOnClickListener {
//                val i = mPager.currentItem
//                mPager.setCurrentItem(i + 1)
//                updateMediaCount()
//            }
//        }

        mPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(i: Int, v: Float, i1: Int) {
                stopPlayer()
                if (i != mPager.currentItem) {
                    return
                }
                val sliderItem = config.items[i]
                config.onAssetSelected(sliderItem)
                setItemText(sliderItem)
                updateMediaCount()
                currentToast?.cancel()
                if (!sliderItem.hasSecondaryItem() && config.debugEnabled && transformResults.contains(i)) {
                    currentToast = Toast.makeText(context, transformResults[i], Toast.LENGTH_LONG)
                    currentToast!!.show()
                    transformResults.remove(i)
                }

                if (config.loadMore != null && mPager.currentItem > config.items.size - 10 && !loading) {
                    loading = true

                    ioScope.launch {
                        val nextItems = config.loadMore!!.invoke()
                        addItemsMain(nextItems)
                        loading = nextItems.isEmpty()
                    }
                }

                if (sliderItem.type == SliderItemType.VIDEO) {
                    val viewTag = mPager.findViewWithTag<View>("view$i") ?: return
                    currentPlayerView = viewTag.findViewById(R.id.video_view)
                    if (currentPlayerView!!.player != null) {
                        currentPlayerInScope = currentPlayerView!!.player as ExoPlayer?
                        currentPlayerInScope!!.seekTo(0, 0)
                        if (currentPlayerInScope!!.playbackState == Player.STATE_IDLE) {
                            prepareMedia(sliderItem.url,
                                currentPlayerInScope!!, defaultExoFactory)
                        }
                        if(!config.isVideoSoundEnable){
                            currentPlayerView!!.player!!.volume = 0f
                        }
                        currentPlayerInScope!!.addListener(listener)
                        currentPlayerInScope!!.playWhenReady = true
                    }
                } else {
                    if (slideShowPlaying) {
                        startTimerNextAsset()
                    }
                    stopPlayer()
                }
            }

            override fun onPageSelected(i: Int) {
//                clearTextView(titleLeft, subtitleLeft, dateLeft)
            }

            override fun onPageScrollStateChanged(i: Int) {
            }
        })
    }

    fun setItemText(sliderItem: SliderItemViewHolder) {
//        titleRight.text = sliderItem.descriptionRight
//        subtitleRight.text = sliderItem.subtitleRight
//        sliderItem.dateRight?.let { dateRight.text = formatDate(it) } ?: clearTextView(this@MediaSliderView.dateRight)

        if (sliderItem.hasSecondaryItem()) {
//            titleLeft.text = sliderItem.descriptionLeft
//            subtitleLeft.text = sliderItem.subtitleLeft
//            sliderItem.dateLeft?.let { dateLeft.text = formatDate(it) } ?: clearTextView(this@MediaSliderView.dateLeft)
        }
    }

    private fun clearTextView(vararg view: TextView) {
        view.forEach { it.text = "" }
    }

    private fun updateMediaCount() {
//        sliderMediaNumber.text = "${(mPager.currentItem + 1)}/${config.items.size}"
    }

    fun onDestroy() {
        if (currentPlayerInScope != null) {
            currentPlayerInScope!!.release()
        }
        clearKeepScreenOnFlags()
        mainHandler.removeCallbacks(goToNextAssetRunnable)
    }

    private fun clearKeepScreenOnFlags() {
        if (context is Activity) {
            // view is being triggered from main app, remove the flags to keep screen on
            val window = (context as Activity).window
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun stopPlayer() {
        if (currentPlayerInScope != null && (currentPlayerInScope!!.isPlaying || currentPlayerInScope!!.isLoading)) {
            currentPlayerInScope!!.stop()
        }
    }

    fun setDefaultExoFactory(defaultExoFactory: DefaultHttpDataSource.Factory) {
        this.defaultExoFactory = defaultExoFactory
    }

    suspend fun addItemsMain(items: List<SliderItemViewHolder>) = withContext(Dispatchers.Main) {
        addItems(items)
    }

    private fun addItems(items: List<SliderItemViewHolder>) {
        setItems(Lists.newArrayList(Iterables.concat(config.items, items)).distinct())
    }

    fun setItems(items: List<SliderItemViewHolder>) {
        if (items.isEmpty()) {
            Toast.makeText(context, "No items received to show in slideshow", Toast.LENGTH_SHORT).show()
            return
        }
        if (slideShowPlaying) {
            // to prevent timing issues when adding + sliding at the same time
            mainHandler.removeCallbacks(goToNextAssetRunnable)
        }
        config.items = items
        pagerAdapter!!.setItems(items)
        if (slideShowPlaying && currentItemType() == SliderItemType.IMAGE) {
            startTimerNextAsset()
        }
    }

    private fun currentItemType(): SliderItemType? = config.items[mPager.currentItem].type

    @OptIn(UnstableApi::class)
    fun isControllerVisible(): Boolean {
        return currentPlayerView?.isControllerFullyVisible == true
    }

    companion object {
        @SuppressLint("UnsafeOptInUsageError")
        fun prepareMedia(mediaUrl: String, player: ExoPlayer, factory: DefaultHttpDataSource.Factory) {
            val mediaUri = Uri.parse(mediaUrl)
            val mediaItem = MediaItem.fromUri(mediaUri)
            val mediaSource = ProgressiveMediaSource.Factory(factory).createMediaSource(mediaItem)
            player.setMediaSource(mediaSource, 0L)
            player.prepare()
        }

        private fun formatDate(date: Date): String {
            val calendar = Calendar.getInstance()
            calendar.time = date
            val locale = Locale.getDefault(Locale.Category.FORMAT)
            val day = calendar[Calendar.DATE]
            val formatString = when (day) {
                1, 21, 31 -> "EEEE',' d'ˢᵗ' MMMM yyyy"
                2, 22 -> "EEEE',' d'ⁿᵈ' MMMM yyyy"
                3, 23 -> "EEEE',' d'ʳᵈ' MMMM yyyy"
                else -> "EEEE',' d'ᵗʰ' MMMM yyyy"
            }
            return SimpleDateFormat(formatString, locale).format(date)
        }
    }
}