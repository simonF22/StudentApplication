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
    var goIp : String = ""
    var clientIp:String = ""
    private val encrypter = Encrypter()
    private var aesKey: SecretKeySpec? = null
    private var aesIv: IvParameterSpec? = null
    var isAuthenticated = false

    init {
        thread {
            clientSocket = Socket(goIp, 9999)
            reader = clientSocket.inputStream.bufferedReader()
            writer = clientSocket.outputStream.bufferedWriter()
            clientIp = clientSocket.inetAddress.hostAddress!!

            while(true){
                try{
                    val serverResponse = reader.readLine()
                    if (serverResponse != null){
                        //handleServerResponse(serverResponse)
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

    /*fun sendInitialMessage() {
        val initialMessage = "I am here"
        sendMessage(ChatContentModel(initialMessage, clientIp))
    }*/

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
        val serverContent = Gson().fromJson(response, ChatContentModel::class.java)
        networkMessageInterface.onContent(serverContent)
        /*if (serverContent.message.startsWith("R:")) {
            val randomNumber = serverContent.message.removePrefix("R:")
            //authenticateStudent(randomNumber)
        } else {
            // Handle other messages (chat messages)
            networkMessageInterface.onContent(serverContent)
        }*/
    }

    /*private fun authenticateStudent(randomNumber: String) {
        val studentHash = encrypter.hashStrSha256(studentID)
        aesKey = encrypter.generateAESKey(studentHash)
        aesIv = encrypter.generateIV(studentHash)
        val encryptedResponse = encrypter.encryptMessage(randomNumber, encrypter.generateAESKey(studentHash), encrypter.generateIV(studentHash))

        sendMessage(ChatContentModel(encryptedResponse, clientIp))
        isAuthenticated = true
    }

    fun sendMessageEncrypted(content: ChatContentModel) {
        thread {
            if (!clientSocket.isConnected) {
                throw Exception("We aren't currently connected to the server!")
            }
            // Ensure aesKey and aesIv are initialized after authentication
            if (aesKey != null && aesIv != null && isAuthenticated) {
                val encryptedContent = encrypter.encryptMessage(content.message, aesKey!!, aesIv!!)
                val encryptedChatContent = ChatContentModel(encryptedContent, clientIp)
                writer.write("${Gson().toJson(encryptedChatContent)}\n")
            } else {
                throw Exception("Encryption keys are not initialized or authentication is incomplete!")
            }
            writer.flush()
        }
    }*/

    fun close(){
        clientSocket.close()
    }

}