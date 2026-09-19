/**
 * Pavitra Mandir (पवित्र मंदिर) - Interactive Web Application Logic
 */

document.addEventListener('DOMContentLoaded', () => {
  // Web Audio Context setup
  let audioCtx = null;

  function getAudioContext() {
    if (!audioCtx) {
      const AudioContext = window.AudioContext || window.webkitAudioContext;
      audioCtx = new AudioContext();
    }
    if (audioCtx.state === 'suspended') {
      audioCtx.resume();
    }
    return audioCtx;
  }

  // 1. Synthesize Brass Temple Bell (घंटी वादन)
  function playBrassBell() {
    const ctx = getAudioContext();
    const now = ctx.currentTime;

    // Harmonic frequencies for brass bell resonance
    const freqs = [528, 1056, 1584, 2112, 2640];
    const gains = [0.6, 0.3, 0.15, 0.08, 0.04];

    freqs.forEach((freq, idx) => {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();

      osc.type = 'sine';
      osc.frequency.setValueAtTime(freq, now);
      // Slight pitch wobble for metallic charm
      osc.frequency.exponentialRampToValueAtTime(freq * 0.995, now + 1.5);

      gain.gain.setValueAtTime(gains[idx], now);
      gain.gain.exponentialRampToValueAtTime(0.0001, now + 2.5);

      osc.connect(gain);
      gain.connect(ctx.destination);

      osc.start(now);
      osc.stop(now + 2.5);
    });

    createVisualRipple('rgba(255, 215, 0, 0.8)');
  }

  // 2. Synthesize Sacred Shankh Naad (432 Hz Conch Call)
  function playShankhNaad() {
    const ctx = getAudioContext();
    const now = ctx.currentTime;

    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    const filter = ctx.createBiquadFilter();

    // Sacred 432 Hz fundamental frequency
    osc.type = 'sawtooth';
    osc.frequency.setValueAtTime(432, now);
    osc.frequency.linearRampToValueAtTime(440, now + 1.2);
    osc.frequency.linearRampToValueAtTime(432, now + 3.0);

    // Warm low-pass filter
    filter.type = 'lowpass';
    filter.frequency.setValueAtTime(800, now);
    filter.Q.setValueAtTime(3, now);

    // Envelope
    gain.gain.setValueAtTime(0.01, now);
    gain.gain.linearRampToValueAtTime(0.7, now + 0.8);
    gain.gain.exponentialRampToValueAtTime(0.0001, now + 3.5);

    osc.connect(filter);
    filter.connect(gain);
    gain.connect(ctx.destination);

    osc.start(now);
    osc.stop(now + 3.5);

    createVisualRipple('rgba(255, 140, 0, 0.9)');
  }

  // Visual Sonic Ripple Effect on Canvas
  const canvas = document.getElementById('sanctumCanvas');
  const ctxCanvas = canvas ? canvas.getContext('2d') : null;
  let ripples = [];

  function resizeCanvas() {
    if (!canvas) return;
    const rect = canvas.parentElement.getBoundingClientRect();
    canvas.width = rect.width;
    canvas.height = rect.height;
  }

  window.addEventListener('resize', resizeCanvas);
  resizeCanvas();

  function createVisualRipple(color) {
    if (!canvas) return;
    ripples.push({
      x: canvas.width / 2,
      y: canvas.height / 2 + 30,
      radius: 10,
      maxRadius: Math.max(canvas.width, canvas.height) * 0.7,
      alpha: 1,
      color: color
    });
  }

  function animateRipples() {
    if (ctxCanvas && canvas) {
      ctxCanvas.clearRect(0, 0, canvas.width, canvas.height);

      for (let i = ripples.length - 1; i >= 0; i--) {
        const r = ripples[i];
        ctxCanvas.beginPath();
        ctxCanvas.arc(r.x, r.y, r.radius, 0, Math.PI * 2);
        ctxCanvas.strokeStyle = r.color;
        ctxCanvas.lineWidth = 3;
        ctxCanvas.globalAlpha = r.alpha;
        ctxCanvas.stroke();

        r.radius += 4;
        r.alpha -= 0.02;

        if (r.alpha <= 0 || r.radius >= r.maxRadius) {
          ripples.splice(i, 1);
        }
      }
    }
    requestAnimationFrame(animateRipples);
  }

  animateRipples();

  // 3. Interactive Diya Toggle
  const diyaBtn = document.getElementById('btnLightDiya');
  const diyaGlow = document.getElementById('diyaGlow');
  let isDiyaLit = false;

  if (diyaBtn) {
    diyaBtn.addEventListener('click', () => {
      isDiyaLit = !isDiyaLit;
      if (diyaGlow) {
        diyaGlow.classList.toggle('active', isDiyaLit);
      }
      diyaBtn.classList.toggle('active', isDiyaLit);
      if (isDiyaLit) {
        playBrassBell();
      }
    });
  }

  // 4. Interactive Bell & Shankh Buttons
  const bellBtn = document.getElementById('btnRingBell');
  if (bellBtn) {
    bellBtn.addEventListener('click', () => {
      playBrassBell();
      bellBtn.classList.add('active');
      setTimeout(() => bellBtn.classList.remove('active'), 600);
    });
  }

  const shankhBtn = document.getElementById('btnBlowShankh');
  if (shankhBtn) {
    shankhBtn.addEventListener('click', () => {
      playShankhNaad();
      shankhBtn.classList.add('active');
      setTimeout(() => shankhBtn.classList.remove('active'), 1000);
    });
  }

  // 5. Offer Flowers (Pushparpan)
  const flowerBtn = document.getElementById('btnOfferFlower');
  if (flowerBtn && canvas && ctxCanvas) {
    flowerBtn.addEventListener('click', () => {
      // Spawn floating petal particles
      for (let i = 0; i < 15; i++) {
        const x = canvas.width / 2 + (Math.random() * 200 - 100);
        const y = Math.random() * 50;
        ripples.push({
          x: x,
          y: y,
          radius: Math.random() * 6 + 4,
          maxRadius: canvas.height * 0.75,
          alpha: 1,
          color: Math.random() > 0.5 ? 'rgba(255, 112, 0, 0.9)' : 'rgba(240, 97, 146, 0.9)'
        });
      }
      flowerBtn.classList.add('active');
      setTimeout(() => flowerBtn.classList.remove('active'), 500);
    });
  }

  // 6. Temporal Aging Engine Simulator Slider
  const agingSlider = document.getElementById('agingSlider');
  const agingTimeDisplay = document.getElementById('agingTimeDisplay');
  const dustLevelDisplay = document.getElementById('dustLevelDisplay');
  const flowerStatusDisplay = document.getElementById('flowerStatusDisplay');
  const agingStatusDot = document.getElementById('agingStatusDot');
  const templeStateImg = document.getElementById('templeStateImg');

  if (agingSlider) {
    agingSlider.addEventListener('input', (e) => {
      const hours = parseInt(e.target.value, 10);
      agingTimeDisplay.textContent = `${hours} Hours`;

      // Dust calculation: min(1.0, (hours - 14)/72)
      let dust = 0;
      if (hours > 14) {
        dust = Math.min(1.0, (hours - 14) / 72);
      }
      dustLevelDisplay.textContent = `${Math.round(dust * 100)}%`;

      // Flower calculation
      let flowerText = 'Fresh & Fragrant (ताज़े पुष्प)';
      if (hours > 12 && hours <= 24) {
        flowerText = 'Fading / Nirmalya (कुम्हलाए पुष्प)';
      } else if (hours > 24) {
        flowerText = 'Wilted / Dried (निर्माल्य विसर्जन आवश्यक)';
      }
      flowerStatusDisplay.textContent = flowerText;

      // Update State Image & Dots
      if (hours <= 12) {
        if (templeStateImg) templeStateImg.src = 'assets/mandir-aarti-facing-idols.png';
        if (agingStatusDot) {
          agingStatusDot.style.background = '#4CAF50';
          agingStatusDot.style.boxShadow = '0 0 10px #4CAF50';
        }
      } else if (hours <= 24) {
        if (templeStateImg) templeStateImg.src = 'assets/mandir-aarti-complete.png';
        if (agingStatusDot) {
          agingStatusDot.style.background = '#FF9800';
          agingStatusDot.style.boxShadow = '0 0 10px #FF9800';
        }
      } else {
        if (templeStateImg) templeStateImg.src = 'assets/mandir-glass-lamp.png';
        if (agingStatusDot) {
          agingStatusDot.style.background = '#F44336';
          agingStatusDot.style.boxShadow = '0 0 10px #F44336';
        }
      }
    });
  }

  // 7. Interactive Aarti Lyrics Player Demo
  const lyricsLines = [
    { time: 0, text: "जय गणेश जय गणेश, जय गणेश देवा ।" },
    { time: 3, text: "माता जाकी पार्वती, पिता महादेवा ॥" },
    { time: 7, text: "एक दन्त दयावन्त, चार भुजाधारी ।" },
    { time: 11, text: "माथे सिन्दूर सोहे, मूसे की सवारी ॥" },
    { time: 15, text: "पान चढ़े फूल चढ़े, और चढ़े मेवा ।" },
    { time: 19, text: "लड्डुअन का भोग लगे, सन्त करे सेवा ॥" },
    { time: 23, text: "जय गणेश जय गणेश, जय गणेश देवा ।" }
  ];

  const lyricsContainer = document.getElementById('lyricsContainer');
  const playAartiBtn = document.getElementById('btnPlayAarti');
  let aartiInterval = null;
  let currentStep = 0;
  let isAartiPlaying = false;

  if (lyricsContainer) {
    // Render initial lines
    lyricsContainer.innerHTML = lyricsLines.map((line, idx) => 
      `<div class="lyric-line ${idx === 0 ? 'active' : ''}" id="lyric-${idx}">${line.text}</div>`
    ).join('');
  }

  if (playAartiBtn) {
    playAartiBtn.addEventListener('click', () => {
      isAartiPlaying = !isAartiPlaying;
      if (isAartiPlaying) {
        playAartiBtn.innerHTML = '<span>⏸ Pause Aarti Stuti</span>';
        playBrassBell();
        aartiInterval = setInterval(() => {
          currentStep = (currentStep + 1) % lyricsLines.length;
          document.querySelectorAll('.lyric-line').forEach((el, idx) => {
            el.classList.toggle('active', idx === currentStep);
            if (idx === currentStep) {
              el.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }
          });
          playBrassBell();
        }, 3000);
      } else {
        playAartiBtn.innerHTML = '<span>▶ Play Aarti Stuti (जय गणेश देवा)</span>';
        clearInterval(aartiInterval);
      }
    });
  }
});
