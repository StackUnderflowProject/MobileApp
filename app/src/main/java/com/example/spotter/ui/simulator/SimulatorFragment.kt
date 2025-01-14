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
import com.example.spotter.databinding.FragmentSimulatorBinding
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
    private lateinit var myApp: SpotterApp
    private lateinit var footballMatches: List<Match>
    private lateinit var handballMatches: List<Match>
    private var footballSimulationJob: Job? = null
    private var handballSimulationJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSimulatorBinding.inflate(inflater, container, false)

        myApp = requireActivity().application as SpotterApp

        // Set up button listeners
        binding.btnStartFootballSimulation.setOnClickListener {
            startFootballSimulation()
        }

        binding.btnStartHandballSimulation.setOnClickListener {
            startHandballSimulation()
        }

        binding.btnStopSimulation.setOnClickListener {
            stopSimulation()
        }

        lifecycleScope.launch {
            try {
                Log.d("SimulatorFragment", "Fetching football matches for ${LocalDate.now()}")
                footballMatches = RetrofitInstance.api.getFootballMatches(
                    LocalDate.now().toString(),
                    LocalDate.now().plusDays(1).toString()
                )
                Log.d("SimulatorFragment", "Fetched ${footballMatches.size} football matches")

                Log.d("SimulatorFragment", "Fetching handball matches for ${LocalDate.now()}")
                handballMatches = RetrofitInstance.api.getHandballMatches(
                    LocalDate.now().toString(),
                    LocalDate.now().plusDays(1).toString()
                )
                Log.d("SimulatorFragment", "Fetched ${handballMatches.size} handball matches")
            } catch (e: Exception) {
                Log.e("SimulatorFragment", "Error fetching matches", e)
            }
        }

        return binding.root
    }

    private fun startFootballSimulation() {
        if (footballMatches.isEmpty()) {
            Log.d("SimulatorFragment", "No football matches available for simulation")
            return
        }

        footballSimulationJob = startSimulation(
            matches = footballMatches,
            totalMinutes = 90,
            delayPerMinute = 5_000 // 1 minute delay
        )
    }

    private fun startHandballSimulation() {
        if (handballMatches.isEmpty()) {
            Log.d("SimulatorFragment", "No handball matches available for simulation")
            return
        }

        handballSimulationJob = startSimulation(
            matches = handballMatches,
            totalMinutes = 60,
            delayPerMinute = 5_000 // 30 seconds delay
        )
    }

    private fun stopSimulation() {
        footballSimulationJob?.cancel()
        footballSimulationJob = null

        handballSimulationJob?.cancel()
        handballSimulationJob = null

        Log.d("SimulatorFragment", "All simulations stopped")
    }

    private fun startSimulation(
        matches: List<Match>,
        totalMinutes: Int,
        delayPerMinute: Long
    ): Job {
        return lifecycleScope.launch {
            for (minute in 1..totalMinutes) {
                delay(delayPerMinute)
                updateMatchTimesAndScore(matches, minute)

                if (!isActive) {
                    Log.d("SimulatorFragment", "Simulation stopped")
                    break
                }
            }
        }
    }

    private fun updateMatchTimesAndScore(matches: List<Match>, currentMinute: Int) {
        // Update the time for all matches
        matches.forEach { match ->
            match.time = "$currentMinute'" // Update time for all matches
        }

        // Adjust scoring chance based on the sport
        val scoringChance = if (matches == footballMatches) 4 else 2 // ~25% for football, ~50% for handball
        if ((1..scoringChance).random() == 1) {
            val randomIdx = (matches.indices).randomOrNull() ?: return
            val (homeScore, awayScore) = matches[randomIdx].score.replace(" ", "").split("-").map { it.toInt() }
            val newScore = if ((0..1).random() == 0) {
                "${homeScore + 1} - $awayScore"
            } else {
                "$homeScore - ${awayScore + 1}"
            }
            matches[randomIdx].score = newScore
            Log.d("SimulatorFragment", "Updating score for match: ${matches[randomIdx]}")
        }

        // Determine the appropriate API endpoint based on the sport
        val updateMatchApi: (String, String, Match) -> Call<Match> = when (matches) {
            footballMatches -> RetrofitInstance.api::updateFootballMatch
            handballMatches -> RetrofitInstance.api::updateHandballMatch
            else -> {
                Log.e("SimulatorFragment", "Unknown match type, cannot update.")
                return
            }
        }

        // Send updates for all matches to the server
        matches.forEach { match ->
            updateMatchApi("Bearer: ${myApp.user?.token!!}", match._id, match).enqueue(object : Callback<Match> {
                override fun onResponse(call: Call<Match>, response: Response<Match>) {
                    if (response.isSuccessful) {
                        val updatedMatch = response.body()
                        updatedMatch?.let { updated ->
                            val currentList = matches.toMutableList()
                            val index = currentList.indexOfFirst { curr -> updated._id == curr._id }
                            if (index != -1) {
                                currentList[index] = updated
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
        }

        // Notify the socket about updates
        myApp.sendUpdateMatchOnSocket()
        Log.d("SimulatorFragment", "Updating matches with time: $currentMinute' and scores")
    }

}
