// ============================================================================
// JUPITER SEARCH ENGINE - CLIENT SCRIPT (v8.0)
// Auto-Suggestions + Real-time Gemini & Offline DSA Linked List Search
// Add Offline Article Modal + SQLite Persistence
// ============================================================================

const searchBox = document.getElementById('searchBox');
const searchInput = document.getElementById('searchInput');
const searchWrapper = document.getElementById('searchWrapper');
const searchBtn = document.getElementById('searchBtn');
const suggestionsDropdown = document.getElementById('suggestionsDropdown');
const resultsContainer = document.getElementById('resultsContainer');

// Mode Switcher Elements
const modeAutoBtn = document.getElementById('modeAutoBtn');
const modeOfflineBtn = document.getElementById('modeOfflineBtn');
let currentSearchMode = 'auto'; // 'auto' | 'offline'

// Modal Elements
const openModalBtn = document.getElementById('openModalBtn');
const closeModalBtn = document.getElementById('closeModalBtn');
const cancelModalBtn = document.getElementById('cancelModalBtn');
const articleModal = document.getElementById('articleModal');
const addArticleForm = document.getElementById('addArticleForm');
const docTitle = document.getElementById('docTitle');
const docDescription = document.getElementById('docDescription');
const docScore = document.getElementById('docScore');
const scoreValue = document.getElementById('scoreValue');
const modalAlert = document.getElementById('modalAlert');
const saveArticleBtn = document.getElementById('saveArticleBtn');

let debounceTimer = null;
let activeSuggestionIndex = -1;
let currentSuggestions = [];

// Instant Client-Side Dictionary (guarantees 0ms suggestions even if server is offline)
const LOCAL_SUGGESTIONS = [
    "Bubble Sort Algorithm",
    "Insertion Sort Algorithm",
    "Selection Sort Algorithm",
    "Merge Sort Algorithm",
    "Quick Sort Algorithm",
    "Heap Sort Algorithm",
    "Radix Sort Algorithm",
    "Counting Sort Algorithm",
    "Shell Sort Algorithm",
    "Bucket Sort Algorithm",
    "Binary Search Algorithm",
    "Linear Search Algorithm",
    "Binary Search Tree (BST)",
    "AVL Tree Balancing",
    "Red-Black Tree",
    "Graph Breadth First Search (BFS)",
    "Graph Depth First Search (DFS)",
    "Graph Dijkstra Algorithm",
    "Custom Singly Linked List",
    "Doubly Linked List",
    "Custom Stack Implementation (LIFO)",
    "Queue Data Structure (FIFO)",
    "Priority Queue & Heap",
    "Dynamic Array Resizing",
    "Hash Table & Hash Map",
    "Trie Prefix Tree",
    "Min Heap and Max Heap",
    "Jupiter Gas Giant Planet",
    "Jupiter Moons Europa & Io",
    "Jupiter Great Red Spot",
    "Saturn Planetary Ring System",
    "Black Hole Singularity",
    "Big Bang Theory",
    "James Webb Space Telescope",
    "Solar System Exploration"
];

// ============================================================================
// SEARCH MODE SWITCHER (Auto Gemini vs Offline DSA Only)
// ============================================================================
modeAutoBtn.addEventListener('click', () => {
    currentSearchMode = 'auto';
    modeAutoBtn.classList.add('active');
    modeOfflineBtn.classList.remove('active');
    if (searchInput.value.trim().length > 0) {
        handleSearch();
    }
});

modeOfflineBtn.addEventListener('click', () => {
    currentSearchMode = 'offline';
    modeOfflineBtn.classList.add('active');
    modeAutoBtn.classList.remove('active');
    if (searchInput.value.trim().length > 0) {
        handleSearch();
    }
});

// ============================================================================
// ADD ARTICLE MODAL DIALOG
// ============================================================================
function openModal() {
    hideSuggestions();
    modalAlert.style.display = 'none';
    modalAlert.className = 'modal-alert';
    docScore.value = '95.0';
    scoreValue.textContent = '95.0';
    articleModal.style.display = 'flex';
    setTimeout(() => docTitle.focus(), 80);
}

function closeModal() {
    articleModal.style.display = 'none';
    addArticleForm.reset();
    modalAlert.style.display = 'none';
    saveArticleBtn.disabled = false;
    saveArticleBtn.textContent = 'Save to Database';
}

