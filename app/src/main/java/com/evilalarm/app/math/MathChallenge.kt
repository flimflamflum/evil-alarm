package com.evilalarm.app.math

import com.evilalarm.app.data.Difficulty
import kotlin.random.Random

data class MathQuestion(val text: String, val answer: Int)

object MathChallenge {
    /** Questions needed to turn off a ringing alarm. */
    const val DISMISS_QUESTIONS = 2

    /** Questions needed to end an app block early. */
    const val END_BLOCK_QUESTIONS = 5

    fun question(difficulty: Difficulty, random: Random = Random.Default): MathQuestion = when (difficulty) {
        Difficulty.EASY -> if (random.nextBoolean()) {
            val a = random.nextInt(2, 21)
            val b = random.nextInt(2, 21)
            MathQuestion("$a + $b", a + b)
        } else {
            val a = random.nextInt(10, 31)
            val b = random.nextInt(2, a)
            MathQuestion("$a − $b", a - b)
        }

        Difficulty.MEDIUM -> when (random.nextInt(3)) {
            0 -> {
                val a = random.nextInt(12, 100)
                val b = random.nextInt(12, 100)
                MathQuestion("$a + $b", a + b)
            }
            1 -> {
                val a = random.nextInt(30, 100)
                val b = random.nextInt(11, a)
                MathQuestion("$a − $b", a - b)
            }
            else -> {
                val a = random.nextInt(3, 13)
                val b = random.nextInt(6, 16)
                MathQuestion("$a × $b", a * b)
            }
        }

        Difficulty.HARD -> if (random.nextBoolean()) {
            val a = random.nextInt(12, 40)
            val b = random.nextInt(12, 30)
            MathQuestion("$a × $b", a * b)
        } else {
            val a = random.nextInt(6, 16)
            val b = random.nextInt(6, 16)
            val c = random.nextInt(11, 100)
            MathQuestion("$a × $b + $c", a * b + c)
        }
    }
}
