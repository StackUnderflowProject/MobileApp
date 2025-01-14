package com.example.spotter.ui.simulator

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.spotter.Match
import com.example.spotter.RetrofitInstance
import com.example.spotter.SpotterApp
import com.example.spotter.User
import com.example.spotter.databinding.FragmentSimulatorBinding
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDate

class SimulatorFragment : Fragment() {
    private lateinit var binding: FragmentSimulatorBinding
    private lateinit var myApp : SpotterApp
    private lateinit var matches : List<Match>
    private var simulationJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSimulatorBinding.inflate(inflater, container, false)

        myApp = requireActivity().application as SpotterApp

        binding.btnStartSimulation.setOnClickListener {
            startSimulation()
        }

        binding.btnStopSimulation.setOnClickListener {
            stopSimulation()
        }

        lifecycleScope.launch {
            try {
                Log.d("SimulatorFragment", "Fetching matches for ${LocalDate.now()}")
                matches = RetrofitInstance.api.getFootballMatches(LocalDate.now().toString(), LocalDate.now().plusDays(1).toString())
                Log.d("SimulatorFragment", "Fetched ${matches.size} matches")
                // Start the simulation when matches are fetched
            } catch (e: Exception) {
                Log.e("SimulatorFragment", "Error fetching matches", e)
            }
        }

        return binding.root
    }

    private fun startSimulation() {
        if(matches.isEmpty()) return

        simulationJob = lifecycleScope.launch {
            for (minute in 1..90) {
                delay(5_000) // Wait for 1 minute
                updateMatchTimesAndScore(minute)

                if (!isActive) {
                    Log.d("SimulatorFragment", "Simulation stopped")
                    break
                }
            }
        }
    }

    private fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
        Log.d("SimulatorFragment", "Simulation stopped")
    }

    private fun updateMatchTimesAndScore(currentMinute: Int) {
        // Update the time for all matches and occasionally update the score for one
        matches.forEach { match ->
            match.time = "$currentMinute'" // Update time for all matches
        }

        if ((1..4).random() == 1) { // ~25% chance to update a score
            val randomIdx = (matches.indices).randomOrNull() ?: return
            val (homeScore, awayScore) = matches[randomIdx].score.split(" - ").map { it.toInt() }
            val newScore = if ((0..1).random() == 0) {
                "${homeScore + 1} - $awayScore"
            } else {
                "$homeScore - ${awayScore + 1}"
            }
            matches[randomIdx].score = newScore
            Log.d("SimulatorFragment", "Updating score for match: ${matches[randomIdx]}")
        }

        // Send updates for all matches to the server
        matches.forEach { match ->
            RetrofitInstance.api.updateFootballMatch("Bearer: ${myApp.user?.token!!}", match._id, match).enqueue(object : Callback<Match> {
                override fun onResponse(call: Call<Match>, response: Response<Match>) {
                    if (response.isSuccessful) {
                        val updatedMatch = response.body()
                        updatedMatch?.let { updated ->
                            val currentList = matches.toMutableList()
                            val index = currentList.indexOfFirst { curr -> updated._id == curr._id }
                            if (index != -1) {
                                currentList[index] = updated
                                matches = currentList
                                Log.i("SimulatorFragment", "updateMatchTimesAndScore(), Success: $updated")
                            }
                        }
                    } else {
                        Log.i("SimulatorFragment", "updateMatchTimesAndScore(), Error: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<Match>, t: Throwable) {
                    Log.e("SimulatorFragment", "updateMatchTimesAndScore() failed: ${t.message}")
                }
            })
            myApp.sendUpdateMatchOnSocket()
        }

        // Notify the socket about updates
        Log.d("SimulatorFragment", "Updating matches with time: $currentMinute' and scores")
    }




}