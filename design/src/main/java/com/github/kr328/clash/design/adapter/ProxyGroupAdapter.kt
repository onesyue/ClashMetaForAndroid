package com.github.kr328.clash.design.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.model.ProxyState

class ProxyGroupAdapter(
    private val groupNames: List<String>,
    private val onSelectNode: (groupIndex: Int, nodeName: String) -> Unit,
    private val onUrlTest: (groupIndex: Int) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    internal data class GroupData(
        val index: Int,
        val name: String,
        var proxies: List<Proxy> = emptyList(),
        var selectable: Boolean = false,
        var state: ProxyState = ProxyState(""),
        var expanded: Boolean = false,
        var testing: Boolean = false,
    )

    internal sealed class ListItem {
        data class Header(val group: GroupData) : ListItem()
        data class Node(val groupIndex: Int, val proxy: Proxy, val state: ProxyState) : ListItem()
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_NODE = 1
    }

    private val groups = groupNames.mapIndexed { i, name -> GroupData(i, name) }.toMutableList()
    private val flatList = mutableListOf<ListItem>()
    private var sortByDelay: Boolean = false

    init {
        rebuildFlatList()
    }

    fun updateGroup(
        index: Int,
        proxies: List<Proxy>,
        selectable: Boolean,
        state: ProxyState,
    ) {
        if (index < groups.size) {
            val g = groups[index]
            g.proxies = proxies
            g.selectable = selectable
            g.state = state
            g.testing = false
        }
        rebuildFlatList()
    }

    fun notifySelectionChanged() {
        notifyDataSetChanged()
    }

    /** Toggle sort-by-delay mode */
    fun setSortByDelay(enabled: Boolean) {
        sortByDelay = enabled
        rebuildFlatList()
    }

    /** Auto-select the fastest node in the given group, returns the node name or null */
    fun autoSelectFastest(groupIndex: Int): String? {
        if (groupIndex >= groups.size) return null
        val group = groups[groupIndex]
        if (!group.selectable) return null
        val fastest = group.proxies
            .filter { it.delay > 0 && it.delay <= Short.MAX_VALUE }
            .minByOrNull { it.delay }
        return fastest?.name
    }

    private fun rebuildFlatList() {
        flatList.clear()
        for (group in groups) {
            flatList.add(ListItem.Header(group))
            if (group.expanded && group.proxies.isNotEmpty()) {
                var nodes = group.proxies.toList()

                // Sort by delay
                if (sortByDelay) {
                    nodes = nodes.sortedWith(compareBy {
                        when {
                            it.delay <= 0 -> Int.MAX_VALUE      // untested → bottom
                            it.delay > Short.MAX_VALUE -> Int.MAX_VALUE - 1  // timeout → near bottom
                            else -> it.delay
                        }
                    })
                }

                for (proxy in nodes) {
                    flatList.add(ListItem.Node(group.index, proxy, group.state))
                }
            }
        }
        notifyDataSetChanged()
    }

    private fun formatDelay(delay: Int): String = when {
        delay <= 0 -> "●"
        delay > Short.MAX_VALUE -> "●"
        else -> "${delay}ms"
    }

    private fun delayColor(ctx: android.content.Context, delay: Int): Int = when {
        delay <= 0 -> ContextCompat.getColor(ctx, R.color.color_status_good)
        delay > Short.MAX_VALUE -> ContextCompat.getColor(ctx, R.color.color_text_tertiary)
        delay < 200 -> ContextCompat.getColor(ctx, R.color.color_status_good)
        delay < 500 -> ContextCompat.getColor(ctx, R.color.color_status_warn)
        else -> ContextCompat.getColor(ctx, R.color.color_status_bad)
    }

    private fun setBadge(badge: TextView, delay: Int) {
        badge.text = formatDelay(delay)
        val dp = badge.context.resources.displayMetrics.density
        val color = delayColor(badge.context, delay)
        val bg = GradientDrawable().apply {
            cornerRadius = dp * 10
            setColor(color)
        }
        badge.background = bg
    }

    override fun getItemViewType(position: Int) = when (flatList[position]) {
        is ListItem.Header -> TYPE_HEADER
        is ListItem.Node -> TYPE_NODE
    }

    override fun getItemCount() = flatList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> GroupViewHolder(
                inflater.inflate(R.layout.item_proxy_group, parent, false)
            )
            else -> NodeViewHolder(
                inflater.inflate(R.layout.item_proxy_node, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = flatList[position]) {
            is ListItem.Header -> (holder as GroupViewHolder).bind(item.group)
            is ListItem.Node -> (holder as NodeViewHolder).bind(item)
        }
    }

    inner class GroupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val groupName: TextView = view.findViewById(R.id.group_name)
        private val currentNode: TextView = view.findViewById(R.id.current_node)
        private val delayBadge: TextView = view.findViewById(R.id.delay_badge)
        private val testProgress: ProgressBar = view.findViewById(R.id.test_progress)
        private val testButton: ImageView = view.findViewById(R.id.test_button)
        private val chevron: ImageView = view.findViewById(R.id.chevron)

        internal fun bind(group: GroupData) {
            groupName.text = group.name
            currentNode.text = group.state.now.ifEmpty { "—" }

            val currentProxy = group.proxies.find { it.name == group.state.now }
            setBadge(delayBadge, currentProxy?.delay ?: 0)

            when {
                group.testing -> {
                    testProgress.visibility = View.VISIBLE
                    testButton.visibility = View.GONE
                }
                group.selectable -> {
                    testProgress.visibility = View.GONE
                    testButton.visibility = View.VISIBLE
                }
                else -> {
                    testProgress.visibility = View.GONE
                    testButton.visibility = View.GONE
                }
            }

            chevron.animate()
                .rotation(if (group.expanded) 90f else 0f)
                .setDuration(150)
                .start()

            itemView.setOnClickListener {
                group.expanded = !group.expanded
                rebuildFlatList()
            }

            testButton.setOnClickListener {
                group.testing = true
                onUrlTest(group.index)
                notifyItemChanged(bindingAdapterPosition)
            }
        }
    }

    inner class NodeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val checkmark: ImageView = view.findViewById(R.id.checkmark)
        private val nodeName: TextView = view.findViewById(R.id.node_name)
        private val nodeSubtitle: TextView = view.findViewById(R.id.node_subtitle)
        private val delayBadge: TextView = view.findViewById(R.id.delay_badge)

        internal fun bind(item: ListItem.Node) {
            val proxy = item.proxy
            val isSelected = proxy.name == item.state.now

            nodeName.text = proxy.title.ifEmpty { proxy.name }

            if (proxy.subtitle.isNotEmpty()) {
                nodeSubtitle.text = proxy.subtitle
                nodeSubtitle.visibility = View.VISIBLE
            } else {
                nodeSubtitle.visibility = View.GONE
            }

            checkmark.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
            setBadge(delayBadge, proxy.delay)

            itemView.setOnClickListener {
                onSelectNode(item.groupIndex, proxy.name)
            }
        }
    }
}
