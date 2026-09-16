package com.worship.nityamandir.engine

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

class PhysicsEngine {
    val particles = mutableStateListOf<Particle>()
    var tick by mutableIntStateOf(0)
        private set

    fun update(deltaTimeSeconds: Float) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val particle = iterator.next()
            particle.update(deltaTimeSeconds)
            if (!particle.active) {
                iterator.remove()
            }
        }
        tick++
    }

    fun emitFlower(x: Float, y: Float, targetFloorY: Float) {
        particles.add(
            Particle(
                x = x + Random.nextFloat() * 40f - 20f,
                y = y,
                vx = Random.nextFloat() * 100f - 50f, // Slight random horizontal velocity
                vy = Random.nextFloat() * -50f, // slight upward toss initially
                ax = 0f,
                ay = 980f, // Gravity (pixels/sec^2)
                type = ParticleType.FLOWER,
                color = listOf(Color(0xFFFF9800), Color(0xFFFFC107), Color(0xFFF44336)).random(),
                size = 30f + Random.nextFloat() * 20f,
                rotation = Random.nextFloat() * 360f,
                angularVelocity = Random.nextFloat() * 360f - 180f,
                maxLife = 5f,
                life = 5f,
                bounciness = 0.2f,
                floorY = targetFloorY + Random.nextFloat() * 20f - 10f
            )
        )
    }

    fun emitWaterStream(x: Float, y: Float, targetFloorY: Float) {
        // Emit multiple droplets per call to simulate a stream
        for (i in 0..3) {
            particles.add(
                Particle(
                    x = x + Random.nextFloat() * 10f - 5f,
                    y = y + Random.nextFloat() * 10f,
                    vx = Random.nextFloat() * 40f - 20f,
                    vy = 100f + Random.nextFloat() * 50f, // Initial downward velocity
                    ax = 0f,
                    ay = 1200f, // Heavy gravity for water
                    type = ParticleType.WATER,
                    color = Color(0xAA00BCD4), // Semi-transparent blue
                    size = 8f + Random.nextFloat() * 8f,
                    maxLife = 2f,
                    life = 2f,
                    bounciness = 0.4f,
                    floorY = targetFloorY + Random.nextFloat() * 10f
                )
            )
        }
    }

    fun emitSmoke(x: Float, y: Float) {
        particles.add(
            Particle(
                x = x + Random.nextFloat() * 20f - 10f,
                y = y,
                vx = Random.nextFloat() * 30f - 15f,
                vy = -100f - Random.nextFloat() * 50f, // Moving up
                ax = Random.nextFloat() * 20f - 10f, // Wind drift
                ay = -50f, // Upward acceleration
                type = ParticleType.SMOKE,
                color = Color(0x66FFFFFF), // Very faint white/grey
                size = 40f + Random.nextFloat() * 40f,
                rotation = Random.nextFloat() * 360f,
                angularVelocity = Random.nextFloat() * 60f - 30f,
                maxLife = 3f,
                life = 3f,
                floorY = null // Smoke doesn't hit the floor
            )
        )
    }
    
    fun clear() {
        particles.clear()
    }
}
