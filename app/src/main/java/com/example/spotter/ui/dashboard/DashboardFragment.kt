package com.example.spotter.ui.dashboard

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spotter.Event
import com.example.spotter.EventClickListener
import com.example.spotter.EventViewModel
import com.example.spotter.SpotterApp
import com.example.spotter.databinding.FragmentDashboardBinding
import org.osmdroid.views.overlay.simplefastpoint.SimplePointTheme
import com.example.spotter.EventsAdapter
import com.example.spotter.R
import com.example.spotter.databinding.FragmentListEventsBinding

class DashboardFragment : Fragment(), EventClickListener {

    private lateinit var binding: FragmentListEventsBinding
    private lateinit var myApp : SpotterApp
    private lateinit var eventsAdapter: EventsAdapter
    private lateinit var eventsViewModel : EventViewModel

    private var events : MutableList<Event> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentListEventsBinding.inflate(inflater, container, false)

        myApp = requireActivity().application as SpotterApp

        eventsViewModel = (requireActivity().application as SpotterApp).eventsViewModel

        eventsViewModel.currentEvents.observe(viewLifecycleOwner, Observer {
            events.clear()
            events.addAll(it)
            if (!::eventsAdapter.isInitialized) {
                eventsAdapter = EventsAdapter(requireContext(), events, this, myApp.user)
                binding.recyclerView.layoutManager = LinearLayoutManager(context)
                binding.recyclerView.adapter = eventsAdapter
            } else {
                when (eventsViewModel.action) {
                    0 -> eventsAdapter.notifyDataSetChanged()
                    1 -> eventsAdapter.notifyItemInserted(it.size - 1)
                    2 -> eventsAdapter.notifyItemRemoved(eventsViewModel.index)
                    3 -> eventsAdapter.notifyItemChanged(eventsViewModel.index)
                }
            }
        })

        savedInstanceState?.let {
            val eventPosition = savedInstanceState.getInt("eventPosition", -1)
            if (eventPosition > 0) {
                binding.recyclerView.smoothScrollToPosition(eventPosition)
            }
        }

        return binding.root
    }

    override fun onNotifyButtonClick(event: Event) {
        // TODO
    }

    override fun onEventEditClick(event: Event) {
        // TODO
    }

    override fun onEventDeleteClick(event: Event) {
        // TODO
    }

    override fun onSubscribeClick(event: Event) {
        eventsViewModel.followEvent(event, myApp.user)
    }
}