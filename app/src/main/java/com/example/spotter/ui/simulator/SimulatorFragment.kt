package com.example.spotter.ui.simulator

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
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
    private  var footballMatches: List<Match> = emptyList()
    private  var handballMatches: List<Match> = emptyList()
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

        binding.tvLogs.movementMethod = ScrollingMovementMethod()

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

        appendLog("All simulations stopped")
        Log.d("SimulatorFragment", "All simulations stopped")
    }

    private fun appendLog(message: String) {
        binding.tvLogs.append("$message\n")
        // Scroll to the bottom of the TextView
        binding.tvLogs.post {
            val scrollAmount = binding.tvLogs.layout.getLineTop(binding.tvLogs.lineCount) - binding.tvLogs.height
            if (scrollAmount > 0) {
                binding.tvLogs.scrollTo(0, scrollAmount)
            } else {
                binding.tvLogs.scrollTo(0, 0)
            }
        }
    }


    private fun startSimulation(
        matches: List<Match>,
        totalMinutes: Int,
        delayPerMinute: Long
    ): Job {
        appendLog("Starting simulation with ${matches.size} matches")

        return lifecycleScope.launch {
            val matchDurations = matches.associateWith { match ->
                // Extract the current minute from the match's time (e.g., "45'")
                val currentTime = match.time.removeSuffix("'").toIntOrNull() ?: 0
                currentTime to totalMinutes - currentTime // Calculate remaining minutes
            }

            matchDurations.forEach { (match, times) ->
                appendLog("\nMatch ${match._id} starts simulation at ${times.first}' with ${times.second} minutes remaining")
            }

            // Simulate matches concurrently
            for (minute in 1..totalMinutes) {
                delay(delayPerMinute)

                matchDurations.forEach { (match, times) ->
                    val (startMinute, remainingMinutes) = times
                    val currentMinute = startMinute + minute
                    if (minute <= remainingMinutes && currentMinute <= totalMinutes) {
                        appendLog("\nMinute $currentMinute for Match ${match._id}:")
                        updateMatchTimeAndScoreForSingleMatch(match, currentMinute)
                    }
                }

                if (!isActive) {
                    appendLog("Simulation stopped at minute $minute")
                    break
                }
            }
        }
    }

    private fun updateMatchTimeAndScoreForSingleMatch(match: Match, currentMinute: Int) {
        // Update time for the match
        match.time = "$currentMinute'"

        // Adjust scoring chance based on the sport
        val scoringChance = if (match in footballMatches) 4 else 2 // ~25% for football, ~50% for handball
        if ((1..scoringChance).random() == 1) {
            val (homeScore, awayScore) = match.score.replace(" ", "").split("-", ":", limit = 2).map { it.toInt() }
            val newScore = if ((0..1).random() == 0) {
                "${homeScore + 1} : $awayScore"
            } else {
                "$homeScore : ${awayScore + 1}"
            }
            match.score = newScore
            appendLog("\t- Score updated ${match.score}")
        }

        // Update match via API
        val updateMatchApi = if (match in footballMatches) {
            RetrofitInstance.api::updateFootballMatch
        } else {
            RetrofitInstance.api::updateHandballMatch
        }

        updateMatchApi("Bearer: ${myApp.user?.token!!}", match._id, match).enqueue(object : Callback<Match> {
            override fun onResponse(call: Call<Match>, response: Response<Match>) {
                if (response.isSuccessful) {
                    val updatedMatch = response.body()
                    updatedMatch?.let { updated ->
                        Log.i("SimulatorFragment", "Match updated successfully: $updated")
                    }
                } else {
                    Log.i("SimulatorFragment", "Failed to update match: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Match>, t: Throwable) {
                Log.e("SimulatorFragment", "Failed to update match: ${t.message}")
            }
        })

        // Notify the socket about the update
        myApp.sendUpdateMatchOnSocket()
        appendLog("\t- Socket event sent")
    }

}
