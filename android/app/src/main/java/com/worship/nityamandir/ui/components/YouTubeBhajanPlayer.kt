package com.worship.nityamandir.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.worship.nityamandir.engine.BhajanPlayer
import kotlinx.coroutines.delay
import org.json.JSONObject

/** Visible, unmodified YouTube controls. No audio extraction or substitute recordings. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun YouTubeBhajanPlayer(
    player: BhajanPlayer,
    track: BhajanPlayer.Track,
    modifier: Modifier = Modifier.fillMaxWidth().height(220.dp)
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val generation = player.playbackGeneration
    val startSeconds = player.position / 1000
    val autoplay = player.youtubeAutoplay
    val webView = remember(generation) {
        WebView(context).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    if (!request.isForMainFrame) return false
                    // Links leave the embedded document; never load arbitrary pages into this player.
                    if (request.hasGesture() && request.url.scheme == "https") {
                        player.pause()
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, request.url)) }
                    }
                    return true
                }
            }
            val origin = "https://${context.packageName}"
            loadDataWithBaseURL(origin, youtubeHtml(track.youtubeId, origin, autoplay, startSeconds, player.repeat),
                "text/html", "utf-8", null)
        }
    }

    DisposableEffect(webView, lifecycle) {
        player.attachYoutube(generation) { command ->
            webView.evaluateJavascript("if(window.${command.substringBefore('(')}){$command}", null)
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    player.pause()
                    webView.onPause()
                }
                Lifecycle.Event.ON_RESUME -> webView.onResume()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            player.detachYoutube(generation)
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.destroy()
        }
    }

    LaunchedEffect(webView) {
        var polls = 0
        while (true) {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                webView.evaluateJavascript("""
                    (function() {
                      if (!window.playerReady) return {ready:false, error:window.playerError || 0};
                      return {ready:true, state:yt.getPlayerState(), seconds:yt.getCurrentTime(),
                        total:yt.getDuration(), error:window.playerError || 0};
                    })()
                """.trimIndent()) { value ->
                    runCatching {
                        val state = JSONObject(value)
                        val isReady = state.optBoolean("ready")
                        val error = state.optInt("error") != 0 || (!isReady && polls > 40)
                        if (isReady || error) player.updateYoutube(generation,
                            state.optInt("state", -1), state.optDouble("seconds", 0.0),
                            state.optDouble("total", 0.0), error)
                    }
                }
                polls++
            }
            delay(500)
        }
    }

    // The embed must remain visible, with at least 200px in both dimensions.
    AndroidView(factory = { webView }, modifier = modifier)
}

private fun youtubeHtml(videoId: String, origin: String, autoplay: Boolean, startSeconds: Long, repeating: Boolean): String {
    require(Regex("[A-Za-z0-9_-]{11}").matches(videoId))
    // Set numeric viewport dimensions after readiness: percentage/vh heights can resolve to
    // zero when WebView loads before its first Android layout, despite audio playing normally.
    return """
        <!doctype html><html><head>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta name="referrer" content="strict-origin-when-cross-origin">
        <style>
        html,body{margin:0;padding:0;background:#000;overflow:hidden}
        #player{display:block;border:0}
        </style>
        </head><body><div id="player"></div><script>
        var yt, playerReady=false, playerError=0, playbackAllowed=$autoplay, repeatEnabled=$repeating;
        function playBhajan(){playbackAllowed=true;if(playerReady)yt.playVideo();}
        function seekBhajan(seconds){if(playerReady)yt.seekTo(seconds,true);}
        function repeatBhajan(enabled){repeatEnabled=enabled;}
        function pauseBhajan(){playbackAllowed=false;if(playerReady)yt.pauseVideo();}
        function sizePlayer(){
          var w=Math.max(200,window.innerWidth), h=Math.max(200,window.innerHeight);
          document.documentElement.style.height=h+'px';
          document.body.style.height=h+'px';
          if(yt && playerReady)yt.setSize(w,h);
        }
        window.addEventListener('resize',sizePlayer);
        function onYouTubeIframeAPIReady(){
          yt=new YT.Player('player',{width:Math.max(200,window.innerWidth),height:Math.max(200,window.innerHeight),videoId:${JSONObject.quote(videoId)},
            playerVars:{playsinline:1,origin:${JSONObject.quote(origin)},start:$startSeconds,fs:0},
            events:{onReady:function(){playerReady=true;sizePlayer();if(playbackAllowed)yt.playVideo();},
              onStateChange:function(e){if(e.data===1)playerError=0;if(e.data===0 && repeatEnabled){yt.seekTo(0,true);yt.playVideo();}},
              onError:function(e){playerError=e.data;},
              onAutoplayBlocked:function(){playbackAllowed=false;}}
          });
        }
        </script><script src="https://www.youtube.com/iframe_api"></script></body></html>
    """.trimIndent()
}
