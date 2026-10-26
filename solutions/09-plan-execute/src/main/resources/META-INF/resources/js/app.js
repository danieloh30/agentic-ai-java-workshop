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
            <td class="description-cell"><div class="description-preview"></div></td>
            <td><span class="status-indicator ${statusClass}"><span class="status-dot"></span><span class="status-text">${getStatusDisplay(incident.status)}</span></span></td>
            <td><button class="btn-view" onclick="openDetailPanel(${incident.id}); event.stopPropagation();">View</button></td>
        `;
        const description = typeof incident.description === 'string' && incident.description.trim()
                ? incident.description : 'N/A';
        row.querySelector('.description-preview').textContent = isPlanningSummary(description)
            ? `${description.split('\n')[0].slice(9).replaceAll('_', ' ')} · Open to inspect the plan and review history`
            : description.replace(/\s+/g, ' ').trim();
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

// Persisted planning summaries stay readable after a reload, without changing the API.
function isPlanningSummary(text) {
    return typeof text === 'string' && text.startsWith('Outcome:') &&
        /^Deployment evidence \(workshop fixture\):/m.test(text);
}

function planningElement(tag, className, text) {
    const element = document.createElement(tag);
    if (className) element.className = className;
    if (text !== undefined) element.textContent = text;
    return element;
}

function planningSteps(container, text) {
    const match = text.match(/^\[([^\]]*)\](?:\s*—\s*)?([\s\S]*)$/);
    if (!match) {
        container.append(planningElement('p', 'planning-copy', text));
        return;
    }
    const steps = planningElement('ol', 'planning-steps');
    for (const step of match[1].split(',').map(s => s.trim()).filter(Boolean)) {
        steps.append(planningElement('li', '', step));
    }
    container.append(steps);
    if (match[2]) container.append(planningElement('p', 'planning-copy', match[2]));
}

function renderPlanningResult(container, text) {
    if (!isPlanningSummary(text)) return false;
    container.replaceChildren();
    container.classList.add('planning-result');
    const parts = {};
    const headings = /^(Outcome|Deployment evidence \(workshop fixture\)|Iteration history|Plan|Diagnosis|Mitigation|Verification|Communication|Recommendation accepted by reviewer):[ \t]*/gm;
    const matches = [...text.matchAll(headings)];
    matches.forEach((match, index) => {
        parts[match[1]] = text.slice(match.index + match[0].length, matches[index + 1]?.index ?? text.length).trim();
    });
    const rounds = [...(parts['Iteration history'] || '').matchAll(/^Round (\d+): (Plan accepted|Plan rejected)\n([\s\S]*?)(?=^Round \d+:|$(?![\s\S]))/gm)];
    const outcome = parts.Outcome;
    const ready = outcome === 'RECOMMENDATION_READY';
    const unavailable = outcome === 'EVIDENCE_UNAVAILABLE';
    const hero = planningElement('section', `planning-outcome ${ready ? 'is-ready' : 'needs-review'}`);
    const topline = planningElement('div', 'planning-topline');
    topline.append(planningElement('span', 'planning-eyebrow', 'PLAN → EXECUTE → REVIEW'));
    if (rounds.length) topline.append(planningElement('span', 'planning-count', `${rounds.length} ${rounds.length === 1 ? 'iteration' : 'iterations'}`));
    hero.append(topline, planningElement('h3', '', ready ? 'Recommendation ready' : unavailable ? 'Evidence unavailable' : 'Operator review needed'),
        planningElement('p', '', ready ? 'Reviewer accepted the recommendation. Production verification is still required.' : unavailable ? 'Required deployment evidence could not be validated. Planning did not run.' : 'The run finished without an accepted recommendation. Inspect the feedback before acting.'));
    container.append(hero);

    const evidenceLines = (parts['Deployment evidence (workshop fixture)'] || '').split('\n');
    const evidenceStatus = evidenceLines.shift();
    const evidence = planningElement('section', 'planning-card');
    const evidenceHeading = planningElement('div', 'planning-topline');
    evidenceHeading.append(planningElement('h4', '', 'Deployment evidence'), planningElement('span', 'planning-chip', evidenceStatus === 'not_requested' ? 'Not fetched' : evidenceStatus));
    evidence.append(evidenceHeading, planningElement('p', 'planning-caption', evidenceStatus === 'not_requested' ? 'No external lookup yet · incident facts only' : 'Workshop fixture · no production recovery has been observed'));
    for (const line of evidenceLines) if (line.trim()) evidence.append(planningElement('p', 'planning-copy', line));
    container.append(evidence);

    if (rounds.length) {
        const history = planningElement('section', 'planning-history');
        history.append(planningElement('h4', '', 'Review timeline'), planningElement('p', 'planning-caption', 'Plan accepted means prerequisite validation passed; reviewer feedback can still require another round.'));
        for (const round of rounds) {
            const item = planningElement('details', 'planning-round');
            const summary = planningElement('summary', '');
            summary.append(planningElement('span', '', `Round ${round[1]}`), planningElement('span', `planning-chip ${round[2] === 'Plan rejected' ? 'is-warning' : ''}`, round[2]));
            item.append(summary);
            const body = planningElement('div', 'planning-round-body');
            const feedbackIndex = round[3].indexOf('Feedback:');
            const plan = (feedbackIndex < 0 ? round[3] : round[3].slice(0, feedbackIndex)).trim();
            if (plan) planningSteps(body, plan);
            if (feedbackIndex >= 0) {
                body.append(planningElement('h5', '', 'Reviewer feedback'), planningElement('p', 'planning-copy', round[3].slice(feedbackIndex + 9).trim()));
            }
            item.append(body); history.append(item);
        }
        container.append(history);
    }
    if (parts.Plan) {
        const plan = planningElement('section', 'planning-card');
        plan.append(planningElement('h4', '', 'Latest specialist plan'));
        planningSteps(plan, parts.Plan); container.append(plan);
    }
    const specialists = planningElement('section', 'planning-specialists');
    for (const [index, name] of ['Diagnosis', 'Mitigation', 'Verification', 'Communication'].entries()) {
        if (!parts[name]) continue;
        const card = planningElement('details', 'planning-specialist');
        const summary = planningElement('summary', '');
        summary.append(planningElement('span', 'planning-step-number', String(index + 1)), planningElement('span', '', name));
        card.append(summary, planningElement('p', 'planning-copy', parts[name]));
        specialists.append(card);
    }
    if (specialists.children.length) {
        specialists.prepend(planningElement('h4', '', 'Specialist recommendations'));
        container.append(specialists);
    }
    const raw = planningElement('details', 'planning-raw');
    raw.append(planningElement('summary', '', 'View original saved result'), planningElement('pre', '', text));
    container.append(raw);
    return true;
}

function renderDescription(container, description) {
    if (renderPlanningResult(container, description)) return;
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
        <div class="incident-metadata">
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
        </div>
        <div class="detail-field">
            <div class="detail-label">${isPlanningSummary(incident.description) ? 'Planning result' : 'Description'}</div>
            <div id="detail-description" class="detail-value detail-description"></div>
        </div>
        ${formHtml}
    `;

    renderDescription(document.getElementById('detail-description'), incident.description);
    document.getElementById('detail-panel').classList.add('open');
    document.getElementById('detail-overlay').classList.add('open');
}

