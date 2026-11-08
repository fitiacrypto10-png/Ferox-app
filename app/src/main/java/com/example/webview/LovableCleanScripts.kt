package com.example.webview

object LovableCleanScripts {
    val MOBILE_VIEWPORT_FIT_JS = """
        (function() {
            function enforceMobileLayout() {
                // 1. Force mobile-first viewport meta tag
                var vp = document.querySelector('meta[name="viewport"]');
                if (!vp) {
                    vp = document.createElement('meta');
                    vp.name = 'viewport';
                    (document.head || document.documentElement).appendChild(vp);
                }
                vp.setAttribute('content', 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover');

                // 2. Inject CSS rules to constrain width and adapt rooms and cards to phone screen
                var styleId = 'ferox-mobile-viewport-fit-css';
                if (!document.getElementById(styleId)) {
                    var css = [
                        'html, body {',
                        '    width: 100% !important;',
                        '    max-width: 100vw !important;',
                        '    overflow-x: hidden !important;',
                        '    margin: 0 !important;',
                        '    padding: 0 !important;',
                        '    -webkit-text-size-adjust: 100% !important;',
                        '    box-sizing: border-box !important;',
                        '}',
                        '#root, [data-reactroot], main, [role="main"] {',
                        '    width: 100% !important;',
                        '    max-width: 100vw !important;',
                        '    overflow-x: hidden !important;',
                        '    box-sizing: border-box !important;',
                        '}',
                        '*, *::before, *::after {',
                        '    box-sizing: border-box !important;',
                        '}',
                        '.container, [class*="container"], [class*="max-w-"], [class*="min-w-"] {',
                        '    max-width: 100vw !important;',
                        '    box-sizing: border-box !important;',
                        '}',
                        'section, article, nav, aside, div {',
                        '    max-width: 100vw !important;',
                        '    box-sizing: border-box !important;',
                        '}',
                        '.lk-room-container, .lk-audio-conference, [class*="lk-"] {',
                        '    width: 100% !important;',
                        '    max-width: 100vw !important;',
                        '    overflow-x: hidden !important;',
                        '    box-sizing: border-box !important;',
                        '}',
                        '[class*="bg-card"], [class*="border-border"] {',
                        '    max-width: 100% !important;',
                        '    box-sizing: border-box !important;',
                        '}',
                        'img, svg, video, canvas {',
                        '    max-width: 100% !important;',
                        '    height: auto !important;',
                        '}',
                        '[role="dialog"], [class*="dialog"] {',
                        '    max-width: 95vw !important;',
                        '    box-sizing: border-box !important;',
                        '}',
                        '/* Empêcher tout dépassement de texte, de liens longs ou de messages */',
                        'p, span, a, div, li, [class*="message"], [class*="bubble"], [class*="content"], [class*="text"] {',
                        '    word-break: break-word !important;',
                        '    overflow-wrap: anywhere !important;',
                        '    box-sizing: border-box !important;',
                        '}',
                        '/* Empêcher les éléments flex d\'exploser la largeur de l\'écran */',
                        '.flex, [class*="flex"], [class*="grid"] {',
                        '    min-width: 0 !important;',
                        '}',
                        '.flex > *, [class*="flex"] > * {',
                        '    min-width: 0 !important;',
                        '    max-width: 100% !important;',
                        '    box-sizing: border-box !important;',
                        '}',
                        '/* S\'assurer que la barre de saisie de message descend bien tout en bas sans espace vide */',
                        'footer, [class*="footer"], [class*="input-container"], [class*="input-bar"], [class*="chat-input"], form:has(input) {',
                        '    margin-bottom: 0 !important;',
                        '    bottom: 0 !important;',
                        '}',
                        '[class*="chat"], .chat-container, .chat-view, [class*="conversation"] {',
                        '    padding-bottom: 0 !important;',
                        '    margin-bottom: 0 !important;',
                        '}',
                        '/* Dégagement léger uniquement sur le dernier message pour qu\'il reste lisible */',
                        '[class*="message-list"] > *:last-child, [class*="messages-list"] > *:last-child {',
                        '    margin-bottom: 8px !important;',
                        '}'
                    ].join('\n');

                    var style = document.createElement('style');
                    style.id = styleId;
                    style.type = 'text/css';
                    style.appendChild(document.createTextNode(css));
                    (document.head || document.documentElement).appendChild(style);
                }

                // 3. Dynamic width constraint on any elements exceeding window width
                try {
                    var winW = window.innerWidth || document.documentElement.clientWidth;
                    if (winW > 0) {
                        var allEls = document.querySelectorAll('*');
                        for (var i = 0; i < allEls.length; i++) {
                            var el = allEls[i];
                            if (el.scrollWidth > winW + 2) {
                                el.style.maxWidth = '100%';
                                el.style.overflowX = 'hidden';
                                el.style.boxSizing = 'border-box';
                            }
                        }
                    }
                } catch(e) {}
            }

            enforceMobileLayout();
            try {
                var mobObserver = new MutationObserver(enforceMobileLayout);
                mobObserver.observe(document.documentElement || document.body, { childList: true, subtree: true });
            } catch(e) {}
            window.addEventListener('resize', enforceMobileLayout);
        })();
    """.trimIndent()

