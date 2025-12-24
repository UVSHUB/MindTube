/* Google Labs - Main Interactions
 * Kept minimal to match the clean aesthetic. 
 * No video background unless requested. 
 */

document.addEventListener('DOMContentLoaded', () => {
    console.log('Labs Theme Loaded');

    // Smooth Scrolling for Anchors
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    });

    // Optional: Add simple scroll animations for cards
    // Generic Scroll Animation Observer
    const scrollObserver = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('is-visible');
            }
        });
    }, { threshold: 0.1 });

    // Observe all elements with .animate-on-scroll
    document.querySelectorAll('.animate-on-scroll').forEach(el => {
        scrollObserver.observe(el);
    });
    // UI Simulation Logic
    const simSection = document.querySelector('.simulation-container');
    if (simSection) {
        const input = simSection.querySelector('.sim-input');
        const btn = simSection.querySelector('.sim-btn');
        const cursor = simSection.querySelector('.sim-cursor');
        const loading = simSection.querySelector('.sim-loading');
        const result = simSection.querySelector('.sim-result');
        let isAnimating = false;

        const startSimulation = () => {
            if (isAnimating) return;
            isAnimating = true;

            // Reset state
            input.value = "";
            loading.style.display = 'none';
            result.style.display = 'none';
            cursor.style.top = '100px';
            cursor.style.left = '90%';

            // Step 1: Move cursor to input
            setTimeout(() => {
                cursor.style.top = '180px'; // Approx input y
                cursor.style.left = '100px'; // Approx input x (relative to container logic needed but basic css works)
                // For simplicity in JS-driven CSS animation without precise coords, we use classes or simpler logic.
                // Re-doing with CSS classes would be cleaner but here we simulate flow.
                // Let's rely on visual placement.
                cursor.style.top = '220px';
                cursor.style.left = '20%';
            }, 500);

            // Step 2: Type URL
            setTimeout(() => {
                let text = "https://youtube.com/watch?v=example";
                let i = 0;
                const typeInterval = setInterval(() => {
                    input.value += text.charAt(i);
                    i++;
                    if (i >= text.length) clearInterval(typeInterval);
                }, 50);
            }, 1500);

            // Step 3: Move cursor to button and click
            setTimeout(() => {
                cursor.style.left = '75%'; // Move to button
            }, 3500);

            setTimeout(() => {
                btn.classList.add('btn-active'); // Simulate click press
                setTimeout(() => btn.classList.remove('btn-active'), 200);
                loading.style.display = 'block';
            }, 4500);

            // Step 4: Show Result
            setTimeout(() => {
                loading.style.display = 'none';
                result.style.display = 'block';
                isAnimating = false; // Allow re-trigger if needed, or keep done.
            }, 6500);
        };

        const simObserver = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    startSimulation();
                }
            });
        }, { threshold: 0.5 });
        simObserver.observe(simSection);
    }
});
