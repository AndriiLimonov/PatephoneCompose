package com.andrii.patephone
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.ShuffleOrder
import java.util.Random

@UnstableApi
class BetterShuffleOrder(
    private val length: Int,
    private val currentIdx: Int,
    private val random: Random = Random()
) : ShuffleOrder {

    private val shuffledMap: IntArray = IntArray(length)
    private val indexInShuffled: IntArray = IntArray(length)

    init {
        generateShuffledOrder()
    }

    private fun generateShuffledOrder() {
        val list = (0 until length).filter { it != currentIdx }.toMutableList()
        list.shuffle(random)

        // This method generates shuffle order, placing current item always as the first.
        if (length > 0 && currentIdx in 0 until length) {
            list.add(0, currentIdx)
        }

        for (i in 0 until length) {
            shuffledMap[i] = list[i]
            indexInShuffled[shuffledMap[i]] = i
        }
    }

    // Some methods i dont use
    override fun getLength(): Int = length
    override fun getNextIndex(index: Int): Int = if (indexInShuffled[index] + 1 < length) shuffledMap[indexInShuffled[index] + 1] else androidx.media3.common.C.INDEX_UNSET
    override fun getPreviousIndex(index: Int): Int = if (indexInShuffled[index] - 1 >= 0) shuffledMap[indexInShuffled[index] - 1] else androidx.media3.common.C.INDEX_UNSET
    override fun getLastIndex(): Int = if (length > 0) shuffledMap[length - 1] else androidx.media3.common.C.INDEX_UNSET
    override fun getFirstIndex(): Int = if (length > 0) shuffledMap[0] else androidx.media3.common.C.INDEX_UNSET

    override fun cloneAndInsert(insertionIndex: Int, insertionCount: Int): ShuffleOrder {
        return BetterShuffleOrder(length + insertionCount, currentIdx, random)
    }

    override fun cloneAndRemove(indexFrom: Int, indexTo: Int): ShuffleOrder {
        return BetterShuffleOrder(length - (indexTo - indexFrom), currentIdx, random)
    }

    override fun cloneAndClear(): ShuffleOrder {
        return BetterShuffleOrder(0, 0)
    }
}