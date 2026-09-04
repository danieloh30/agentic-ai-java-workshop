let currentSortColumn = 'id';
let currentSortDirection = 'asc';
let incidentsData = [];
let currentFilterText = '';
let currentFilterField = 'all';
let currentStatusFilter = null;
let lastUpdatedIncidentId = null;
let selectedIncidentId = null;

document.addEventListener('DOMContentLoaded', function () {
    initTheme();
    loadAllIncidents();
    setupEventListeners();
    setupSorting();
});

function initTheme() {
    const saved = localStorage.getItem('theme') || 'dark';
    applyTheme(saved);
    const toggle = document.getElementById('theme-toggle');
    if (toggle) toggle.addEventListener('click', function () {
        const next = document.documentElement.getAttribute('data-theme') === 'light' ? 'dark' : 'light';
        applyTheme(next);
        localStorage.setItem('theme', next);
    });
}

function applyTheme(theme) {
    if (theme === 'light') {
        document.documentElement.setAttribute('data-theme', 'light');
    } else {
        document.documentElement.removeAttribute('data-theme');
    }
    const btn = document.getElementById('theme-toggle');
    if (btn) btn.innerHTML = theme === 'light' ? '&#9790; Dark' : '&#9788; Light';
}

function getPriorityPresentation(priority) {
    const match = String(priority ?? '').trim().match(/^P?(\d+)$/i);
    if (!match) return { label: 'N/A', cssClass: '', sortValue: 0 };

    const level = match[1];
    return { label: `P${level}`, cssClass: `priority-${level}`, sortValue: Number(level) };
}

function loadAllIncidents() {
    fetch('/incidents')
        .then(response => {
            if (!response.ok) throw new Error('Network response was not ok');
            return response.json();
        })
        .then(incidents => {
            incidentsData = incidents;
            sortIncidents();
            renderStats();
            populateIncidentTable();
        })
        .catch(error => {
            console.error('Error fetching incidents:', error);
            showToast('Failed to load incident data. Please try again later.', 'error');
        });
}

function renderStats() {
    const counts = { OPEN: 0, TRIAGING: 0, IN_PROGRESS: 0, ESCALATED: 0, RESOLVED: 0 };
    incidentsData.forEach(i => { if (counts[i.status] !== undefined) counts[i.status]++; });

    const defs = [
        { key: 'OPEN', label: 'Open', color: 'var(--orange)', rgb: '255,131,43' },
        { key: 'TRIAGING', label: 'Triaging', color: 'var(--blue)', rgb: '69,137,255' },
        { key: 'IN_PROGRESS', label: 'In Progress', color: 'var(--red)', rgb: '250,77,86' },
        { key: 'ESCALATED', label: 'Escalated', color: 'var(--purple)', rgb: '165,110,255' },
        { key: 'RESOLVED', label: 'Resolved', color: 'var(--green)', rgb: '66,190,101' }
    ];

    const container = document.getElementById('stats-row');
    container.innerHTML = defs.map(d =>
        `<div class="stat-card${currentStatusFilter === d.key ? ' active' : ''}" data-status="${d.key}" style="--stat-color:${d.color};--stat-rgb:${d.rgb}">
            <div class="stat-count">${counts[d.key]}</div>
            <div class="stat-label">${d.label}</div>
        </div>`
    ).join('');

    container.querySelectorAll('.stat-card').forEach(card => {
        card.addEventListener('click', function () {
            const status = this.getAttribute('data-status');
            currentStatusFilter = currentStatusFilter === status ? null : status;
            renderStats();
            populateIncidentTable();
        });
    });

    const countEl = document.getElementById('incident-count');
    if (countEl) countEl.textContent = `(${incidentsData.length})`;
}

function setupSorting() {
    document.querySelectorAll('.sortable').forEach(header => {
        header.addEventListener('click', function () {
            const column = this.getAttribute('data-sort');
            if (column === currentSortColumn) {
                currentSortDirection = currentSortDirection === 'asc' ? 'desc' : 'asc';
            } else {
                currentSortColumn = column;
                currentSortDirection = 'asc';
            }
            updateSortHeaders();
            sortIncidents();
            populateIncidentTable();
        });
    });
}

function updateSortHeaders() {
    document.querySelectorAll('.sortable').forEach(h => h.classList.remove('sort-asc', 'sort-desc'));
    const cur = document.querySelector(`.sortable[data-sort="${currentSortColumn}"]`);
    if (cur) cur.classList.add(currentSortDirection === 'asc' ? 'sort-asc' : 'sort-desc');
}

