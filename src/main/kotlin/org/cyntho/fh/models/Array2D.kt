package org.cyntho.fh.models

import kotlinx.serialization.Serializable

@Serializable
data class Array2D(val width: Int,
                   val height: Int,
                   val arrayContents: Array<Array<Int>>) {
    constructor(width: Int, height: Int) : this(width, height, Array(width) { Array(height) { 0 } })

    fun prettyPrint() {
        print("Size: ${height}x$width\t")
        for (x in 0 until width){
            print("[")
            for (y in 0 until height){
                if (y != height - 1){
                    print("${arrayContents[x][y]}, ")
                } else {
                    print("${arrayContents[x][y]}")
                }
            }
            print("]")
        }
        print("\n")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Array2D

        if (width != other.width) return false
        if (height != other.height) return false
        if (!arrayContents.contentDeepEquals(other.arrayContents)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + arrayContents.contentDeepHashCode()
        return result
    }
}

