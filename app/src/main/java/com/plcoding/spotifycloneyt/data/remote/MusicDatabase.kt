package com.plcoding.spotifycloneyt.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.plcoding.spotifycloneyt.data.entities.Song
import com.plcoding.spotifycloneyt.other.Constants.SONG_COLLECTION
import kotlinx.coroutines.tasks.await

class MusicDatabase {

    private val firestore = FirebaseFirestore.getInstance()
    private val songCollection = firestore.collection(SONG_COLLECTION)

    // suspend - we can use coroutines in that function
    // asynchronous option
    suspend fun getAllSongs(): List<Song> {
        return try {
            // await() - we can use it, because fun is suspend (for BG processing)
            songCollection.get().await().toObjects(Song::class.java)
        } catch (e : Exception) {
            emptyList()
        }
    }
}