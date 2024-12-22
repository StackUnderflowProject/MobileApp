package com.example.spotter

import android.content.Context
import android.graphics.drawable.Drawable
import android.provider.Settings.Global.getString
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.spotter.databinding.FragmentEventBinding
import com.bumptech.glide.Glide
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EventsAdapter(val context: Context, private val events: List<Event>, private val listener: EventClickListener) : RecyclerView.Adapter<EventsAdapter.EventViewHolder>() {

    class EventViewHolder(val binding: FragmentEventBinding) : RecyclerView.ViewHolder(binding.root)

    // change from XML to View and store the binding to ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = FragmentEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event: Event = events[position]

        holder.binding.username.text = event.hostObj?.username ?: "Username not loaded"
        holder.binding.userEmail.text = event.hostObj?.email ?: "Email not loaded"
        holder.binding.eventDate.text = event.getLocalDate()
        holder.binding.eventTime.text = event.getLocalTime()
        holder.binding.title.text = event.title
        holder.binding.description.text = event.description
        holder.binding.location.text = event.location.toString()
        holder.binding.activity.text = event.activity
        holder.binding.activityIcon.setImageDrawable(
            when (event.activity) {
                "nogomet" -> Drawable.createFromPath("@drawable/download_removebg_preview__1_")
                "football" -> Drawable.createFromPath("@drawable/download_removebg_preview__1_")
                "rokomet" -> Drawable.createFromPath("@drawable/group_91")
                "handball" -> Drawable.createFromPath("@drawable/group_91")
                else -> Drawable.createFromPath("@drawable/group_92")
        })
        holder.binding.subscribeCount.text = event.followers.size.toString()

        if (event.hostObj != null && event.hostObj!!.iconPath.isEmpty()) {
            holder.binding.userIcon.setImageDrawable(Drawable.createFromPath("@drawable/download__5__removebg_preview"))
        } else {
            Glide.with(holder.itemView.context)
                .load(event.hostObj?.iconPath)
                .into(holder.binding.userIcon)
        }

        holder.binding.btnSubscribe.setOnClickListener {
            RetrofitInstance.api.subscribeToEvent(event.uuid).enqueue(object : Callback<ServerResponse> {
                override fun onResponse(
                    call: Call<ServerResponse>,
                    response: Response<ServerResponse>
                ) {
                    if (response.isSuccessful) {
                        val message = response.body()?.message
                        Log.i("Output", message ?: "no message")
                        // notify item changed myb
                    } else {
                        Log.i("Output", "Error: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<ServerResponse>, t: Throwable) {
                    Log.i("Output", "Failed ${t.message}")
                }
            })
        }

        holder.binding.btnOptions.setOnClickListener {

        }
        /*
        holder.binding.btnNotify.text = if (event.notifyOn) context.getString(R.string.will_notify) else context.getString(R.string.notify_me)

        holder.binding.btnNotify.setOnClickListener {
            holder.binding.btnNotify.text = context.getString(R.string.will_notify)
            if (event.notifyOn) return@setOnClickListener
            listener.onNotifyButtonClick(event)
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
}