openModalBtn.addEventListener('click', openModal);
closeModalBtn.addEventListener('click', closeModal);
cancelModalBtn.addEventListener('click', closeModal);

// Close modal when clicking on overlay background
articleModal.addEventListener('click', (e) => {
    if (e.target === articleModal) {
        closeModal();
    }
});

// Slider score display update
docScore.addEventListener('input', () => {
    scoreValue.textContent = parseFloat(docScore.value).toFixed(1);
});

// Form submission -> POST /add-document
addArticleForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    const title = docTitle.value.trim();
    const description = docDescription.value.trim();
    const score = parseFloat(docScore.value) || 95.0;

    if (!title || !description) {
        showModalAlert('Please fill out both the title and notes.', 'error');
        return;
    }

    saveArticleBtn.disabled = true;
    saveArticleBtn.textContent = 'Saving...';
    modalAlert.style.display = 'none';

    try {
        const response = await fetch('http://localhost:8080/add-document', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json; charset=UTF-8'
            },
            body: JSON.stringify({ title, description, score })
        });

        const data = await response.json();

        if (response.ok && data.success) {
            showModalAlert(`✅ "${escapeHtml(title)}" saved to SQLite DB & Custom LinkedList!`, 'success');
            
            // Add immediately to client-side suggestions
            if (!LOCAL_SUGGESTIONS.includes(title)) {
                LOCAL_SUGGESTIONS.unshift(title);
            }

            setTimeout(() => {
                closeModal();
                // If in offline mode, automatically search for this new title
                searchInput.value = title;
                searchBox.classList.add('active');
                searchWrapper.classList.add('active');
                handleSearch();
            }, 1200);

        } else {
            showModalAlert(`⚠️ Failed to save: ${data.error || 'Server rejected request'}`, 'error');
            saveArticleBtn.disabled = false;
            saveArticleBtn.textContent = 'Save to Database';
        }

    } catch (err) {
        showModalAlert('⚠️ Server offline! Make sure JupiterServer.java is running on port 8080.', 'error');
        saveArticleBtn.disabled = false;
        saveArticleBtn.textContent = 'Save to Database';
    }
});

function showModalAlert(msg, type) {
    modalAlert.innerHTML = msg;
    modalAlert.className = `modal-alert ${type}`;
    modalAlert.style.display = 'block';
}

// ============================================================================
// SEARCH INPUT EXPAND / COLLAPSE & SUGGESTIONS
// ============================================================================

// Expand search bar when input is focused
searchInput.addEventListener('focus', () => {
    searchBox.classList.add('active');
    searchWrapper.classList.add('active');
    const query = searchInput.value.trim();
    if (query.length > 0) {
        showSuggestions(query);
    }
});

// Clicking on search box focuses input
searchBox.addEventListener('click', (e) => {
    searchBox.classList.add('active');
    searchWrapper.classList.add('active');
    if (!e.target.closest('#searchBtn')) {
        searchInput.focus();
    }
});

// Typing triggers real-time suggestions & ensures active state
searchInput.addEventListener('input', () => {
    const query = searchInput.value.trim();
    if (query.length > 0) {
        searchBox.classList.add('active');
        searchWrapper.classList.add('active');
        showSuggestions(query);
    } else {
        hideSuggestions();
    }
});

// Collapse only when clicked away AND both query & results are empty
searchInput.addEventListener('blur', () => {
    setTimeout(() => {
        const hasQuery = searchInput.value.trim().length > 0;
        const hasResults = resultsContainer && resultsContainer.innerHTML.trim().length > 0;
        if (!hasQuery && !hasResults) {
            searchBox.classList.remove('active');
            searchWrapper.classList.remove('active');
        }
    }, 250);
});

// Keyboard navigation (ArrowDown, ArrowUp, Escape, Enter)
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        if (articleModal.style.display === 'flex') {
            closeModal();
            return;
        }
        hideSuggestions();
    }
});

