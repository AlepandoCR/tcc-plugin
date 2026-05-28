package tcc.gamers.util

interface Builder<T, A: Any> {
    fun build(arg: A):T
}