package org.cyntho.fh.models


data class LayoutWrapper(
    val id: Int,
    val sizeX: Int,
    val sizeY: Int,
    val name: String,
    val created: Long,
    val validFrom: Long,
    val active: Boolean,
    var data: MutableList<Int>?
) {
    fun fillFromArray2D(arr: Array2D){
        data = mutableListOf()
        for (x in 0 until arr.height){
            for (y in 0 until arr.width){
                data!!.add(x * arr.width + y, arr.arrayContents[y][x])
            }
        }
    }

    fun fillFromString(str: String){
        var raw = str
        data = mutableListOf()

        raw = raw.replace("[", "")
        raw = raw.replace("]", "")

        val a = raw.split(", ")
        for ((i, x) in a.withIndex()){
            data!!.add(i, x.toInt())
        }
    }


    fun asArray2D(): Array2D? {
        if (data == null) return null
        val arr: Array2D = Array2D(sizeY, sizeX)

        var counter = 0
        for (x in 0 until sizeY){
            for (y in 0 until sizeX){
                arr.arrayContents[x][y] = data!![counter++]
            }
        }

        return arr
    }
}
