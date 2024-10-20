package com.example.studentapplication

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import java.net.InetAddress
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studentapplication.peerlist.PeerListAdapter
import com.example.studentapplication.peerlist.PeerListAdapterInterface
import com.example.studentapplication.wifidirect.WifiDirectInterface
import com.example.studentapplication.wifidirect.WifiDirectManager
import com.example.studentapplication.chatlist.ChatListAdapter
import com.example.studentapplication.models.ChatContentModel
import com.example.studentapplication.network.NetworkMessageInterface
import com.example.studentapplication.network.Client
import kotlin.concurrent.thread

class CommunicationActivity : AppCompatActivity(), WifiDirectInterface, PeerListAdapterInterface, NetworkMessageInterface {
    private var wfdManager: WifiDirectManager? = null
    private var peerListAdapter:PeerListAdapter? = null
    private var chatListAdapter:ChatListAdapter? = null
    private var client: Client? = null

    private var wfdAdapterEnabled = false
    private var wfdHasConnection = false
    private var hasDevices = false
    private var thisDevice : WifiP2pDevice? = null
    private var deviceIp : String = ""
    private var studentID : String = ""
    private var goIp : String = ""

    private lateinit var etEnterStudentID: EditText
    private lateinit var btnSearchForClass: View
    private lateinit var chatView : RecyclerView

    private val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_communication)
        // Initialize the input field and button
        etEnterStudentID = findViewById(R.id.etEnterStudentID)
        btnSearchForClass = findViewById(R.id.btnSearchForClassBtn)

        btnSearchForClass.setOnClickListener { view ->
            searchForClasses(view)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val manager: WifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        val channel = manager.initialize(this, mainLooper, null)
        wfdManager = WifiDirectManager(manager, channel, this)

        peerListAdapter = PeerListAdapter(this)
        val peerList: RecyclerView= findViewById(R.id.rvPeerListing)
        peerList.adapter = peerListAdapter
        peerList.layoutManager = LinearLayoutManager(this)

        chatView = findViewById(R.id.rvChat)
        chatListAdapter = ChatListAdapter()
        chatView.adapter = chatListAdapter
        chatView.layoutManager = LinearLayoutManager(this)
    }

    // Search for classes only if student ID is valid
    private fun searchForClasses(view: View) {
        studentID = findViewById<EditText>(R.id.etEnterStudentID).text.toString()
        if (isValidStudentID()) {
            // Valid ID, proceed to discover peers
            discoverNearbyPeers(view)
            updateUI()
        } else {
            Toast.makeText(this, "Please enter a valid Student ID", Toast.LENGTH_SHORT).show()
            updateUI()
        }
    }

    private fun isValidStudentID(): Boolean {
        studentID = findViewById<EditText>(R.id.etEnterStudentID).text.toString()
        return studentID.length == 9
    }

    override fun onResume() {
        super.onResume()
        wfdManager?.also {
            registerReceiver(it, intentFilter)
        }
    }

    override fun onPause() {
        super.onPause()
        wfdManager?.also {
            unregisterReceiver(it)
        }
    }

    fun discoverNearbyPeers(view: View) {
        wfdManager?.discoverPeers()
    }

    override fun onWiFiDirectStateChanged(isEnabled: Boolean) {
        wfdAdapterEnabled = isEnabled
        var text = "There was a state change in the WiFi Direct. Currently it is "
        text = if (isEnabled){
            "$text enabled!"
        } else {
            "$text disabled! Try turning on the WiFi adapter"
        }

        val toast = Toast.makeText(this, text, Toast.LENGTH_SHORT)
        toast.show()
        updateUI()
    }

    override fun onPeerListUpdated(deviceList: Collection<WifiP2pDevice>) {
        val toast = Toast.makeText(this, "Updated listing of nearby WiFi Direct devices", Toast.LENGTH_SHORT)
        toast.show()
        hasDevices = deviceList.isNotEmpty()
        peerListAdapter?.updateList(deviceList)
        updateUI()
    }

    override fun onGroupStatusChanged(groupInfo: WifiP2pGroup?, wifiP2pInfo: WifiP2pInfo) {
        val text : String

        if (groupInfo == null){
            text = "Group is not formed"
        } else {
            text = "Group has been formed"
            val className = groupInfo.networkName
            findViewById<TextView>(R.id.tvClassName).text = className
        }

        var toast = Toast.makeText(this, text , Toast.LENGTH_SHORT)
        toast.show()
        wfdHasConnection = (groupInfo != null)
        updateUI()

        if (groupInfo == null){
            client?.close()
            client = null
        } else if (!groupInfo.isGroupOwner && (client == null)) {
            thread {
                client = Client(this)
                client!!.studentID = studentID
                deviceIp = client!!.clientIp
                goIp = client!!.goIp
                //client!!.sendInitialMessage(deviceIp)
            }
        }
    }

    override fun onDeviceStatusChanged(thisDevice: WifiP2pDevice) {
        val toast = Toast.makeText(this, "Device parameters have been updated" , Toast.LENGTH_SHORT)
        toast.show()
    }

    override fun onPeerClicked(peer: WifiP2pDevice) {
        wfdManager?.connectToPeer(peer)
    }

    private fun updateUI() {
        val wfdAdapterErrorView:ConstraintLayout = findViewById(R.id.clWfdAdapterDisabled)
        wfdAdapterErrorView.visibility = if (!wfdAdapterEnabled) View.VISIBLE else View.GONE

        val wfdNoConnectionView:ConstraintLayout = findViewById(R.id.clNoWifiDirectConnection)
        wfdNoConnectionView.visibility = if (wfdAdapterEnabled && !wfdHasConnection) View.VISIBLE else View.GONE

        val nearbyClasses: TextView = findViewById(R.id.tvNearbyClasses)
        nearbyClasses.visibility = if (wfdAdapterEnabled && !wfdHasConnection && isValidStudentID()) View.VISIBLE else View.GONE

        val noPeersMessage: TextView = findViewById(R.id.tvNoPeersMessage)
        noPeersMessage.visibility = if (wfdAdapterEnabled && !wfdHasConnection && !hasDevices && isValidStudentID()) View.VISIBLE else View.GONE

        val peerList: RecyclerView = findViewById(R.id.rvPeerListing)
        peerList.visibility = if (wfdAdapterEnabled && !wfdHasConnection && hasDevices && isValidStudentID()) View.VISIBLE else View.GONE

        val wfdConnectedView:ConstraintLayout = findViewById(R.id.clHasConnection)
        wfdConnectedView.visibility = if(wfdHasConnection)View.VISIBLE else View.GONE
    }

    fun sendMessage(view: View) {
        val etMessage:EditText = findViewById(R.id.etMessage)
        val etString = etMessage.text.toString()
        val content = ChatContentModel(etString, deviceIp)
        etMessage.text.clear()
        chatListAdapter?.addItemToEnd(content)
        client?.sendMessage(content)
    }

    override fun onContent(content: ChatContentModel) {
        runOnUiThread{
                chatListAdapter?.addItemToEnd(content)
        }
    }

    fun disconnect(view: View){
     wfdManager?.disconnect()
    }

    fun goToSettings(view: View) {
        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
        startActivity(intent)
    }
}