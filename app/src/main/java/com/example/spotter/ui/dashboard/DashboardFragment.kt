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
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerViewAccessibilityDelegate
import com.example.spotter.AddEventFragment
import com.example.spotter.Event
import com.example.spotter.EventClickListener
import com.example.spotter.EventViewModel
import com.example.spotter.SpotterApp
import com.example.spotter.databinding.FragmentDashboardBinding
import org.osmdroid.views.overlay.simplefastpoint.SimplePointTheme
import com.example.spotter.EventsAdapter
import com.example.spotter.R
import com.example.spotter.databinding.ActivityMainBinding
import com.example.spotter.databinding.FragmentListEventsBinding

class DashboardFragment : Fragment(), EventClickListener {

    private lateinit var binding: FragmentListEventsBinding
    private lateinit var myApp : SpotterApp
    private lateinit var eventsAdapter: EventsAdapter
    private lateinit var eventsViewModel : EventViewModel

    private var events : MutableList<Event> = mutableListOf()
    private var scrollIndex = -1

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
                if (eventsViewModel.action == 1) scrollIndex = it.size - 1
            }
        })

        binding.btnAdd.setOnClickListener {
            val fragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
            fragmentTransaction.replace(container!!.id, AddEventFragment())
            fragmentTransaction.addToBackStack(null)
            fragmentTransaction.commit()
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        if (scrollIndex != -1) {
            binding.recyclerView.post {binding.recyclerView.layoutManager!!.smoothScrollToPosition(binding.recyclerView, RecyclerView.State(), scrollIndex) }
            scrollIndex = -1
        }
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

    override fun onDeleteClick(event: Event) {

    }
}