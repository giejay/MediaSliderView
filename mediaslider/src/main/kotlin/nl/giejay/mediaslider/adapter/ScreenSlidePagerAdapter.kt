package nl.giejay.mediaslider.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.zeuskartik.mediaslider.R
import nl.giejay.mediaslider.model.SliderItem
import nl.giejay.mediaslider.model.SliderItemType
import nl.giejay.mediaslider.model.SliderItemViewHolder
import nl.giejay.mediaslider.view.TouchImageView
import nl.giejay.mediaslider.config.MediaSliderConfiguration
import nl.giejay.mediaslider.view.MediaSliderView.Companion.prepareMedia
import timber.log.Timber

class ScreenSlidePagerAdapter(private val context: Context,
                              private var items: List<SliderItemViewHolder>,
                              private val exoFactory: DefaultHttpDataSource.Factory,
                              private val config: MediaSliderConfiguration,
                              private val transformResult: (String, Int) -> Unit,
                              private val buttonListener: (Int) -> Unit) : PagerAdapter() {
    private var imageView: TouchImageView? = null
    private val progressBars: MutableMap<Int, ProgressBar> = HashMap()

    fun setItems(items: List<SliderItemViewHolder>) {
        this.items = items
        notifyDataSetChanged()
    }

    fun hideProgressBar(position: Int) {
        val progressBar = progressBars[position]
        if (progressBar != null) {
            progressBar.visibility = GONE
            progressBars.remove(position)
        }
    }

    override fun getItemPosition(`object`: Any): Int {
        return POSITION_NONE
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        var view: View? = null
        val model = items[position]
        if (model.type == SliderItemType.IMAGE) {
            if (model.hasSecondaryItem()) {
                view = inflater.inflate(R.layout.image_double_item, container, false)
                loadImageIntoView(view, R.id.left_image, position, model.mainItem)
                loadImageIntoView(view, R.id.right_image, position, model.secondaryItem!!)
            } else {
                view = inflater.inflate(R.layout.image_item, container, false)
                loadImageIntoView(view, R.id.mBigImage, position, model.mainItem)
            }
        } else if (model.type == SliderItemType.VIDEO) {
            view = inflater.inflate(R.layout.video_item, container, false)
            val playerView = view.findViewById<PlayerView>(R.id.video_view)
            val playBtn = playerView.findViewById<ImageButton>(R.id.exo_pause)
            playerView.tag = "view$position"
            val player = ExoPlayer.Builder(context)
                .setLoadControl(DefaultLoadControl.Builder()
                    .setPrioritizeTimeOverSizeThresholds(false)
                    .build()
                ).build()
            model.url?.let { prepareMedia(it, player, exoFactory) }
            if (!config.isVideoSoundEnable) {
                player.volume = 0f
            }

            player.playWhenReady = false
            playerView.player = player
            playBtn.setOnClickListener { v: View? ->
                buttonListener.invoke(R.id.exo_pause)
                //events on play buttons
                if (player.isPlaying) {
                    player.pause()
                } else {
                    if (player.currentPosition >= player.contentDuration) {
                        player.seekToDefaultPosition()
                    }
                    player.play()
                }
            }

            val muteButton = playerView.findViewById<ImageButton>(R.id.exo_mute)
            muteButton.setImageResource(if (player.volume > 0f) R.drawable.unmute_icon else R.drawable.mute_icon)
            muteButton.setOnClickListener { _: View? ->
                if (player.volume == 0f) {
                    player.volume = 1f
                    muteButton.setImageResource(R.drawable.unmute_icon)
                    config.isVideoSoundEnable = true
                } else {
                    player.volume = 0f
                    muteButton.setImageResource(R.drawable.mute_icon)
                    config.isVideoSoundEnable = false
                }
            }
            playerView.findViewById<ImageButton>(R.id.exo_forward).setOnClickListener { _ ->
                buttonListener.invoke(R.id.exo_forward)
            }
            playerView.findViewById<ImageButton>(R.id.exo_rewind).setOnClickListener { _ ->
                buttonListener.invoke(R.id.exo_rewind)
            }

        }
        container.addView(view)
        return view!!
    }

    private fun loadImageIntoView(imageRootLayout: View,
                                  imageViewResource: Int,
                                  position: Int,
                                  model: SliderItem) {
        imageView = imageRootLayout.findViewById(imageViewResource)
        val progressBar = imageRootLayout.findViewById<ProgressBar>(R.id.mProgressBar)
        if (progressBar != null) {
            progressBars[position] = progressBar
        }
        var glideLoader = Glide.with(context)
            .load(if (config.isOnlyUseThumbnails) model.thumbnailUrl else model.url)
            .transform(config.glideTransformation.transform(context, config) { result -> transformResult(result, position) })
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(e: GlideException?,
                                          model: Any?,
                                          target: com.bumptech.glide.request.target.Target<Drawable>,
                                          isFirstResource: Boolean): Boolean {
                    Timber.e(e, "Could not fetch image: %s", model)
                    hideProgressBar(position)
                    return false
                }

                override fun onResourceReady(resource: Drawable,
                                             model: Any,
                                             target: com.bumptech.glide.request.target.Target<Drawable>,
                                             dataSource: com.bumptech.glide.load.DataSource,
                                             isFirstResource: Boolean): Boolean {
                    hideProgressBar(position)
                    return false
                }

            })
        if (!config.isOnlyUseThumbnails) {
            glideLoader = glideLoader.thumbnail(Glide.with(context)
                .load(model.thumbnailUrl))
        }
        glideLoader.into(imageView!!)
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun isViewFromObject(view: View, o: Any): Boolean {
        return (view === o)
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        val view = `object` as View
        val exoplayer = view.findViewById<PlayerView>(R.id.video_view)
        if (exoplayer != null && exoplayer.player != null) {
            exoplayer.player!!.release()
        } else {
            val imageView = view.findViewById<View>(R.id.mBigImage)
            if (imageView != null) {
                Glide.with(context).clear(imageView)
            }
        }
        container.removeView(view)
    }
}