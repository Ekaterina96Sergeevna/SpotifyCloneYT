package com.plcoding.spotifycloneyt.exoplayer

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.*
import androidx.core.net.toUri
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.plcoding.spotifycloneyt.data.remote.MusicDatabase
import com.plcoding.spotifycloneyt.exoplayer.State.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

// fetch songs from firestore database (we will later have a list of songs that we got from firebase)
// to get these songs that takes a little bit of time
// but int our service we don't have the possibility wait until that is finished -> we created state variable
class FirebaseMusicSource @Inject constructor(
    // we need access to MusicDatabase
    private val musicDatabase: MusicDatabase
){

    // list contains meta data compat objects
    // such an object contains meta information about a specific song
    var songs = emptyList<MediaMetadataCompat>()
    // we need access to that songs list from service

    // fun that gets all of our song objects from firebase
    // we switch the currently running thread - which is optimized for io operations (network/database operations)
    suspend fun fetchMediaData() = withContext(Dispatchers.IO){
        // we want to get all the songs from firebase
        // but we also want to change the state to initializing
        state = STATE_INITIALIZING
        val allSongs = musicDatabase.getAllSongs()
        // we need meta data
        songs = allSongs.map{ song ->
            MediaMetadataCompat.Builder()
                .putString(METADATA_KEY_ARTIST, song.subtitle)
                .putString(METADATA_KEY_MEDIA_ID, song.mediaId)
                .putString(METADATA_KEY_TITLE, song.title)
                .putString(METADATA_KEY_DISPLAY_TITLE, song.title)
                .putString(METADATA_KEY_DISPLAY_ICON_URI, song.imageUrl)
                .putString(METADATA_KEY_MEDIA_URI, song.songUrl)
                .putString(METADATA_KEY_ALBUM_ART_URI, song.imageUrl)
                .putString(METADATA_KEY_DISPLAY_SUBTITLE, song.subtitle)
                .putString(METADATA_KEY_DISPLAY_DESCRIPTION, song.subtitle)
                .build()
        }
        state = STATE_INITIALIZED
    }

    // convert list songs into such a media source object
    // ConcatenatingMediaSource contains the information for exoplayer from where it can stream that song
    fun asMediaSource(dataSourceFactory: DefaultDataSourceFactory) : ConcatenatingMediaSource {
        val concatenatingMediaSource = ConcatenatingMediaSource()
        songs.forEach { song ->
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(song.getString(METADATA_KEY_MEDIA_URI).toUri())
            concatenatingMediaSource.addMediaSource(mediaSource)
        }
        return concatenatingMediaSource
    }

    // we need as a list of media items in our music service
    // we create fun here to convert that
    fun asMediaItems() = songs.map { song ->
        val desc = MediaDescriptionCompat.Builder()
            .setMediaUri(song.getString(METADATA_KEY_MEDIA_URI).toUri())
            .setTitle(song.description.title)
            .setSubtitle(song.description.subtitle)
            .setMediaId(song.description.mediaId)
            .setIconUri(song.description.iconUri)
            .build()
        MediaBrowserCompat.MediaItem(desc, FLAG_PLAYABLE)
        // FLAG_PLAYABLE - только воспроизводится song
    }.toMutableList()

    // need check when the source or a music source - all of our songs finished downloading
    // in our service we often need an immediate result
    private val onReadyListener = mutableListOf<(Boolean) -> Unit>()

    // create an instance, of such a state object to check the state our music is currently in
    private var state: State = STATE_CREATED
        // when we start to initialize our music source
        // so before we download our songs we will set it to state initializing
        // after we downloaded those we set it to state initialized
        // for the state variable we define a setter
        set(value) {
            // if we set it to initialized or error
            if (value == STATE_INITIALIZED || value == STATE_ERROR) {
                synchronized(onReadyListener) {
                    //field - current value of the state
                    field = value
                    onReadyListener.forEach { listener ->
                        listener(state == STATE_INITIALIZED)
                    }
                }
            } else {
                field = value
            }
        }

    fun whenReady(action: (Boolean) -> Unit): Boolean {
        if (state == STATE_CREATED || state == STATE_INITIALIZING) {
            onReadyListener += action
            return false // our music source is not ready
        } else {
            action(state == STATE_INITIALIZED)
            return true // music source is ready
        }
    }
}

// class contains different state variables
enum class State {
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
}