searchInput.addEventListener('keydown', (e) => {
    const items = suggestionsDropdown.querySelectorAll('.suggestion-item');

    if (e.key === 'ArrowDown') {
        e.preventDefault();
        if (items.length > 0) {
            activeSuggestionIndex = (activeSuggestionIndex + 1) % items.length;
            updateActiveSuggestion(items);
        }
    } else if (e.key === 'ArrowUp') {
        e.preventDefault();
        if (items.length > 0) {
            activeSuggestionIndex = (activeSuggestionIndex - 1 + items.length) % items.length;
            updateActiveSuggestion(items);
        }
    } else if (e.key === 'Enter') {
        e.preventDefault();
        e.stopPropagation();

        if (activeSuggestionIndex >= 0 && activeSuggestionIndex < currentSuggestions.length) {
            searchInput.value = currentSuggestions[activeSuggestionIndex];
        }
        hideSuggestions();
        handleSearch();
    }
});

// Search button click
searchBtn.addEventListener('click', (e) => {
    e.preventDefault();
    e.stopPropagation();
    hideSuggestions();
    handleSearch();
});

// Close suggestions when clicking outside
document.addEventListener('click', (e) => {
    if (!e.target.closest('#searchWrapper')) {
        hideSuggestions();
    }
});

// ============================================================================
// AUTO SUGGESTIONS (Client Filter + Backend CustomTrie & SQLite)
// ============================================================================
function showSuggestions(query) {
    if (!query || query.length === 0) {
        hideSuggestions();
        return;
    }

    const qLower = query.toLowerCase();

    // 1. Instant local dictionary filter (0ms delay)
    const localMatches = LOCAL_SUGGESTIONS.filter(item => 
        item.toLowerCase().includes(qLower)
    );

    currentSuggestions = localMatches.slice(0, 6);
    renderSuggestions(currentSuggestions, query);

    // 2. Query backend Java Trie & SQLite search history in parallel
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
        fetchServerSuggestions(query);
    }, 120);
}

async function fetchServerSuggestions(query) {
    try {
        const res = await fetch(`http://localhost:8080/suggest?q=${encodeURIComponent(query)}`);
        if (!res.ok) return;
        const serverList = await res.json();

        if (searchInput.value.trim().toLowerCase() !== query.toLowerCase()) return;

        const mergedSet = new Set();
        if (Array.isArray(serverList)) {
            serverList.forEach(s => mergedSet.add(s));
        }
        currentSuggestions.forEach(s => mergedSet.add(s));

        currentSuggestions = Array.from(mergedSet).slice(0, 6);
        renderSuggestions(currentSuggestions, query);
    } catch (err) {
        // Backend offline; local dictionary keeps functioning smoothly
    }
}

function renderSuggestions(suggestions, query) {
    if (!suggestions || suggestions.length === 0) {
        hideSuggestions();
        return;
    }

    activeSuggestionIndex = -1;
    const qLower = query.toLowerCase();

    let html = '';
    suggestions.forEach((text, idx) => {
        let displayHtml = escapeHtml(text);
        const matchPos = text.toLowerCase().indexOf(qLower);
        if (matchPos !== -1) {
            const before = escapeHtml(text.substring(0, matchPos));
            const match = escapeHtml(text.substring(matchPos, matchPos + query.length));
            const after = escapeHtml(text.substring(matchPos + query.length));
            displayHtml = `${before}<span class="suggestion-match">${match}</span>${after}`;
        }

        html += `
            <div class="suggestion-item" data-index="${idx}">
                <svg class="suggestion-icon" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-4.35-4.35m0 0A7.5 7.5 0 104.5 4.5a7.5 7.5 0 0012.15 12.15z" />
                </svg>
                <span class="suggestion-text">${displayHtml}</span>
            </div>
        `;
    });

    suggestionsDropdown.innerHTML = html;
    suggestionsDropdown.style.display = 'block';

    suggestionsDropdown.querySelectorAll('.suggestion-item').forEach(item => {
        item.addEventListener('mousedown', (e) => {
            e.preventDefault();
            const idx = parseInt(item.getAttribute('data-index'), 10);
            if (currentSuggestions[idx]) {
                searchInput.value = currentSuggestions[idx];
                hideSuggestions();
                handleSearch();
            }
        });
    });
}

function updateActiveSuggestion(items) {
    items.forEach((item, idx) => {
        if (idx === activeSuggestionIndex) {
            item.classList.add('active');
            item.scrollIntoView({ block: 'nearest' });
            searchInput.value = currentSuggestions[idx];
        } else {
            item.classList.remove('active');
        }
    });
}

