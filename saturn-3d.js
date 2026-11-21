// =============================================================================
// JUPITER / SATURN 3D WEBGL ENGINE (THREE.JS)
// Photorealistic 3D Planet with Rotating Rings, Authentic Shadows & Starfield
// =============================================================================

(function() {
    'use strict';

    const canvas = document.getElementById('saturnCanvas');
    if (!canvas || typeof THREE === 'undefined') return;

    // 1. Scene, Camera & WebGL Renderer
    const scene = new THREE.Scene();

    const camera = new THREE.PerspectiveCamera(
        38,
        window.innerWidth / window.innerHeight,
        0.1,
        1000
    );
    camera.position.set(0, 0, 19.5);

    const renderer = new THREE.WebGLRenderer({
        canvas: canvas,
        antialias: true,
        alpha: true,
        powerPreference: 'high-performance'
    });
    renderer.setSize(window.innerWidth, window.innerHeight);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.shadowMap.enabled = true;
    renderer.shadowMap.type = THREE.PCFSoftShadowMap;
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.1;

    // 2. Procedural Fallback Canvas Textures (Guarantee 100% offline & file:// compatibility)
    function generatePlanetCanvasTexture() {
        const w = 1024, h = 512;
        const c = document.createElement('canvas');
        c.width = w;
        c.height = h;
        const ctx = c.getContext('2d');

        // Color bands matching user's uploaded planet
        const grad = ctx.createLinearGradient(0, 0, 0, h);
        grad.addColorStop(0.00, '#888480'); // North pole
        grad.addColorStop(0.12, '#a59e94');
        grad.addColorStop(0.24, '#b8ad95');
        grad.addColorStop(0.38, '#c8bc9d');
        grad.addColorStop(0.50, '#d2c8a7'); // Equator
        grad.addColorStop(0.62, '#c4b595');
        grad.addColorStop(0.74, '#ab9b7c');
        grad.addColorStop(0.88, '#70644a');
        grad.addColorStop(1.00, '#221e17'); // South pole

        ctx.fillStyle = grad;
        ctx.fillRect(0, 0, w, h);

        // Add fine cloud bands and atmospheric turbulence
        for (let y = 0; y < h; y += 2) {
            const normY = y / h;
            const wave = Math.sin(normY * 42.0) * 8;
            const alpha = 0.04 + 0.03 * Math.cos(normY * 18.0);
            ctx.fillStyle = (wave > 0) ? `rgba(255, 248, 230, ${alpha})` : `rgba(60, 48, 30, ${alpha})`;
            ctx.fillRect(0, y, w, 2);
        }

        const texture = new THREE.CanvasTexture(c);
        texture.generateMipmaps = true;
        texture.minFilter = THREE.LinearMipmapLinearFilter;
        return texture;
    }

    function generateRingCanvasTexture() {
        const w = 1024, h = 64;
        const c = document.createElement('canvas');
        c.width = w;
        c.height = h;
        const ctx = c.getContext('2d');

        // Concentric radial ring bands matching user's reference image
        const grad = ctx.createLinearGradient(0, 0, w, 0);
        grad.addColorStop(0.00, 'rgba(0, 0, 0, 0)');          // Inner gap
        grad.addColorStop(0.05, 'rgba(100, 90, 78, 0.40)');    // C-ring faint
        grad.addColorStop(0.16, 'rgba(145, 134, 115, 0.75)');
        grad.addColorStop(0.24, 'rgba(215, 203, 175, 0.98)');  // B-ring bright
        grad.addColorStop(0.58, 'rgba(200, 188, 160, 0.95)');
        grad.addColorStop(0.64, 'rgba(12, 10, 15, 0.12)');     // Cassini Division
        grad.addColorStop(0.70, 'rgba(180, 168, 142, 0.92)');  // A-ring
        grad.addColorStop(0.92, 'rgba(140, 128, 108, 0.85)');
        grad.addColorStop(0.98, 'rgba(60, 52, 42, 0.40)');     // Outer edge
        grad.addColorStop(1.00, 'rgba(0, 0, 0, 0)');

        ctx.fillStyle = grad;
        ctx.fillRect(0, 0, w, h);

        // Add micro-bands and striations across ring radius
        for (let x = 40; x < w - 20; x += 3) {
            const frac = x / w;
            if (frac > 0.62 && frac < 0.66) continue; // Don't paint in Cassini division
            const lum = (Math.sin(x * 1.5) > 0) ? 'rgba(255,245,225,0.06)' : 'rgba(0,0,0,0.06)';
            ctx.fillStyle = lum;
            ctx.fillRect(x, 0, 2, h);
        }

        const texture = new THREE.CanvasTexture(c);
        texture.generateMipmaps = true;
        texture.minFilter = THREE.LinearMipmapLinearFilter;
        return texture;
    }

    // 3. Texture Initialization with Fallback
    const textureLoader = new THREE.TextureLoader();
    let planetMat, ringMat;

    // Start with procedural textures (loads in 0 ms, guaranteed to work offline/file://)
    const initialPlanetTex = generatePlanetCanvasTexture();
    const initialRingTex = generateRingCanvasTexture();

    planetMat = new THREE.MeshStandardMaterial({
        map: initialPlanetTex,
        roughness: 0.88,
        metalness: 0.05
    });

    ringMat = new THREE.MeshStandardMaterial({
        map: initialRingTex,
        side: THREE.DoubleSide,
        transparent: true,
        opacity: 0.96,
        roughness: 0.65,
        metalness: 0.08
    });

    // Attempt to enhance with high-res pre-baked PNG textures if accessible
    textureLoader.load('saturn-planet-texture.png', function(tex) {
        tex.generateMipmaps = true;
        tex.minFilter = THREE.LinearMipmapLinearFilter;
        planetMat.map = tex;
        planetMat.needsUpdate = true;
    }, undefined, function() {
        // Fallback already in place
    });

    textureLoader.load('saturn-ring-texture.png', function(tex) {
        tex.generateMipmaps = true;
        tex.minFilter = THREE.LinearMipmapLinearFilter;
        ringMat.map = tex;
        ringMat.needsUpdate = true;
    }, undefined, function() {
        // Fallback already in place
    });

    // 4. Planet Sphere Geometry
    const planetRadius = 3.2;
    const planetGeo = new THREE.SphereGeometry(planetRadius, 64, 64);
    const planet = new THREE.Mesh(planetGeo, planetMat);
    planet.castShadow = true;
    planet.receiveShadow = true;

    // 5. Ring Geometry with Radial UV Mapping
    const innerRadius = 4.2;
    const outerRadius = 9.8;
    const ringGeo = new THREE.RingGeometry(innerRadius, outerRadius, 240);

    const pos = ringGeo.attributes.position;
    const uvs = ringGeo.attributes.uv;
    for (let i = 0; i < pos.count; i++) {
        const x = pos.getX(i);
        const y = pos.getY(i);
        const dist = Math.sqrt(x * x + y * y);
        const u = (dist - innerRadius) / (outerRadius - innerRadius);
        const angle = Math.atan2(y, x);
        const v = (angle + Math.PI) / (2.0 * Math.PI);
        uvs.setXY(i, u, v);
    }
    uvs.needsUpdate = true;

    const ring = new THREE.Mesh(ringGeo, ringMat);
    ring.rotation.x = Math.PI / 2;
    ring.castShadow = true;
    ring.receiveShadow = true;

    // 6. Hierarchy: Spinning Body inside Tilted System
    // Both planet AND ring belong to rotatingBody so they rotate together in 3D
    const rotatingBody = new THREE.Group();
    rotatingBody.add(planet);
    rotatingBody.add(ring);

    // tiltedSystem holds the authentic Saturn axial tilt from the reference image
    const tiltedSystem = new THREE.Group();
    tiltedSystem.add(rotatingBody);

    // Reference image tilt angles:
    tiltedSystem.rotation.x = 0.44;  // Pitch: brings top of planet & rings into clear view
    tiltedSystem.rotation.z = -0.19; // Yaw/Roll: downward slope to the right
    tiltedSystem.position.set(0, 0.2, 0);

    scene.add(tiltedSystem);

    // 7. Deep Cosmic Starfield
    const starCount = 1400;
    const starGeo = new THREE.BufferGeometry();
    const starPositions = new Float32Array(starCount * 3);
    const starColors = new Float32Array(starCount * 3);

    for (let i = 0; i < starCount; i++) {
        const idx = i * 3;
        const u = Math.random();
        const v = Math.random();
        const theta = u * 2.0 * Math.PI;
        const phi = Math.acos(2.0 * v - 1.0);
        const dist = 70 + Math.random() * 80;

        starPositions[idx]     = dist * Math.sin(phi) * Math.cos(theta);
        starPositions[idx + 1] = dist * Math.sin(phi) * Math.sin(theta);
        starPositions[idx + 2] = -30 - Math.random() * 50;

        const warmth = Math.random();
        starColors[idx]     = 0.90 + 0.10 * warmth;
        starColors[idx + 1] = 0.88 + 0.12 * (1.0 - warmth);
        starColors[idx + 2] = 0.95 + 0.05 * (1.0 - warmth);
    }
    starGeo.setAttribute('position', new THREE.BufferAttribute(starPositions, 3));
    starGeo.setAttribute('color', new THREE.BufferAttribute(starColors, 3));

    const starMat = new THREE.PointsMaterial({
        size: 1.2,
        vertexColors: true,
        transparent: true,
        opacity: 0.85
    });
    const starfield = new THREE.Points(starGeo, starMat);
    scene.add(starfield);

    // 8. Lighting System (Matches Directional Sunlight in Reference Image)
    const sunLight = new THREE.DirectionalLight(0xfff5e6, 2.4);
    sunLight.position.set(12, 10, 16);
    sunLight.castShadow = true;
    sunLight.shadow.mapSize.width = 2048;
    sunLight.shadow.mapSize.height = 2048;
    sunLight.shadow.camera.near = 5;
    sunLight.shadow.camera.far = 40;
    sunLight.shadow.camera.left = -12;
    sunLight.shadow.camera.right = 12;
    sunLight.shadow.camera.top = 12;
    sunLight.shadow.camera.bottom = -12;
    sunLight.shadow.bias = -0.0005;
    scene.add(sunLight);

    const ambientLight = new THREE.AmbientLight(0x18141f, 0.42);
    scene.add(ambientLight);

    const ringBounce = new THREE.DirectionalLight(0xdcc8a0, 0.4);
    ringBounce.position.set(-10, -6, -8);
    scene.add(ringBounce);

    // 9. Interactive Mouse Parallax
    let targetTiltX = 0;
    let targetTiltY = 0;

    window.addEventListener('mousemove', (e) => {
        const xNorm = (e.clientX / window.innerWidth) - 0.5;
        const yNorm = (e.clientY / window.innerHeight) - 0.5;
        targetTiltY = xNorm * 0.12;
        targetTiltX = yNorm * 0.08;
    }, { passive: true });

    // 10. Responsive Viewport Handler
    function onWindowResize() {
        const width = window.innerWidth;
        const height = window.innerHeight;
        camera.aspect = width / height;

        if (width < 768) {
            camera.position.z = 26.0;
        } else if (width < 1200) {
            camera.position.z = 22.0;
        } else {
            camera.position.z = 19.5;
        }

        camera.updateProjectionMatrix();
        renderer.setSize(width, height);
    }
    window.addEventListener('resize', onWindowResize, { passive: true });
    onWindowResize();

    // 11. Smooth 60 FPS Animation Loop
    // Rotates the planet AND the ring together in lockstep 3D space
    let lastTime = performance.now();

    function animate(currentTime) {
        requestAnimationFrame(animate);

        const delta = Math.min((currentTime - lastTime) / 1000, 0.1);
        lastTime = currentTime;

        // 1. Pure axial rotation: Planet AND ring rotate together left-to-right
        const spinSpeed = 0.08; // radians / sec
        rotatingBody.rotation.y += spinSpeed * delta;

        // 2. Subtle 3D orbital undulation
        tiltedSystem.rotation.y = Math.sin(currentTime * 0.00015) * 0.06;

        // 3. Smooth mouse parallax
        tiltedSystem.rotation.z = -0.19 + (targetTiltY - tiltedSystem.rotation.z) * 0.04;
        tiltedSystem.rotation.x = 0.44 + (targetTiltX - tiltedSystem.rotation.x) * 0.04;

        renderer.render(scene, camera);
    }

    requestAnimationFrame(animate);

})();
