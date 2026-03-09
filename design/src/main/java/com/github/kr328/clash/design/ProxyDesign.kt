package com.github.kr328.clash.design

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.design.adapter.ProxyGroupAdapter
import com.github.kr328.clash.design.databinding.DesignProxyBinding
import com.github.kr328.clash.design.model.ProxyState
import com.github.kr328.clash.design.store.UiStore
import com.github.kr328.clash.design.util.applyFrom
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.root
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProxyDesign(
    context: Context,
    overrideMode: TunnelState.Mode?,
    private val groupNames: List<String>,
    uiStore: UiStore,
) : Design<ProxyDesign.Request>(context) {
    sealed class Request {
        object ReloadAll : Request()
        object ReLaunch : Request()

        data class PatchMode(val mode: TunnelState.Mode?) : Request()
        data class Reload(val index: Int) : Request()
        data class Select(val index: Int, val name: String) : Request()
        data class UrlTest(val index: Int) : Request()
        data class AutoSelect(val index: Int) : Request()
    }

    private val binding = DesignProxyBinding
        .inflate(context.layoutInflater, context.root, false)

    private val proxyAdapter = ProxyGroupAdapter(
        groupNames = groupNames,
        onSelectNode = { groupIndex, nodeName ->
            requests.trySend(Request.Select(groupIndex, nodeName))
        },
        onUrlTest = { groupIndex ->
            requests.trySend(Request.UrlTest(groupIndex))
        }
    )

    override val root: View = binding.root

    suspend fun updateGroup(
        position: Int,
        proxies: List<Proxy>,
        selectable: Boolean,
        parent: ProxyState,
        links: Map<String, ProxyState>
    ) {
        withContext(Dispatchers.Main) {
            proxyAdapter.updateGroup(position, proxies, selectable, parent)
            if (groupNames.isEmpty()) {
                binding.emptyView.visibility = View.VISIBLE
            } else {
                binding.emptyView.visibility = View.GONE
            }
        }
    }

    suspend fun requestRedrawVisible() {
        withContext(Dispatchers.Main) {
            proxyAdapter.notifySelectionChanged()
        }
    }

    suspend fun showModeSwitchTips() {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, R.string.mode_switch_tips, Toast.LENGTH_LONG).show()
        }
    }

    fun requestUrlTesting() {
        requests.trySend(Request.ReloadAll)
    }

    private fun applyMode(mode: TunnelState.Mode?) {
        binding.chipRule.isChecked = mode == null || mode == TunnelState.Mode.Rule
        binding.chipGlobal.isChecked = mode == TunnelState.Mode.Global
        binding.chipDirect.isChecked = mode == TunnelState.Mode.Direct
    }

    init {
        binding.self = this
        binding.activityBarLayout.applyFrom(context)

        binding.proxyList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = proxyAdapter
            setHasFixedSize(false)
        }

        if (groupNames.isEmpty()) {
            binding.emptyView.visibility = View.VISIBLE
        }

        applyMode(overrideMode)

        binding.chipRule.setOnClickListener {
            requests.trySend(Request.PatchMode(TunnelState.Mode.Rule))
            applyMode(TunnelState.Mode.Rule)
        }
        binding.chipGlobal.setOnClickListener {
            requests.trySend(Request.PatchMode(TunnelState.Mode.Global))
            applyMode(TunnelState.Mode.Global)
        }
        binding.chipDirect.setOnClickListener {
            requests.trySend(Request.PatchMode(TunnelState.Mode.Direct))
            applyMode(TunnelState.Mode.Direct)
        }

        // Search field
        binding.searchField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                proxyAdapter.filter(s?.toString() ?: "")
            }
        })

        // Sort by delay toggle
        var sortByDelay = false
        binding.sortButton.setOnClickListener {
            sortByDelay = !sortByDelay
            proxyAdapter.setSortByDelay(sortByDelay)
            binding.sortButton.alpha = if (sortByDelay) 1.0f else 0.5f
        }
        binding.sortButton.alpha = 0.5f
    }

    /** Auto-select fastest node in the given group */
    fun autoSelectFastest(groupIndex: Int): String? {
        return proxyAdapter.autoSelectFastest(groupIndex)
    }
}
