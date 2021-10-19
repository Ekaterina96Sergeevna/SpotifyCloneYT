package com.plcoding.spotifycloneyt.other

// обертка для того чтобы создавался 1 объект и его состояние сохранялось после уничтожения, например, состояния активити
open class Event<out T> (private val data: T) {

    var hasBeenHandled = false
    private set

    fun getContentIfNotHandled(): T? {
        return if(hasBeenHandled){
            null
        } else {
            hasBeenHandled = true
            data
        }
    }

    fun peekContent() = data
}