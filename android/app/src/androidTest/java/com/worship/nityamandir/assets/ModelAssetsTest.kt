package com.worship.nityamandir.assets

import android.media.MediaPlayer
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.worship.nityamandir.R
import com.worship.nityamandir.ui.components.AssetImageCache
import com.worship.nityamandir.ui.components.GlbAartiModelCache
import io.github.sceneview.SceneView
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.ModelNode
import io.github.sceneview.utils.destroy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Exercise the actual Android glTF/texture loader for every shipped model. */
@RunWith(AndroidJUnit4::class)
class ModelAssetsTest {
    @Test fun everyBundledModelLoadsWithFiniteBounds() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        fun models(directory:String):List<String> = context.assets.list(directory).orEmpty().flatMap {name ->
            val path="$directory/$name"
            if(name.endsWith(".glb")) listOf(path) else if(!name.contains('.')) models(path) else emptyList()
        }
        val paths=models("shrine")
        assertEquals("Audit all shipped runtime 3D Aarti models",2,paths.size)
        instrumentation.runOnMainSync {
            val egl=SceneView.createEglContext()
            val engine=SceneView.createEngine(egl)
            try {
                for(path in paths) {
                    val loader=ModelLoader(engine,context)
                    try {
                        val node=ModelNode(loader.createModelInstance(path),scaleToUnits=1f)
                        try {
                            val bounds=node.halfExtent
                            assertTrue("Invalid bounds: $path",listOf(bounds.x,bounds.y,bounds.z).all {it.isFinite() && it>=0f})
                            assertTrue("Empty geometry: $path",bounds.x+bounds.y+bounds.z>0f)
                        } finally {node.destroy()}
                    } finally {loader.destroy()}
                }
            } finally {
                engine.destroy()
                egl.destroy()
            }
        }
    }

    @Test fun optimizedAartiMeshesWebpImagesAndOggAudioDecodeCleanly() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // 1. Verify GlbAartiModelCache loads real baked COLOR_0 meshes (not fallback)
        val golden = GlbAartiModelCache.getOrLoad(context, "original")
        assertEquals(7419, golden.vertexCount)
        assertEquals(8000, golden.triangleCount)
        assertTrue("golden COLOR_0 must have varied shading", golden.baseRgb.toSet().size > 20)

        val traditional = GlbAartiModelCache.getOrLoad(context, "traditional")
        assertEquals(8655, traditional.vertexCount)
        assertEquals(7999, traditional.triangleCount)
        assertTrue("traditional COLOR_0 must have varied shading", traditional.baseRgb.toSet().size > 20)

        val karpura = GlbAartiModelCache.getOrLoad(context, "karpura")
        assertTrue(karpura.vertexCount > golden.vertexCount)
        assertTrue(karpura.triangleCount > golden.triangleCount)

        // 2. Verify every bundled WebP shrine asset decodes cleanly via AssetImageCache
        fun webps(directory: String): List<String> = context.assets.list(directory).orEmpty().flatMap { name ->
            val path = "$directory/$name"
            if (name.endsWith(".webp")) listOf(path) else if (!name.contains('.')) webps(path) else emptyList()
        }
        val images = webps("shrine")
        assertTrue("Expected bundled WebP shrine assets, found ${images.size}", images.size >= 50)
        for (path in images) {
            val bmp = AssetImageCache.bitmap(context, path)
            assertTrue("Empty bitmap for $path", bmp.width > 0 && bmp.height > 0)
        }

        // 3. Verify resized WebP launcher artwork drawable resolves cleanly
        val launcher = ContextCompat.getDrawable(context, R.drawable.launcher_artwork)
        assertNotNull("launcher_artwork drawable must resolve", launcher)

        // 4. Verify all 6 compressed OGG raw audio cues initialize in MediaPlayer
        val rawIds = listOf(
            R.raw.bell, R.raw.conch, R.raw.flower_offering,
            R.raw.water_offering, R.raw.mindfulness_flute, R.raw.mindfulness_note
        )
        for (id in rawIds) {
            val mp = MediaPlayer.create(context, id)
            assertNotNull("Failed to load raw audio resource $id", mp)
            try {
                assertTrue("Audio duration must be > 0 for $id", mp.duration > 0)
            } finally {
                mp.release()
            }
        }
    }
}