function closeDetailPanel() {
    document.getElementById('detail-panel').classList.remove('open');
    document.getElementById('detail-overlay').classList.remove('open');
    selectedIncidentId = null;
    populateIncidentTable();
}

function processFromPanel(incidentId, status) {
    const report = document.getElementById('detail-report').value;
    const button = document.getElementById('detail-process-btn');

    button.disabled = true;
    button.classList.add('loading');
    button.textContent = 'Processing...';

    const statusLabels = { 'OPEN': 'open incident', 'TRIAGING': 'triage', 'IN_PROGRESS': 'investigation' };

    fetch(`/incident-management/process/${incidentId}?report=${encodeURIComponent(report)}`, { method: 'POST' })
        .then(response => {
            if (!response.ok) throw new Error('Network response was not ok');
            return response.text();
        })
        .then(() => {
            lastUpdatedIncidentId = incidentId;
            showToast(`Incident successfully processed from ${statusLabels[status]}`);
            closeDetailPanel();
            loadAllIncidents();
        })
        .catch(error => {
            console.error(`Error processing incident from ${statusLabels[status]}:`, error);
            showToast(`Failed to process ${statusLabels[status]}. Please try again.`, 'error');
            button.disabled = false;
            button.classList.remove('loading');
            button.textContent = 'Process Incident';
        });
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
