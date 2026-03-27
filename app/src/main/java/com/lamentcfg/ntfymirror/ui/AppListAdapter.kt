package com.lamentcfg.ntfymirror.ui

import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lamentcfg.ntfymirror.R
import com.lamentcfg.ntfymirror.databinding.ItemAppBinding

/**
 * RecyclerView adapter for displaying a list of apps with expandable channels.
 */
class AppListAdapter(
    private val onAppToggle: (AppInfo, Boolean) -> Unit,
    private val onChannelToggle: (ChannelInfo, Boolean) -> Unit,
    private val onAppExpanded: (AppInfo) -> Unit = {}
) : ListAdapter<AppInfo, AppListAdapter.AppViewHolder>(AppDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position), onAppToggle, onChannelToggle, onAppExpanded)
    }

    class AppViewHolder(
        private val binding: ItemAppBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var channelAdapter: ChannelListAdapter? = null
        private var isExpanded = false

        init {
            binding.channelsRecyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                isNestedScrollingEnabled = false
            }
        }

        fun bind(
            appInfo: AppInfo,
            onAppToggle: (AppInfo, Boolean) -> Unit,
            onChannelToggle: (ChannelInfo, Boolean) -> Unit,
            onAppExpanded: (AppInfo) -> Unit
        ) {
            binding.appIcon.setImageDrawable(appInfo.icon)
            binding.appName.text = appInfo.appName
            binding.appPackage.visibility = View.GONE

            // Set switch without triggering listener
            binding.appToggle.setOnCheckedChangeListener(null)
            binding.appToggle.isChecked = appInfo.isEnabled

            binding.appToggle.setOnCheckedChangeListener { _, isChecked ->
                onAppToggle(appInfo, isChecked)
            }

            // Show channel count and expand icon if there are channels
            val hasChannels = appInfo.channels.isNotEmpty()
            if (hasChannels) {
                binding.channelCount.text = binding.root.context.resources.getQuantityString(
                    R.plurals.channel_count_plural,
                    appInfo.channels.size,
                    appInfo.channels.size
                )
                binding.channelCount.visibility = View.VISIBLE
                binding.expandIcon.visibility = View.VISIBLE
            } else {
                binding.channelCount.visibility = View.GONE
                binding.expandIcon.visibility = View.GONE
            }

            // Update channel adapter with proper toggle callback
            channelAdapter = ChannelListAdapter { channelInfo, isEnabled ->
                onChannelToggle(channelInfo, isEnabled)
            }
            binding.channelsRecyclerView.adapter = channelAdapter
            channelAdapter?.submitList(appInfo.channels)

            // Set expansion state
            isExpanded = appInfo.isExpanded
            updateExpansionState(animate = false)

            // Header click to expand/collapse (only if there are channels)
            binding.appHeader.setOnClickListener {
                if (!hasChannels) {
                    return@setOnClickListener
                }
                isExpanded = !isExpanded
                updateExpansionState(animate = true)
                if (isExpanded) {
                    onAppExpanded(appInfo)
                }
            }
        }

        private fun updateExpansionState(animate: Boolean) {
            binding.expandIcon.rotation = if (isExpanded) 180f else 0f

            if (animate) {
                val rotationAnimator = ValueAnimator.ofFloat(
                    if (isExpanded) 0f else 180f,
                    if (isExpanded) 180f else 0f
                ).apply {
                    duration = 200
                    interpolator = AccelerateDecelerateInterpolator()
                    addUpdateListener { animation ->
                        binding.expandIcon.rotation = animation.animatedValue as Float
                    }
                }
                rotationAnimator.start()
            }

            binding.channelsContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
        }
    }

    class AppDiffCallback : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem == newItem
        }
    }
}
