package com.example.spotter

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.spotter.databinding.FragmentEventBinding
import com.squareup.picasso.Picasso
import java.util.Locale

class EventsAdapter(val context: Context, private val events: List<Event>, private val listener: EventClickListener, private var user: User?) : RecyclerView.Adapter<EventsAdapter.EventViewHolder>() {

    class EventViewHolder(val binding: FragmentEventBinding) : RecyclerView.ViewHolder(binding.root)

    // change from XML to View and store the binding to ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = FragmentEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event: Event = events[position]
        Log.i("Output2", event.toString())
        holder.binding.username.text = event.hostObj?.username ?: "Username not loaded"
        holder.binding.userEmail.text = event.hostObj?.email ?: "Email not loaded"
        holder.binding.eventDate.text = "${event.date.dayOfMonth}-${event.date.monthValue}-${event.date.year}"
        holder.binding.eventTime.text = formatTime(event.time)
        holder.binding.title.text = event.name
        holder.binding.description.text = event.description
        holder.binding.location.text = getAddressFromCoordinates(event.location.coordinates[1], event.location.coordinates[0]) ?: "Unknown location"
        holder.binding.activity.text = event.activity
        holder.binding.activityIcon.setImageDrawable(
            when (event.activity.lowercase()) {
                "nogomet", "futsal", "football" -> ContextCompat.getDrawable(context, R.drawable.download_removebg_preview__1_)
                "rokomet", "handball" -> ContextCompat.getDrawable(context, R.drawable.group_91)
                else -> ContextCompat.getDrawable(context, R.drawable.group_92)
        })
        holder.binding.subscribeCount.text = event.followers.size.toString()

        if (event.hostObj == null || event.hostObj?.image.isNullOrEmpty()) {
            holder.binding.userIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.download__5__removebg_preview))
        } else {
            Picasso.get()
                .load("http://77.38.76.152:3000/public/images/profile_pictures/" + (event.hostObj?.image ?: ""))
                .error(ContextCompat.getDrawable(context, R.drawable.download__5__removebg_preview)!!)
                .into(holder.binding.userIcon)
        }

        holder.binding.btnSubscribe.text = if (event.followers.any { it == user?._id }) "Subscribed" else "Subscribe"
        holder.binding.btnSubscribe.setOnClickListener {
            listener.onSubscribeClick(event)
        }

        holder.binding.btnOptions.setOnClickListener {
            val popupMenu = PopupMenu(context, holder.binding.btnOptions)
            popupMenu.menuInflater.inflate(R.menu.event_options, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit -> {
                        listener.onEventEditClick(event)
                        true
                    }
                    R.id.action_delete -> {
                        listener.onEventDeleteClick(event)
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }
        /*
        holder.binding.btnNotify.text = if (event.notifyOn) context.getString(R.string.will_notify) else context.getString(R.string.notify_me)

        holder.binding.btnNotify.setOnClickListener {
            holder.binding.btnNotify.text = context.getString(R.string.will_notify)
            if (event.notifyOn) return@setOnClickListener
            listener.onNotifyButtonClick(event)
        }

        holder.binding.btnDelete.setOnClickListener {

        }

        holder.binding.root.setOnClickListener {
            listener.onEventEditClick(event)
        }

        holder.binding.root.setOnLongClickListener {
            listener.onEventDeleteClick(event)
            return@setOnLongClickListener true
        }
         */
    }

    override fun getItemCount(): Int {
        return events.size
    }

    private fun getAddressFromCoordinates(latitude: Double, longitude: Double): String? {
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                addresses[0].getAddressLine(0) // Full address
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun formatTime(timeString: String): String {
        val timeParts = timeString.split(":")
        val hours = timeParts[0]
        val minutes = timeParts[1]
        return "$hours:${if (minutes.length == 2) minutes else "0$minutes"}"
    }
}