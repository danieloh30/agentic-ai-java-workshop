let currentSortColumn = 'id';
let currentSortDirection = 'asc';
let incidentsData = [];
let currentFilterText = '';
let currentFilterField = 'all';
let currentStatusFilter = null;
let lastUpdatedIncidentId = null;
let selectedIncidentId = null;
let workshopMode = localStorage.getItem('part2-mode') || 'PLAN';
const incidentResults = new Map();
const pendingIncidents = new Set();

document.addEventListener('DOMContentLoaded', function () {
    initTheme();
    initWorkshopMode();
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
    return fetch('/incidents')
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
            <td class="description-cell"><div class="description-preview"></div></td>
            <td><span class="status-indicator ${statusClass}"><span class="status-dot"></span><span class="status-text">${getStatusDisplay(incident.status)}</span></span></td>
            <td><button class="btn-view" onclick="openDetailPanel(${incident.id}); event.stopPropagation();">View</button></td>
        `;
        const description = typeof incident.description === 'string' && incident.description.trim()
                ? incident.description : 'N/A';
        row.querySelector('.description-preview').textContent = description.replace(/\s+/g, ' ').trim();
        if (description.length > 180) {
            const hint = document.createElement('span');
            hint.className = 'description-hint';
            hint.textContent = 'Open to read full description';
            row.querySelector('.description-cell').appendChild(hint);
        }
        row.querySelector('.btn-view').setAttribute('aria-label', `View incident #${incident.id} and full description`);
        row.addEventListener('click', () => openDetailPanel(incident.id));
        tbody.appendChild(row);
    });
}