function hideSuggestions() {
    suggestionsDropdown.style.display = 'none';
    suggestionsDropdown.innerHTML = '';
    activeSuggestionIndex = -1;
    currentSuggestions = [];
}

function escapeHtml(str) {
    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

let currentSortCriteria = 'score_desc'; // 'score_desc' | 'title_az' | 'date_desc'
let lastSearchResults = [];
let lastResponseData = null;

// ============================================================================
// PURE DSA MERGE SORT: O(n log n) Guaranteed Time & Stable Object Sorting
// Supports: "score_desc" (High to Low), "title_az" (A to Z), "date_desc" (Newest)
// ============================================================================
function mergeSortResults(arr, criteria) {
    if (!arr || arr.length <= 1) return arr;

    const mid = Math.floor(arr.length / 2);
    const left = mergeSortResults(arr.slice(0, mid), criteria);
    const right = mergeSortResults(arr.slice(mid), criteria);

    return mergeResults(left, right, criteria);
}

function mergeResults(left, right, criteria) {
    const result = [];
    let i = 0, j = 0;

    while (i < left.length && j < right.length) {
        let leftComesFirst = false;

        if (criteria === 'title_az') {
            leftComesFirst = left[i].title.localeCompare(right[j].title) <= 0;
        } else if (criteria === 'date_desc') {
            const dateA = left[i].timestamp || 0;
            const dateB = right[j].timestamp || 0;
            leftComesFirst = dateA >= dateB; // Newest first
        } else {
            // Default: 'score_desc' (Highest score first)
            const scoreA = parseFloat(left[i].score) || 0;
            const scoreB = parseFloat(right[j].score) || 0;
            leftComesFirst = scoreA >= scoreB;
        }

        if (leftComesFirst) {
            result.push(left[i++]);
        } else {
            result.push(right[j++]);
        }
    }

    return result.concat(left.slice(i)).concat(right.slice(j));
}

// Render results with dynamic Merge Sort toolbar & meta badges
function renderResultsUI(data, sortedItems) {
    const engineBadge = data.engine ? ` | Engine: <b style="color:#84e1bc">${data.engine}</b>` : '';
    let html = `
        <div style="font-size: 0.85rem; color: #b8c4d0; margin-bottom: 12px; text-align: center; background: rgba(20,20,25,0.6); padding: 8px 16px; border-radius: 20px; border: 1px solid rgba(235,205,155,0.2);">
            Search Time: <b style="color:#e6c280">${data.timeMs} ms</b> | 
            History Stack: <b style="color:#e6c280">${data.historyCount}</b> | 
            Query: <b style="color:#e6c280">${data.query}</b>${engineBadge}
        </div>

        <!-- Merge Sort Ranking Toolbar -->
        <div class="sort-toolbar">
            <span class="sort-label">
                <svg width="14" height="14" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M3 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 4a1 1 0 011-1h8a1 1 0 110 2H4a1 1 0 01-1-1zm0 4a1 1 0 011-1h4a1 1 0 110 2H4a1 1 0 01-1-1z" clip-rule="evenodd" />
                </svg>
                Merge Sort:
            </span>
            <div class="sort-chips">
                <button type="button" class="sort-chip ${currentSortCriteria === 'score_desc' ? 'active' : ''}" data-sort="score_desc">
                    ★ Score: High to Low
                </button>
                <button type="button" class="sort-chip ${currentSortCriteria === 'title_az' ? 'active' : ''}" data-sort="title_az">
                    🔤 Title: A–Z
                </button>
                <button type="button" class="sort-chip ${currentSortCriteria === 'date_desc' ? 'active' : ''}" data-sort="date_desc">
                    📅 Date Modified
                </button>
            </div>
        </div>
    `;

    sortedItems.forEach(item => {
        const dateDisplay = item.date ? `<span class="result-date-badge">📅 ${escapeHtml(item.date)}</span>` : '';
        html += `
            <div style="background: rgba(18, 18, 24, 0.88); backdrop-filter: blur(14px); border: 1px solid rgba(235, 205, 155, 0.35); padding: 16px 20px; border-radius: 14px; margin-bottom: 12px; box-shadow: 0 8px 24px rgba(0,0,0,0.6);">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; gap: 10px;">
                    <h3 style="color: #f5deb3; font-size: 1.05rem; font-weight: 600;">${escapeHtml(item.title)}</h3>
                    <div class="result-meta-badges">
                        ${dateDisplay}
                        <span style="font-size: 0.8rem; color: #84e1bc; font-weight: 500; background: rgba(132, 225, 188, 0.12); padding: 2px 8px; border-radius: 8px;">★ ${item.score}</span>
                    </div>
                </div>
                <p style="color: #d0d7de; font-size: 0.92rem; line-height: 1.45;">${escapeHtml(item.description)}</p>
            </div>
        `;
    });

    resultsContainer.innerHTML = html;

    // Attach click listeners to Sort Chips to re-sort results in 0ms!
    resultsContainer.querySelectorAll('.sort-chip').forEach(chip => {
        chip.addEventListener('click', () => {
            const newSort = chip.getAttribute('data-sort');
            if (newSort === currentSortCriteria) return;
            currentSortCriteria = newSort;

            // Execute Pure DSA Merge Sort instantaneously on client
            const reSorted = mergeSortResults(lastSearchResults, currentSortCriteria);
            renderResultsUI(lastResponseData, reSorted);
        });
    });
}

// ============================================================================
// SEARCH EXECUTION (Gemini AI or Offline CustomLinkedList)
// ============================================================================
async function handleSearch() {
    const query = searchInput.value.trim();
    if (!query) return;

    hideSuggestions();
    searchBox.classList.add('active');
    searchWrapper.classList.add('active');

    const searchingBadge = currentSearchMode === 'offline' 
        ? '💾 Searching offline CustomLinkedList & SQLite...' 
        : '⚡ Searching with Jupiter AI...';

    resultsContainer.innerHTML = `
        <div style="text-align: center; padding: 20px; color: #e6c280; font-size: 1.1rem; background: rgba(20,20,25,0.7); border-radius: 14px; border: 1px solid rgba(235,205,155,0.25);">
            ${searchingBadge}
        </div>
    `;

    try {
        const url = `http://localhost:8080/search?q=${encodeURIComponent(query)}&mode=${encodeURIComponent(currentSearchMode)}&sort=${encodeURIComponent(currentSortCriteria)}`;
        const response = await fetch(url);
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }
        const data = await response.json();
        lastResponseData = data;

        if (!data.results || data.results.length === 0) {
            lastSearchResults = [];
            const extraTip = currentSearchMode === 'offline' 
                ? '<div style="margin-top: 10px; font-size: 0.85rem; color: #84e1bc;">Tip: Click <b>"+ Add Article"</b> above to save this topic into your offline database!</div>'
                : '';
            resultsContainer.innerHTML = `
                <div style="background: rgba(20, 20, 25, 0.8); padding: 22px; border-radius: 14px; text-align: center; border: 1px solid rgba(235, 205, 155, 0.25); color: #f5deb3;">
                    No articles found for "<b>${escapeHtml(data.query)}</b>"
                    ${extraTip}
                </div>
            `;
            return;
        }

        lastSearchResults = Array.isArray(data.results) ? data.results : [];

        // Ensure each item has timestamp and human date
        lastSearchResults.forEach((item, idx) => {
            if (!item.timestamp) {
                item.timestamp = Date.now() - (idx * 60000);
            }
            if (!item.date) {
                const d = new Date(item.timestamp);
                const year = d.getFullYear();
                const month = String(d.getMonth() + 1).padStart(2, '0');
                const day = String(d.getDate()).padStart(2, '0');
                const hours = String(d.getHours()).padStart(2, '0');
                const minutes = String(d.getMinutes()).padStart(2, '0');
                item.date = `${year}-${month}-${day} ${hours}:${minutes}`;
            }
        });

        // Run Merge Sort on results array
        const sortedItems = mergeSortResults(lastSearchResults, currentSortCriteria);
        renderResultsUI(data, sortedItems);

    } catch (error) {
        resultsContainer.innerHTML = `
            <div style="background: rgba(40, 15, 20, 0.85); border: 1px solid rgba(255, 107, 107, 0.4); padding: 18px; border-radius: 14px; text-align: center; color: #ff8585;">
                <p style="font-weight: 600; font-size: 1rem; margin-bottom: 6px;">⚠️ Cannot Connect to Jupiter Server</p>
                <p style="font-size: 0.85rem; color: #d0d7de;">Please make sure <b>JupiterServer.java</b> is running in VS Code on port 8080!</p>
            </div>
        `;
    }
}
