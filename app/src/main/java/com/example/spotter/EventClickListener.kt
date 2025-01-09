package com.example.spotter

interface EventClickListener {
    fun onEventEditClick(event: Event)
    fun onEventDeleteClick(event: Event)
    fun onSubscribeClick(event: Event)
    fun onDeleteClick(event: Event)
}