function sortIncidents() {
    incidentsData.sort((a, b) => {
        let va, vb;
        if (currentSortColumn === 'status') {
            va = getStatusDisplay(a.status);
            vb = getStatusDisplay(b.status);
        } else if (currentSortColumn === 'priority') {
            va = getPriorityPresentation(a.priority).sortValue;
            vb = getPriorityPresentation(b.priority).sortValue;
        } else {
            va = a[currentSortColumn];
            vb = b[currentSortColumn];
        }
        if (currentSortColumn === 'id' || currentSortColumn === 'priority') {
            va = Number(va) || 0;
            vb = Number(vb) || 0;
        }
        if (va < vb) return currentSortDirection === 'asc' ? -1 : 1;
        if (va > vb) return currentSortDirection === 'asc' ? 1 : -1;
        return 0;
    });
}

function filterIncidents() {
    if (!currentFilterText) return incidentsData;
    const ft = currentFilterText.toLowerCase();
    return incidentsData.filter(incident => {
        if (currentFilterField !== 'all') {
            let v = incident[currentFilterField];
            if (currentFilterField === 'status') v = getStatusDisplay(v);
            return String(v).toLowerCase().includes(ft);
        }
        return (
            String(incident.id).toLowerCase().includes(ft) ||
            incident.system.toLowerCase().includes(ft) ||
            incident.service.toLowerCase().includes(ft) ||
            String(incident.priority).toLowerCase().includes(ft) ||
            (incident.description && incident.description.toLowerCase().includes(ft)) ||
            getStatusDisplay(incident.status).toLowerCase().includes(ft)
        );
    });
}

function populateIncidentTable() {
    const tbody = document.getElementById('incident-table-body');
    tbody.innerHTML = '';
    let filtered = currentFilterText ? filterIncidents() : incidentsData;
    if (currentStatusFilter) {
        filtered = filtered.filter(i => i.status === currentStatusFilter);
    }

    if (filtered.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="empty-state">No incidents match your filter</td></tr>';
        return;
    }

    filtered.forEach(incident => {
        const row = document.createElement('tr');
        if (incident.id === lastUpdatedIncidentId) {
            row.classList.add('highlight-row');
            setTimeout(() => { lastUpdatedIncidentId = null; }, 3000);
        }
        if (incident.id === selectedIncidentId) {
            row.classList.add('active-row');
        }

        const statusClass = getStatusClass(incident.status);
        const priority = getPriorityPresentation(incident.priority);

        row.innerHTML = `
            <td><span style="color:var(--accent);font-weight:600">#${incident.id}</span></td>
            <td>${incident.system}</td>
            <td>${incident.service}</td>
            <td><span class="priority-badge ${priority.cssClass}">${priority.label}</span></td>
            <td>${incident.description || 'N/A'}</td>
            <td><span class="status-indicator ${statusClass}"><span class="status-dot"></span><span class="status-text">${getStatusDisplay(incident.status)}</span></span></td>
            <td><button class="btn-view" onclick="openDetailPanel(${incident.id}); event.stopPropagation();">View</button></td>
        `;
        row.addEventListener('click', () => openDetailPanel(incident.id));
        tbody.appendChild(row);
    });
}

function openDetailPanel(incidentId) {
    const incident = incidentsData.find(i => i.id === incidentId);
    if (!incident) return;

    selectedIncidentId = incidentId;
    populateIncidentTable();

    const body = document.getElementById('detail-body');
    const title = document.getElementById('detail-title');
    title.textContent = `Incident #${incident.id}`;

    const statusClass = getStatusClass(incident.status);
    const priority = getPriorityPresentation(incident.priority);
    body.innerHTML = `
        <div class="detail-field">
            <div class="detail-label">Status</div>
            <div class="detail-value"><span class="status-indicator ${statusClass}"><span class="status-dot"></span><span class="status-text">${getStatusDisplay(incident.status)}</span></span></div>
        </div>
        <div class="detail-field">
            <div class="detail-label">Priority</div>
            <div class="detail-value"><span class="priority-badge ${priority.cssClass}">${priority.label}</span></div>
        </div>
        <div class="detail-field">
            <div class="detail-label">System</div>
            <div class="detail-value">${incident.system}</div>
        </div>
        <div class="detail-field">
            <div class="detail-label">Service</div>
            <div class="detail-value">${incident.service}</div>
        </div>
        <div class="detail-field">
            <div class="detail-label">Description</div>
            <div class="detail-value">${incident.description || 'N/A'}</div>
        </div>
        ${['RESOLVED', 'ESCALATED'].includes(incident.status) ? `
        <div class="detail-divider"></div>
        <div class="detail-form-title">Generate Post-Incident Report</div>
        <button class="btn-process" id="detail-report-btn" onclick="generateReport(${incident.id})">Generate Report</button>
        <div id="report-result" style="display:none; margin-top:1rem;"></div>
        ` : ''}
    `;

    document.getElementById('detail-panel').classList.add('open');
    document.getElementById('detail-overlay').classList.add('open');
}

