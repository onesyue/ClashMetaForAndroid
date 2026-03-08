package com.github.kr328.clash.design

import android.content.Context
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
            android.R.drawable.ic_menu_compass,
            context.getString(R.string.onboarding_title_1),
            context.getString(R.string.onboarding_desc_1)
        ),
        Page(
            android.R.drawable.ic_menu_agenda,
            context.getString(R.string.onboarding_title_2),
            context.getString(R.string.onboarding_desc_2)
        ),
        Page(
            android.R.drawable.ic_lock_idle_lock,
            context.getString(R.string.onboarding_title_3),
            context.getString(R.string.onboarding_desc_3)
        )
    )

    private val dp = context.resources.displayMetrics.density
    private val indicators = mutableListOf<View>()

    init {
        // Setup ViewPager
        binding.onboardingPager.adapter = OnboardingAdapter(pages)

        // Create indicators
        pages.forEachIndexed { i, _ ->
            val dot = View(context).apply {
                val size = (8 * dp).toInt()
                val margin = (4 * dp).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(margin, 0, margin, 0)
                }
                setBackgroundResource(android.R.drawable.presence_invisible)
                alpha = if (i == 0) 1f else 0.3f
            }
            indicators.add(dot)
            binding.indicatorContainer.addView(dot)
        }

        // Page change callback
        binding.onboardingPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                indicators.forEachIndexed { i, dot -> dot.alpha = if (i == position) 1f else 0.3f }
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