function renderDescription(container, description) {
    const text = typeof description === 'string' && description.trim() ? description : 'N/A';
    const blocks = text.startsWith('Plan:') ? text.split(/\r?\n[ \t]*\r?\n/) : [text];
    let section = null;

    for (const block of blocks) {
        if (!block.trim()) continue;
        const match = text.startsWith('Plan:')
                ? block.match(/^(Plan|Diagnosis|Mitigation|Verification|Communication|Resolved):[ \t]*([\s\S]*)$/)
                : null;
        if (match) {
            section = document.createElement('section');
            section.className = 'description-section';
            const heading = document.createElement('h4');
            heading.textContent = match[1];
            section.appendChild(heading);
            container.appendChild(section);
        }
        const paragraph = document.createElement('p');
        paragraph.textContent = match ? match[2] : block;
        (section || container).appendChild(paragraph);
    }
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
    const canProcess = ['OPEN', 'TRIAGING', 'IN_PROGRESS'].includes(incident.status);

    let formHtml = '';
    if (canProcess) {
        formHtml = `
            <div class="detail-divider"></div>
            <div class="detail-form-title">Process Incident</div>
            <textarea id="detail-report" class="detail-textarea" placeholder="Enter incident report details..."></textarea>
            <button class="btn-process" id="detail-process-btn" onclick="processFromPanel(${incident.id}, '${incident.status}')">Process Incident</button>
        `;
    } else {
        formHtml = `
            <div class="detail-divider"></div>
            <div class="detail-resolved-msg">This incident has been ${incident.status === 'RESOLVED' ? 'resolved' : 'escalated'}.</div>
        `;
    }

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
        ${typeof incident.description === 'string' && incident.description.startsWith('Outcome:') ? `
        <details class="detail-field saved-planning">
            <summary>Saved planning result</summary>
            <div id="detail-description" class="detail-value detail-description"></div>
        </details>` : `
        <div class="detail-field">
            <div class="detail-label">Description</div>
            <div id="detail-description" class="detail-value detail-description"></div>
        </div>`}
        <div class="detail-field" id="detail-result-field" hidden>
            <div class="detail-label" id="detail-result-label"></div>
            <pre id="detail-result" class="detail-result" aria-live="polite"></pre>
        </div>
        ${formHtml}
    `;

    renderDescription(document.getElementById('detail-description'), incident.description);
    const labels = {PLAN:'Planning', MEMORY:'Assistant reply', VOTING:'Voting outcome', EVENTS:'Processing outcome'};
    const key = `${workshopMode}:${incidentId}`;
    document.getElementById('detail-result-label').textContent = labels[workshopMode];
    if (incidentResults.has(key)) showWorkshopResult(incidentResults.get(key));
    if (workshopMode === 'MEMORY') {
        document.querySelector('.detail-form-title')?.replaceChildren('Conversation');
        const button = document.getElementById('detail-process-btn');
        if (button) button.textContent = 'Send Message';
    }
    if (workshopMode === 'EVENTS') loadProcessingOutcome(incidentId);
    document.getElementById('detail-panel').classList.add('open');
    document.getElementById('detail-overlay').classList.add('open');
}

function closeDetailPanel() {
    document.getElementById('detail-panel').classList.remove('open');
    document.getElementById('detail-overlay').classList.remove('open');
    selectedIncidentId = null;
    populateIncidentTable();
}

function initWorkshopMode() {
    const selector = document.getElementById('workshop-mode');
    if (!['PLAN','MEMORY','VOTING','EVENTS'].includes(workshopMode)) workshopMode='PLAN';
    selector.value=workshopMode;
    selector.addEventListener('change', () => {
        workshopMode=selector.value;
        localStorage.setItem('part2-mode',workshopMode);
        updateWorkshopHelp();
        if(selectedIncidentId!==null) openDetailPanel(selectedIncidentId);
    });
    updateWorkshopHelp();
}

function updateWorkshopHelp() {
    const help=document.getElementById('workshop-mode-help');
    const descriptions={
        PLAN:'Plan specialist work, inspect feedback, then add required MCP evidence in Exercise 10.',
        MEMORY:'Send follow-up messages for one incident. Inspect the stored window and its eviction boundary.',
        VOTING:'Compare specialist ballots. A recommendation requires sufficient agreement.',
        EVENTS:'Submit work asynchronously. Reopen the incident to inspect its persisted outcome and request ID.'
    };
    help.textContent=descriptions[workshopMode];
    if(workshopMode==='VOTING') {
        const links=document.createElement('span');links.className='workshop-fixtures';
        for(const scenario of ['agreement','disagreement']) {
            const link=document.createElement('a');link.href=`/incident-consensus/fixtures/${scenario}`;
            link.target='_blank';link.rel='noopener';link.textContent=`Inspect ${scenario} fixture`;links.append(link);
        }
        help.append(links);
    }
}

function showWorkshopResult(text) {
    document.getElementById('detail-result-field').hidden=false;
    document.getElementById('detail-result').textContent=text;
}

async function loadProcessingOutcome(incidentId) {
    try {
        const response=await fetch(`/incident-events/resolutions/${incidentId}`);
        if(selectedIncidentId!==incidentId || workshopMode!=='EVENTS') return;
        if(response.status===404) return;
        if(!response.ok) throw new Error('Unable to load processing outcome');
        const outcome=await response.json();
        if(selectedIncidentId!==incidentId || workshopMode!=='EVENTS') return;
        showWorkshopResult(`${outcome.processingStatus}\nRequest: ${outcome.requestId}\n${outcome.triage || ''}\n${outcome.resolution || 'Processing is pending; close and reopen to refresh.'}`);
    } catch(error) {
        if(selectedIncidentId===incidentId && workshopMode==='EVENTS') showWorkshopResult('Unable to load the processing outcome. Reopen to retry.');
    }
}

async function processFromPanel(incidentId) {
    if(pendingIncidents.has(incidentId)) return;
    const report=document.getElementById('detail-report').value.trim();
    if(!report) { showToast('Enter a report or message.','error'); return; }
    const mode=workshopMode;
    pendingIncidents.add(incidentId);
    const button=document.getElementById('detail-process-btn');
    button.disabled=true;button.textContent='Processing...';
    try {
        const response=await fetch(`/incident-management/process/${incidentId}?mode=${mode}&report=${encodeURIComponent(report)}`,{method:'POST'});
        if(!response.ok) throw new Error('Processing failed');
        const result=await response.text();
        incidentResults.set(`${mode}:${incidentId}`,result);
        showToast(mode==='EVENTS'?'Request submitted; processing continues.':'Result ready.');
        await loadAllIncidents();
        if(selectedIncidentId===incidentId && workshopMode===mode) {
            openDetailPanel(incidentId);
            showWorkshopResult(result);
        }
    } catch(error) {
        showToast('Request failed. Your report is retained; try again.','error');
    } finally {
        pendingIncidents.delete(incidentId);
        if(button.isConnected) { button.disabled=false;button.textContent=mode==='MEMORY'?'Send Message':'Process Incident'; }
    }
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
