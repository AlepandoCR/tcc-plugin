package tcc.gamers.util

interface Manager <T: Any> {
    val list: MutableList<T>

    fun add(thing: T){
        list.add(thing)
    }

    fun remove(thing: T){
        list.remove(thing)
    }

    fun contains(thing: T): Boolean{
        return list.contains(thing)
    }
}