function generateReport(incidentId) {
    const button = document.getElementById('detail-report-btn');
    const resultDiv = document.getElementById('report-result');

    button.disabled = true;
    button.classList.add('loading');
    button.textContent = 'Generating...';
    resultDiv.style.display = 'none';

    fetch(`/incident-report/${incidentId}`, { method: 'POST' })
        .then(response => {
            if (!response.ok) throw new Error('Network response was not ok');
            return response.json();
        })
        .then(data => {
            const score = data.score || 'N/A';
            const iteration = data.iteration || 'N/A';
            const report = (data.report || 'No report generated')
                .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
                .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
                .replace(/^- (.+)$/gm, '&bull; $1')
                .replace(/^\d+\.\s+(.+)$/gm, '&bull; $1')
                .replace(/\n/g, '<br>');
            resultDiv.innerHTML = `
                <div style="padding:0.75rem;background:var(--bg-secondary);border-radius:8px;border:1px solid var(--border-color);">
                    <div style="display:flex;gap:1rem;margin-bottom:0.75rem;">
                        <span class="priority-badge priority-1">Score: ${score}</span>
                        <span class="priority-badge priority-3">Iterations: ${iteration}</span>
                    </div>
                    <div style="font-size:0.85rem;max-height:300px;overflow-y:auto;line-height:1.6;">${report}</div>
                </div>
            `;
            resultDiv.style.display = 'block';
            showToast(`Report generated — score: ${score}, iterations: ${iteration}`);
            button.disabled = false;
            button.classList.remove('loading');
            button.textContent = 'Generate Report';
        })
        .catch(error => {
            console.error('Error generating report:', error);
            showToast('Failed to generate report. Check terminal logs.', 'error');
            button.disabled = false;
            button.classList.remove('loading');
            button.textContent = 'Generate Report';
        });
}

function closeDetailPanel() {
    document.getElementById('detail-panel').classList.remove('open');
    document.getElementById('detail-overlay').classList.remove('open');
    selectedIncidentId = null;
    populateIncidentTable();
}


function getStatusClass(status) {
    switch (status) {
        case 'OPEN': return 'status-open';
        case 'TRIAGING': return 'status-triaging';
        case 'IN_PROGRESS': return 'status-in-progress';
        case 'ESCALATED': return 'status-escalated';
        case 'RESOLVED': return 'status-resolved';
        default: return '';
    }
}

function getStatusDisplay(status) {
    switch (status) {
        case 'OPEN': return 'Open';
        case 'TRIAGING': return 'Triaging';
        case 'IN_PROGRESS': return 'In Progress';
        case 'ESCALATED': return 'Escalated';
        case 'RESOLVED': return 'Resolved';
        default: return status;
    }
}

function setupEventListeners() {
    const filterInput = document.getElementById('incident-filter');
    if (filterInput) filterInput.addEventListener('input', function () {
        currentFilterText = this.value;
        populateIncidentTable();
    });

    const filterField = document.getElementById('filter-field');
    if (filterField) filterField.addEventListener('change', function () {
        currentFilterField = this.value;
        populateIncidentTable();
    });

    const clearBtn = document.getElementById('clear-filter');
    if (clearBtn) clearBtn.addEventListener('click', function () {
        currentFilterText = '';
        currentFilterField = 'all';
        currentStatusFilter = null;
        const fi = document.getElementById('incident-filter');
        const ff = document.getElementById('filter-field');
        if (fi) fi.value = '';
        if (ff) ff.value = 'all';
        renderStats();
        populateIncidentTable();
    });

    document.getElementById('detail-close').addEventListener('click', closeDetailPanel);
    document.getElementById('detail-overlay').addEventListener('click', closeDetailPanel);

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') closeDetailPanel();
    });
}

function showToast(message, type) {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type === 'error' ? 'toast-error' : 'toast-success'}`;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(() => {
        toast.classList.add('toast-exit');
        toast.addEventListener('animationend', () => toast.remove());
    }, 3000);
}
