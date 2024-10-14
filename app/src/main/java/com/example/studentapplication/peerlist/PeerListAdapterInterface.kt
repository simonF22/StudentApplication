package com.example.studentapplication.peerlist

import android.net.wifi.p2p.WifiP2pDevice

interface PeerListAdapterInterface {
    fun onPeerClicked(peer: WifiP2pDevice)
}