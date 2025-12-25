package hu.speeder.huroutes.web

import android.graphics.Bitmap
import android.webkit.WebView
import org.json.JSONObject

class PhotospotsWebViewClient: CarappsWebViewClient() {
    override fun onPageStarted(
        view: WebView?,
        url: String?,
        favicon: Bitmap?,
    ) {
        super.onPageStarted(view, url, favicon)

        // CSS to hide the top bar and work around the clickability of the position marker
        val css = """
            #map-canvas > div:has(div[data-tooltip*="Autós"]) {
                display: none !important;
            }
            div[title='Current location'] {
                pointer-events: none;
            }
        """.trimIndent()
        val escapedCss = JSONObject.quote(css)
        val injectCssJs = """
            (function() {
                // Inject CSS into the page
                function applyCss() {
                    var style = document.createElement('style');
                    (document.head || document.documentElement).appendChild(style);
                    style.innerHTML = $escapedCss;
                    console.log("applied CSS");
                }

                if (document.readyState === 'loading') {
                    document.addEventListener('DOMContentLoaded', applyCss, { once: true });
                } else {
                    applyCss();
                }

                // Hook maps to inject the geolocation marker
                const observer = new MutationObserver(() => {
                  if (typeof google === 'undefined' || !google.maps || !google.maps.Map)
                      return;
                  observer.disconnect();
                
                  const maps = [];
                
                  // Hook the maps contructor to capture maps objects
                  const originalMap = google.maps.Map;
                  google.maps.Map = function(el, opts) {
                      const instance = new originalMap(el, opts);
                      maps.push(instance);
                      return instance;
                  };
                  google.maps.Map.prototype = originalMap.prototype;
                
                  // Expose captured maps objects
                  window.__getMaps = () => maps.slice();
                
                  // Load the geolocation marker script
                  const script = document.createElement('script');
                  script.src = 'https://cdn.jsdelivr.net/npm/geolocation-marker@2.0.5/geolocation-marker.min.js';
                  document.head.appendChild(script);
                });
                observer.observe(document, { childList: true, subtree: true });
                addEventListener('load', () => {
                    var map = window.__getMaps()[0];
                    window.geoMarker = new GeolocationMarker(map, {zIndex: 998}, {zIndex: 999}, {zIndex: 997});
                })
            })();
        """.trimIndent()

        view?.evaluateJavascript(injectCssJs, null)
    }
}