    val HIDE_LOVABLE_JS = """
        (function() {
            function purgeLovable() {
                var selectors = [
                    '#lovable-badge',
                    '.lovable-badge',
                    'a[href*="lovable.dev"]',
                    'a[href*="lovable.app"]',
                    '[aria-label*="lovable" i]',
                    'footer a[href*="lovable"]',
                    'div[style*="z-index: 2147483647"]',
                    'div[style*="z-index: 99999"]',
                    'div[style*="z-index: 9999"]',
                    '[class*="lovable-badge"]'
                ];
                selectors.forEach(function(sel) {
                    try {
                        var elements = document.querySelectorAll(sel);
                        for (var i = 0; i < elements.length; i++) {
                            var el = elements[i];
                            el.style.setProperty('display', 'none', 'important');
                            el.style.setProperty('visibility', 'hidden', 'important');
                            el.style.setProperty('opacity', '0', 'important');
                            el.style.setProperty('pointer-events', 'none', 'important');
                            el.style.setProperty('height', '0', 'important');
                            if (el.parentNode && el.tagName.toLowerCase() === 'a' && el.parentNode.children.length === 1 && el.parentNode !== document.body) {
                                el.parentNode.style.display = 'none';
                                el.parentNode.remove();
                            } else {
                                el.remove();
                            }
                        }
                    } catch (e) {}
                });
            }

            try {
                var css = '#lovable-badge, .lovable-badge, a[href*="lovable.dev"], a[href*="lovable.app"], div:has(> a[href*="lovable.dev"]), div:has(> a[href*="lovable.app"]), [class*="lovable-badge"] { display: none !important; visibility: hidden !important; opacity: 0 !important; pointer-events: none !important; height: 0 !important; }';
                var style = document.createElement('style');
                style.type = 'text/css';
                style.appendChild(document.createTextNode(css));
                (document.head || document.documentElement).appendChild(style);
            } catch (e) {}

            purgeLovable();
            try {
                var observer = new MutationObserver(purgeLovable);
                observer.observe(document.documentElement || document.body, { childList: true, subtree: true });
            } catch (e) {}
        })();
    """.trimIndent()

    val SETUP_WEBRTC_MIC_HOOK_JS = """
        (function() {
            if (window.__voiceHookInstalled) return;
            window.__voiceHookInstalled = true;
            window.__micMuted = false;
            window.__voiceAudioTracks = window.__voiceAudioTracks || [];
            window.__audioContexts = window.__audioContexts || [];

            // 1. Hook and auto-resume AudioContext to bypass mobile browser audio suspension
            var OrigAC = window.AudioContext || window.webkitAudioContext;
            if (OrigAC) {
                var ProxyAC = function(opts) {
                    var ctx = opts ? new OrigAC(opts) : new OrigAC();
                    window.__audioContexts.push(ctx);
                    if (ctx.state === 'suspended') {
                        ctx.resume().catch(function(){});
                    }
                    return ctx;
                };
                ProxyAC.prototype = OrigAC.prototype;
                window.AudioContext = ProxyAC;
                window.webkitAudioContext = ProxyAC;
            }

            // Function to wake up and resume any suspended audio channels
            function unlockAllAudio() {
                try {
                    if (window.__audioContexts) {
                        window.__audioContexts.forEach(function(ctx) {
                            if (ctx.state === 'suspended') {
                                ctx.resume().catch(function(){});
                            }
                        });
                    }
                    var audios = document.querySelectorAll('audio, video');
                    for (var i = 0; i < audios.length; i++) {
                        var el = audios[i];
                        el.muted = false;
                        if (el.paused && (el.srcObject || el.src)) {
                            el.play().catch(function(){});
                        }
                    }
                } catch(e) {}
            }

            // Auto-unlock on user touches / clicks
            ['touchstart', 'touchend', 'click', 'pointerdown'].forEach(function(ev) {
                document.addEventListener(ev, unlockAllAudio, { passive: true, capture: true });
            });
            setInterval(unlockAllAudio, 2500);

            // 2. Intercept getUserMedia to store audio tracks and enforce active state
            if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
                var origGUM = navigator.mediaDevices.getUserMedia.bind(navigator.mediaDevices);
                navigator.mediaDevices.getUserMedia = function(constraints) {
                    unlockAllAudio();
                    return origGUM(constraints).then(function(stream) {
                        if (stream && stream.getAudioTracks) {
                            stream.getAudioTracks().forEach(function(track) {
                                window.__voiceAudioTracks.push(track);
                                track.enabled = !window.__micMuted;
                            });
                        }
                        return stream;
                    });
                };
            }

            // 3. Function to mute or unmute all audio tracks and page buttons
            window.setMicrophoneMuted = function(isMuted) {
                window.__micMuted = isMuted;

                // Disable or enable WebRTC audio tracks directly
                if (window.__voiceAudioTracks) {
                    window.__voiceAudioTracks.forEach(function(track) {
                        try {
                            track.enabled = !isMuted;
                        } catch(e) {}
                    });
                }

                // Also find and click any web mute/unmute buttons
                try {
                    var buttons = document.querySelectorAll('button[aria-label*="mic" i], button[aria-label*="mute" i], button[data-testid*="mic"], [class*="mic"]');
                    for (var i = 0; i < buttons.length; i++) {
                        var b = buttons[i];
                        var aria = (b.getAttribute('aria-label') || b.innerText || '').toLowerCase();
                        var isMutedInWeb = aria.includes('unmute') || aria.includes('activer') || aria.includes('désactivé');
                        if (isMuted && !isMutedInWeb) {
                            b.click();
                        } else if (!isMuted && isMutedInWeb) {
                            b.click();
                        }
                    }
                } catch(e) {}
            };
        })();
    """.trimIndent()

    fun getSetMicMutedScript(muted: Boolean): String {
        return "if (window.setMicrophoneMuted) { window.setMicrophoneMuted($muted); }"
    }
}
