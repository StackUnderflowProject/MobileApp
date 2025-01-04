package com.example.spotter

interface EventClickListener {
    fun onNotifyButtonClick(event: Event)
    fun onEventEditClick(event: Event)
    fun onEventDeleteClick(event: Event)
    fun onSubscribeClick(event: Event)
}