package com.example.spotter.ui.dashboard

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerViewAccessibilityDelegate
import com.example.spotter.AddEventFragment
import com.example.spotter.CustomItemDecoration
import com.example.spotter.Event
import com.example.spotter.EventClickListener
import com.example.spotter.EventViewModel
import com.example.spotter.SpotterApp
import com.example.spotter.databinding.FragmentDashboardBinding
import org.osmdroid.views.overlay.simplefastpoint.SimplePointTheme
import com.example.spotter.EventsAdapter
import com.example.spotter.MainActivity
import com.example.spotter.R
import com.example.spotter.RetrofitInstance
import com.example.spotter.UpdateEventFragment
import com.example.spotter.databinding.ActivityMainBinding
import com.example.spotter.databinding.FragmentListEventsBinding
import com.example.spotter.ui.home.LocalDateDeserializer
import com.example.spotter.ui.home.LocalDateSerializer
import com.example.spotter.ui.home.ObjectIdSerializer
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.bson.types.ObjectId
import java.time.LocalDate
import java.util.UUID

class DashboardFragment : Fragment(), EventClickListener {

    private lateinit var binding: FragmentListEventsBinding
    private lateinit var myApp : SpotterApp
    private lateinit var eventsAdapter: EventsAdapter
    private lateinit var eventsViewModel : EventViewModel
    private lateinit var gson : Gson
    private var containerView: ViewGroup? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentListEventsBinding.inflate(inflater, container, false)
        containerView = container

        myApp = requireActivity().application as SpotterApp

        val gsonBuilder = GsonBuilder()
        gsonBuilder.registerTypeAdapter(LocalDate::class.java, LocalDateSerializer())
        gsonBuilder.registerTypeAdapter(LocalDate::class.java, LocalDateDeserializer())
        gsonBuilder.registerTypeAdapter(ObjectId::class.java, ObjectIdSerializer())
        gsonBuilder.registerTypeAdapter(ObjectId::class.java, RetrofitInstance.ObjectIdDeserializer())
        gson = gsonBuilder.create()

        eventsViewModel = (requireActivity().application as SpotterApp).eventsViewModel

        eventsViewModel.currentEvents.observe(viewLifecycleOwner, Observer {
            events.clear()
            events.addAll(it)
            if (!::eventsAdapter.isInitialized) {
                eventsAdapter = EventsAdapter(requireContext(), events, this, myApp.user)
                val itemDecoration = CustomItemDecoration(10, ContextCompat.getColor(requireContext(), R.color.gap_color))
                binding.recyclerView.addItemDecoration(itemDecoration)
                binding.recyclerView.layoutManager = LinearLayoutManager(context)
                binding.recyclerView.adapter = eventsAdapter
                val eventId = arguments?.getString("eventNotifcation")
                eventId?.let { id ->
                    val eventId = ObjectId(id)
                    val position = events.indexOfFirst { it._id == eventId }
                    if (position != -1) {
                        binding.recyclerView.post {
                            binding.recyclerView.scrollToPosition(position)
                        }
                    }
                }
            } else {
                when (eventsViewModel.action) {
                    0 -> eventsAdapter.notifyDataSetChanged()
                    1 -> eventsAdapter.notifyItemInserted(it.size - 1)
                    2 -> eventsAdapter.notifyItemRemoved(eventsViewModel.index)
                    3 -> eventsAdapter.notifyItemChanged(eventsViewModel.index)
                }
            }
        })

        binding.btnAdd.setOnClickListener {
            /*
            val fragmenttransaction = requireactivity().supportfragmentmanager.begintransaction()
            fragmenttransaction.replace(.id, addeventfragment())
            fragmenttransaction.addtobackstack(null)
            fragmenttransaction.commit()*/
            (requireActivity() as MainActivity).launchFragment(AddEventFragment(), "AddEventFragment")
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        if (scrollActive && scrollEvent != null) {
            val pos = events.indexOfFirst { it._id == scrollEvent!!._id }
            Log.i("Output", "${events.size}")
            if (pos != -1) {
                scrollActive = false
                binding.recyclerView.post {binding.recyclerView.layoutManager!!.smoothScrollToPosition(binding.recyclerView, RecyclerView.State(), pos) }
            }
        }
    }

    override fun onEventEditClick(event: Event) {
        if (containerView == null) return
        val f = UpdateEventFragment()
        val b = Bundle()
        b.putString("updateEvent", gson.toJson(event))
        f.arguments = b
        /*
        val fragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(containerView!!.id, f)
        fragmentTransaction.addToBackStack("UpdateEventFragment")
        fragmentTransaction.commit()*/
        (requireActivity() as MainActivity).launchFragment(f, "UpdateEventFragment")
    }

    override fun onEventDeleteClick(event: Event) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.delete_item))  // Title of the dialog
        builder.setMessage(getString(R.string.delete_item_question))  // Message asking for confirmation
        builder.setNegativeButton(getString(R.string.yes)) { dialog, which ->
            eventsViewModel.removeItem(myApp.user, event) {success ->
                if (!success) Toast.makeText(requireContext(), getString(R.string.failed_to_delete_event), Toast.LENGTH_SHORT).show()
            }
        }
        builder.setPositiveButton(getString(R.string.no)) { dialog, which ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    override fun onSubscribeClick(event: Event) {
        eventsViewModel.followEvent(event, myApp.user)
        myApp.scheduleNotification(event)
    }

    override fun onDeleteClick(event: Event) {

    }

    companion object {
        var scrollActive = false
        var scrollEvent : Event? = null

        private var events : MutableList<Event> = mutableListOf()
    }
}