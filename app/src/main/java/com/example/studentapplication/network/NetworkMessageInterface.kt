package com.example.studentapplication.network

import com.example.studentapplication.models.ChatContentModel

interface NetworkMessageInterface {
    fun onContent(content: ChatContentModel)
}