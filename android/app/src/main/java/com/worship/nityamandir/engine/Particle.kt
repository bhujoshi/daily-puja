package com.worship.nityamandir.engine

import androidx.compose.ui.graphics.Color

enum class ParticleType {
    FLOWER,
    WATER,
    SMOKE
}

data class Particle(
    var id: Long = System.nanoTime(),
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var ax: Float = 0f,
    var ay: Float = 0f,
    var type: ParticleType,
    var color: Color = Color.White,
    var size: Float = 10f,
    var rotation: Float = 0f,
    var angularVelocity: Float = 0f,
    var life: Float = 1.0f,      // from 1.0 to 0.0
    var maxLife: Float = 1.0f,
    var active: Boolean = true,
    var bounciness: Float = 0.3f, // 0 to 1
    var floorY: Float? = null     // If set, the particle bounces/stops at this Y
) {
    fun update(deltaTime: Float) {
        if (!active) return

        // Apply acceleration
        vx += ax * deltaTime
        vy += ay * deltaTime

        // Apply velocity
        x += vx * deltaTime
        y += vy * deltaTime
        
        // Apply rotation
        rotation += angularVelocity * deltaTime

        // Floor collision
        if (floorY != null && y >= floorY!!) {
            y = floorY!!
            if (vy > 10f) { // If falling fast enough, bounce
                vy = -vy * bounciness
                // Dampen horizontal velocity on bounce
                vx *= 0.8f
                angularVelocity *= 0.5f
            } else {
                // Settle
                vy = 0f
                vx = 0f
                ay = 0f // Stop gravity
                angularVelocity = 0f
            }
        }

        // Decay life
        life -= (deltaTime / maxLife)
        if (life <= 0f) {
            active = false
        }
    }
}
