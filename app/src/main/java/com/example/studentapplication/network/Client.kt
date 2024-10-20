package com.example.studentapplication.network

import android.util.Log
import android.widget.Toast
import com.example.studentapplication.encryption.Encrypter
import com.google.gson.Gson
import com.example.studentapplication.models.ChatContentModel
import java.io.BufferedReader
import java.io.BufferedWriter
import java.net.Socket
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.concurrent.thread


class Client (
    private val networkMessageInterface: NetworkMessageInterface,){

    private lateinit var clientSocket: Socket
    private lateinit var reader: BufferedReader
    private lateinit var writer: BufferedWriter
    var studentID : String = ""
    var goIp : String = "192.168.49.1"
    var clientIp:String = ""
    private val encrypter = Encrypter()
    private var aesKey: SecretKeySpec? = null
    private var aesIv: IvParameterSpec? = null
    var isAuthenticated = false

    init {
        thread {
            clientSocket = Socket("192.168.49.1", 9999)
            reader = clientSocket.inputStream.bufferedReader()
            writer = clientSocket.outputStream.bufferedWriter()
            clientIp = clientSocket.inetAddress.hostAddress!!
            while(true){
                try{
                    val serverResponse = reader.readLine()
                    if (serverResponse != null){
                        val serverContent = Gson().fromJson(serverResponse, ChatContentModel::class.java)
                        networkMessageInterface.onContent(serverContent)
                    }
                } catch(e: Exception){
                    Log.e("CLIENT", "An error has occurred in the client")
                    e.printStackTrace()
                    break
                }
            }
        }
    }

    fun sendInitialMessage(clientIp : String) {
        val initialMessage = "I am here"
        val content = ChatContentModel(initialMessage, clientIp)
        thread {
            if (!clientSocket.isConnected){
                throw Exception("We aren't currently connected to the server!")
            }
            val contentAsStr:String = Gson().toJson(content)
            writer.write("$contentAsStr\n")
            writer.flush()
        }
    }

    fun sendMessage(content: ChatContentModel){
        thread {
            if (!clientSocket.isConnected){
                throw Exception("We aren't currently connected to the server!")
            }
            val contentAsStr:String = Gson().toJson(content)
            writer.write("$contentAsStr\n")
            writer.flush()
        }
    }

    private fun handleServerResponse(response: String) {
        // Check if the response is the random number R
        try{
            if (response!= null){
                Log.e("SERVER", "Received a message from client")
                val clientContent = Gson().fromJson(response, ChatContentModel::class.java)
                networkMessageInterface.onContent(clientContent)
            }
        } catch (e: Exception){
            Log.e("SERVER", "An error has occurred with the client")
            e.printStackTrace()
        }
    }


    fun close(){
        clientSocket.close()
    }

}