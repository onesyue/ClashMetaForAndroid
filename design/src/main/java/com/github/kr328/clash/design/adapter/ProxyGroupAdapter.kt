package com.github.kr328.clash.design.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.model.ProxyState

class ProxyGroupAdapter(
    private val groupNames: List<String>,
    private val onSelectNode: (groupIndex: Int, nodeName: String) -> Unit,
    private val onUrlTest: (groupIndex: Int) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private data class GroupData(
        val index: Int,
        val name: String,
        var proxies: List<Proxy> = emptyList(),
        var selectable: Boolean = false,
        var state: ProxyState = ProxyState(""),
        var expanded: Boolean = false,
        var testing: Boolean = false,
    )

    private sealed class ListItem {
        data class Header(val group: GroupData) : ListItem()
        data class Node(val groupIndex: Int, val proxy: Proxy, val state: ProxyState) : ListItem()
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_NODE = 1

        private val COLOR_GOOD = Color.parseColor("#FF4CAF50")
        private val COLOR_MEDIUM = Color.parseColor("#FFFFC107")
        private val COLOR_BAD = Color.parseColor("#FFF44336")
        private val COLOR_NONE = Color.parseColor("#FF9E9E9E")
    }

    private val groups = groupNames.mapIndexed { i, name -> GroupData(i, name) }.toMutableList()
    private val flatList = mutableListOf<ListItem>()

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

    private fun rebuildFlatList() {
        flatList.clear()
        for (group in groups) {
            flatList.add(ListItem.Header(group))
            if (group.expanded && group.proxies.isNotEmpty()) {
                for (proxy in group.proxies) {
                    flatList.add(ListItem.Node(group.index, proxy, group.state))
                }
            }
        }
        notifyDataSetChanged()
    }

    private fun formatDelay(delay: Int): String = when {
        delay <= 0 -> "—"
        delay <= Short.MAX_VALUE -> "${delay}ms"
        else -> "超时"
    }

    private fun delayColor(delay: Int): Int = when {
        delay <= 0 -> COLOR_NONE
        delay > Short.MAX_VALUE -> COLOR_BAD
        delay <= 150 -> COLOR_GOOD
        delay <= 300 -> COLOR_MEDIUM
        else -> COLOR_BAD
    }

    private fun setBadge(badge: TextView, delay: Int) {
        badge.text = formatDelay(delay)
        val dp = badge.context.resources.displayMetrics.density
        val bg = GradientDrawable().apply {
            cornerRadius = dp * 10
            setColor(delayColor(delay))
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

        fun bind(group: GroupData) {
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

        fun bind(item: ListItem.Node) {
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
