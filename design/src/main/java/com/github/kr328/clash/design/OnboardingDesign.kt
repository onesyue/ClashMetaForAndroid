package com.github.kr328.clash.design

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.github.kr328.clash.design.databinding.DesignOnboardingBinding
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.root

class OnboardingDesign(context: Context) : Design<OnboardingDesign.Request>(context) {

    enum class Request { Skip, Next, Start }

    data class Page(val iconRes: Int, val title: String, val description: String)

    private val binding = DesignOnboardingBinding
        .inflate(context.layoutInflater, context.root, false)

    override val root: View
        get() = binding.root

    private val pages = listOf(
        Page(
            R.drawable.ic_baseline_flash_on,
            context.getString(R.string.onboarding_title_1),
            context.getString(R.string.onboarding_desc_1)
        ),
        Page(
            R.drawable.ic_baseline_cloud_download,
            context.getString(R.string.onboarding_title_2),
            context.getString(R.string.onboarding_desc_2)
        ),
        Page(
            R.drawable.ic_baseline_vpn_lock,
            context.getString(R.string.onboarding_title_3),
            context.getString(R.string.onboarding_desc_3)
        )
    )

    private val dp = context.resources.displayMetrics.density
    private val indicators = mutableListOf<View>()
    private val pillActiveWidth = (24 * dp).toInt()
    private val pillInactiveWidth = (8 * dp).toInt()
    private val pillHeight = (8 * dp).toInt()
    private val pillRadius = (4 * dp)
    private val pillMargin = (4 * dp).toInt()

    private val activeColor = 0xFFFFFFFF.toInt()
    private val inactiveColor = 0x4DFFFFFF.toInt()

    init {
        binding.onboardingPager.adapter = OnboardingAdapter(pages)

        // Create pill indicators
        pages.forEachIndexed { i, _ ->
            val pill = View(context).apply {
                val w = if (i == 0) pillActiveWidth else pillInactiveWidth
                layoutParams = LinearLayout.LayoutParams(w, pillHeight).apply {
                    setMargins(pillMargin, 0, pillMargin, 0)
                }
                background = GradientDrawable().apply {
                    cornerRadius = pillRadius
                    setColor(if (i == 0) activeColor else inactiveColor)
                }
            }
            indicators.add(pill)
            binding.indicatorContainer.addView(pill)
        }

        binding.onboardingPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                animateIndicators(position)
                if (position == pages.size - 1) {
                    binding.nextBtn.text = context.getString(R.string.onboarding_start)
                    binding.skipBtn.visibility = View.INVISIBLE
                } else {
                    binding.nextBtn.text = context.getString(R.string.onboarding_next)
                    binding.skipBtn.visibility = View.VISIBLE
                }
            }
        })

        binding.skipBtn.setOnClickListener { requests.trySend(Request.Skip) }
        binding.nextBtn.setOnClickListener {
            val current = binding.onboardingPager.currentItem
            if (current < pages.size - 1) {
                binding.onboardingPager.currentItem = current + 1
            } else {
                requests.trySend(Request.Start)
            }
        }
    }

    private fun animateIndicators(activeIndex: Int) {
        indicators.forEachIndexed { i, pill ->
            val targetWidth = if (i == activeIndex) pillActiveWidth else pillInactiveWidth
            val targetColor = if (i == activeIndex) activeColor else inactiveColor
            val params = pill.layoutParams
            val startWidth = params.width

            if (startWidth != targetWidth) {
                ValueAnimator.ofInt(startWidth, targetWidth).apply {
                    duration = 250
                    addUpdateListener { anim ->
                        params.width = anim.animatedValue as Int
                        pill.layoutParams = params
                    }
                    start()
                }
            }
            (pill.background as? GradientDrawable)?.setColor(targetColor)
        }
    }

    private class OnboardingAdapter(private val pages: List<Page>) :
        RecyclerView.Adapter<OnboardingAdapter.VH>() {

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.page_icon)
            val title: TextView = view.findViewById(R.id.page_title)
            val desc: TextView = view.findViewById(R.id.page_desc)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_onboarding_page, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val page = pages[position]
            holder.icon.setImageResource(page.iconRes)
            holder.title.text = page.title
            holder.desc.text = page.description
        }

        override fun getItemCount() = pages.size
    }
}
