package com.lamentcfg.ntfymirror.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lamentcfg.ntfymirror.databinding.ItemChannelBinding

/**
 * RecyclerView adapter for displaying a list of notification channels with enable/disable toggles.
 */
class ChannelListAdapter(
    private val onChannelToggle: (ChannelInfo, Boolean) -> Unit
) : ListAdapter<ChannelInfo, ChannelListAdapter.ChannelViewHolder>(ChannelDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val binding = ItemChannelBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChannelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        holder.bind(getItem(position), onChannelToggle)
    }

    class ChannelViewHolder(
        private val binding: ItemChannelBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            channelInfo: ChannelInfo,
            onChannelToggle: (ChannelInfo, Boolean) -> Unit
        ) {
            binding.channelName.text = channelInfo.channelName

            // Hide channel ID - only show friendly name
            binding.channelId.visibility = android.view.View.GONE

            // Show status indicator for ongoing notifications
            if (channelInfo.hasOngoingNotification) {
                binding.channelStatus.text = "ongoing"
                binding.channelStatus.visibility = android.view.View.VISIBLE
            } else {
                binding.channelStatus.visibility = android.view.View.GONE
            }

            // Set switch without triggering listener
            binding.channelToggle.setOnCheckedChangeListener(null)
            binding.channelToggle.isChecked = channelInfo.isEnabled

            binding.channelToggle.setOnCheckedChangeListener { _, isChecked ->
                onChannelToggle(channelInfo, isChecked)
            }
        }
    }

    class ChannelDiffCallback : DiffUtil.ItemCallback<ChannelInfo>() {
        override fun areItemsTheSame(oldItem: ChannelInfo, newItem: ChannelInfo): Boolean {
            return oldItem.channelId == newItem.channelId && oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: ChannelInfo, newItem: ChannelInfo): Boolean {
            return oldItem == newItem
        }
    }
}
