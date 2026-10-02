package com.worship.nityamandir.assets

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.sceneview.SceneView
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.ModelNode
import io.github.sceneview.utils.destroy
import org.junit.Assert.assertEquals
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
        assertEquals("Audit all shipped models, including procedural and legacy props",17,paths.size)
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
}
