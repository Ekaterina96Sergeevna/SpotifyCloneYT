package com.plcoding.spotifycloneyt.exoplayer

import com.plcoding.spotifycloneyt.exoplayer.State.*

// fetch songs from firestore database (we will later have a list of songs that we got from firebase)
// to get these songs that takes a little bit of time
// but int our service we don't have the possibility wait until that is finished -> we created state variable
class FirebaseMusicSource {

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