document.addEventListener('DOMContentLoaded', () => {
    const canvas = document.getElementById('heroCanvas');
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    let width, height;

    // Configuration
    // Greatly increased particle count to fill "blank spots" during rotation
    const particleCount = 400;
    const fov = 800;
    const connectionDistance = 140;
    const rotationSpeed = 0.001;

    // Brand Colors with Full Opacity
    const colors = [
        'rgba(66, 133, 244, 1.0)', // Blue
        'rgba(234, 67, 53, 1.0)',  // Red
        'rgba(251, 188, 4, 1.0)',  // Yellow
        'rgba(52, 168, 83, 1.0)'   // Green
    ];

    let points = [];
    let angleX = 0;
    let angleY = 0;
    let mouseX = 0;
    let mouseY = 0;

    function resize() {
        width = canvas.width = canvas.offsetWidth;
        height = canvas.height = canvas.offsetHeight;
        initPoints();
    }

    class Point3D {
        constructor() {
            // Create a more spherical/uniform distribution
            // We use a larger range to ensure even when rotated, there is depth
            const range = Math.max(width, height) * 1.2;

            this.x = (Math.random() - 0.5) * range;
            this.y = (Math.random() - 0.5) * range;
            this.z = (Math.random() - 0.5) * range;

            this.color = colors[Math.floor(Math.random() * colors.length)];
            // Big size: 4px to 10px
            this.size = Math.random() * 6 + 4;
        }

        rotate(angleX, angleY) {
            const cosY = Math.cos(angleY);
            const sinY = Math.sin(angleY);
            const x1 = this.x * cosY - this.z * sinY;
            const z1 = this.z * cosY + this.x * sinY;

            const cosX = Math.cos(angleX);
            const sinX = Math.sin(angleX);
            const y2 = this.y * cosX - z1 * sinX;
            const z2 = z1 * cosX + this.y * sinX;

            return { x: x1, y: y2, z: z2, color: this.color, size: this.size };
        }
    }

    function initPoints() {
        points = [];
        for (let i = 0; i < particleCount; i++) {
            points.push(new Point3D());
        }
    }

    function loop() {
        ctx.clearRect(0, 0, width, height);

        // Slow auto rotation
        angleY += rotationSpeed;
        // Gentle X tilt based on mouse to keep it dynamic but stable
        angleX += (mouseY / height - 0.5) * 0.005;

        // Mouse X interaction speeds up rotation slightly
        angleY += (mouseX / width - 0.5) * 0.005;

        let projectedPoints = [];

        for (let i = 0; i < points.length; i++) {
            const p = points[i].rotate(angleX, angleY);

            const scale = fov / (fov + p.z + 1000);

            const x2d = p.x * scale + width / 2;
            const y2d = p.y * scale + height / 2;

            if (scale > 0) {
                projectedPoints.push({ x: x2d, y: y2d, scale: scale, color: p.color, size: p.size });
            }
        }

        // Draw connections
        ctx.lineWidth = 1.0;
        for (let i = 0; i < projectedPoints.length; i++) {
            const p1 = projectedPoints[i];

            // Optimization: limit inner loop to nearby indices or fewer checks if slow
            // For 400 particles, O(N^2) is 160,000 checks/frame. Can be heavy.
            // Let's check against a subset or only draw if close for valid connections.
            // A simple distance check is fast enough for modern CPUs.

            for (let j = i + 1; j < projectedPoints.length; j++) {
                const p2 = projectedPoints[j];
                const dx = p1.x - p2.x;
                const dy = p1.y - p2.y;

                // Quick bounding box check
                if (Math.abs(dx) > connectionDistance * p1.scale) continue;
                if (Math.abs(dy) > connectionDistance * p1.scale) continue;

                const dist = Math.sqrt(dx * dx + dy * dy);

                if (dist < connectionDistance * p1.scale) {
                    ctx.beginPath();
                    const alpha = (1 - dist / (connectionDistance * p1.scale)) * 0.5;
                    ctx.strokeStyle = `rgba(100, 100, 100, ${alpha})`;
                    ctx.moveTo(p1.x, p1.y);
                    ctx.lineTo(p2.x, p2.y);
                    ctx.stroke();
                }
            }
        }

        // Draw Nodes
        for (let i = 0; i < projectedPoints.length; i++) {
            const p = projectedPoints[i];
            ctx.beginPath();
            ctx.arc(p.x, p.y, p.size * p.scale, 0, Math.PI * 2);
            ctx.fillStyle = p.color;
            ctx.fill();
        }

        requestAnimationFrame(loop);
    }

    document.addEventListener('mousemove', (e) => {
        mouseX = e.clientX;
        mouseY = e.clientY;
    });

    window.addEventListener('resize', resize);
    resize();
    initPoints();
    loop();
});
