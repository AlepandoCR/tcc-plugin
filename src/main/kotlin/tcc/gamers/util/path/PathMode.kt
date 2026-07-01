package tcc.gamers.util.path

enum class PathMode {
    CLOSEST_TO_FURTHEST,  // walk insertion order: first -> last
    FURTHEST_TO_CLOSEST   // walk insertion order reversed: last -> first
}