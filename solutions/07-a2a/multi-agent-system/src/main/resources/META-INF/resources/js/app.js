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
    startApprovalPolling();
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
            populateIncidentStatusTable();
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
            populateIncidentStatusTable();
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
            populateIncidentStatusTable();
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

function populateIncidentStatusTable() {
    const tbody = document.getElementById('incident-status-table-body');
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
        const priorityLabel = 'P' + incident.priority;

        row.innerHTML = `
            <td><span style="color:var(--accent);font-weight:600">#${incident.id}</span></td>
            <td>${incident.system}</td>
            <td>${incident.service}</td>
            <td><span class="priority-badge priority-${incident.priority}">${priorityLabel}</span></td>
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
    populateIncidentStatusTable();

    const body = document.getElementById('detail-body');
    const title = document.getElementById('detail-title');
    title.textContent = `Incident #${incident.id}`;

    const statusClass = getStatusClass(incident.status);
    const priorityLabel = 'P' + incident.priority;
    const canProcess = ['OPEN', 'TRIAGING', 'IN_PROGRESS'].includes(incident.status);

    let formHtml = '';
    if (canProcess) {
        formHtml = `
            <div class="detail-divider"></div>
            <div class="detail-form-title">Process Incident</div>
            <textarea id="detail-report" class="detail-textarea" placeholder="Enter incident report details..."></textarea>
            <input type="file" id="detail-log-image" accept="image/*" class="detail-file-input">
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
            <div class="detail-value"><span class="priority-badge priority-${incident.priority}">${priorityLabel}</span></div>
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
        ${formHtml}
    `;

    document.getElementById('detail-panel').classList.add('open');
    document.getElementById('detail-overlay').classList.add('open');
}

function closeDetailPanel() {
    document.getElementById('detail-panel').classList.remove('open');
    document.getElementById('detail-overlay').classList.remove('open');
    selectedIncidentId = null;
    populateIncidentStatusTable();
}

function processFromPanel(incidentId, status) {
    const report = document.getElementById('detail-report').value;
    const button = document.getElementById('detail-process-btn');
    const imageInput = document.getElementById('detail-log-image');

    button.disabled = true;
    button.classList.add('loading');
    button.textContent = 'Processing...';

    const statusLabels = { 'OPEN': 'open incident', 'TRIAGING': 'triage', 'IN_PROGRESS': 'investigation' };

    const formData = new FormData();
    formData.append('report', report);
    if (imageInput && imageInput.files.length > 0) {
        formData.append('logImage', imageInput.files[0]);
    }

    fetch(`/incident-management/process/${incidentId}`, { method: 'POST', body: formData })
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
        populateIncidentStatusTable();
    });

    const filterField = document.getElementById('filter-field');
    if (filterField) filterField.addEventListener('change', function () {
        currentFilterField = this.value;
        populateIncidentStatusTable();
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
        populateIncidentStatusTable();
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

// ============================================================================
// HUMAN-IN-THE-LOOP APPROVAL FUNCTIONS
// ============================================================================

let approvalPollingInterval = null;
let lastApprovalCount = 0;
let isModalOpen = false;

async function loadPendingApprovals() {
    try {
        const response = await fetch('/api/approvals/pending');
        const proposals = await response.json();

        const floatBtn = document.getElementById('approval-notification-btn');
        const countBadge = floatBtn.querySelector('.approval-count-badge');

        if (proposals.length > lastApprovalCount && lastApprovalCount >= 0 && proposals.length > 0) {
            showBrowserNotification('Approval Required',
                `${proposals.length} incident escalation${proposals.length > 1 ? 's' : ''} awaiting your approval`);
        }
        lastApprovalCount = proposals.length;

        if (proposals.length > 0) {
            floatBtn.style.display = 'flex';
            countBadge.textContent = proposals.length;
        } else {
            floatBtn.style.display = 'none';
            if (isModalOpen) closeApprovalModal();
        }

        if (!isModalOpen) {
            const modalBody = document.getElementById('approval-modal-body');
            if (!proposals || proposals.length === 0) {
                modalBody.innerHTML = '<p style="text-align:center;padding:40px;color:var(--text-muted);">No pending approvals at this time.</p>';
            } else {
                modalBody.innerHTML = '';
                proposals.forEach(p => modalBody.appendChild(createApprovalCard(p)));
            }
        }
    } catch (error) {
        console.error('Error loading pending approvals:', error);
    }
}

function openApprovalModal() {
    isModalOpen = true;
    document.getElementById('approval-modal').style.display = 'flex';
    loadModalContent();
}

function closeApprovalModal() {
    isModalOpen = false;
    document.getElementById('approval-modal').style.display = 'none';
}

async function loadModalContent() {
    try {
        const response = await fetch('/api/approvals/pending');
        const proposals = await response.json();
        const modalBody = document.getElementById('approval-modal-body');
        if (!proposals || proposals.length === 0) {
            modalBody.innerHTML = '<p style="text-align:center;padding:40px;color:var(--text-muted);">No pending approvals at this time.</p>';
        } else {
            modalBody.innerHTML = '';
            proposals.forEach(p => modalBody.appendChild(createApprovalCard(p)));
        }
    } catch (error) {
        console.error('Error loading modal content:', error);
    }
}

function showBrowserNotification(title, body) {
    if (!("Notification" in window)) return;
    if (Notification.permission === "granted") {
        new Notification(title, { body });
    } else if (Notification.permission !== "denied") {
        Notification.requestPermission().then(permission => {
            if (permission === "granted") new Notification(title, { body });
        });
    }
}

function createApprovalCard(proposal) {
    const card = document.createElement('div');
    card.className = 'approval-card';
    card.id = `approval-${proposal.id}`;

    card.innerHTML = `
        <div class="approval-card-header">
            <div class="incident-title">
                <span class="incident-icon">&#9888;</span>
                <h3>${proposal.incidentPriority} - ${proposal.incidentSystem} / ${proposal.incidentService}</h3>
            </div>
            <div class="info-row">
                <span class="info-label">Incident #${proposal.incidentNumber}</span>
                <span class="info-label">${proposal.incidentDescription}</span>
            </div>
        </div>
        <div class="approval-card-body">
            <div class="damage-section">
                <div class="section-title">Business Impact</div>
                <div class="impact-text">${proposal.businessImpact || 'No impact assessment'}</div>
            </div>
            <div class="damage-section">
                <div class="section-title">Incident Report</div>
                <div class="damage-text">${proposal.incidentReport || 'No report provided'}</div>
            </div>
            <div class="proposal-section">
                <div class="section-title">AI Recommendation</div>
                <div class="proposal-action">
                    <span class="action-badge">${proposal.proposedEscalation}</span>
                    ${proposal.escalationReason ? `<span class="action-reason">${proposal.escalationReason}</span>` : ''}
                </div>
            </div>
        </div>
        <div class="approval-card-footer">
            <button class="btn-approve" onclick="handleProposalDecision(${proposal.id}, 'RESOLVE_INCIDENT')">Resolve Incident</button>
            <button class="btn-reject" onclick="handleProposalDecision(${proposal.id}, 'ESCALATE_INCIDENT')">Escalate to Management</button>
        </div>
    `;
    return card;
}

async function handleProposalDecision(proposalId, decision) {
    try {
        const reasonInput = document.getElementById(`reason-${proposalId}`);
        const reason = reasonInput ? reasonInput.value.trim() : '';

        const response = await fetch(`/api/approvals/${proposalId}/decide`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                decision: decision,
                reason: reason || `${decision === 'RESOLVE_INCIDENT' ? 'Resolve incident' : 'Escalate to management'} decision by human reviewer`,
                approvedBy: 'Workshop User'
            })
        });

        if (response.ok) {
            const actionText = decision === 'RESOLVE_INCIDENT' ? 'RESOLVE' : 'ESCALATE';
            showToast(`Decision: ${actionText} - Workflow will complete shortly`);
            const card = document.getElementById(`approval-${proposalId}`);
            if (card) {
                card.style.opacity = '0';
                card.style.transform = 'scale(0.95)';
                setTimeout(() => { card.remove(); loadPendingApprovals(); }, 300);
            }
        } else {
            const error = await response.json();
            showToast(`Error: ${error.error || 'Failed to record decision'}`, 'error');
        }
    } catch (error) {
        console.error('Error handling proposal decision:', error);
        showToast('Error recording decision', 'error');
    }
}

function startApprovalPolling() {
    if ("Notification" in window && Notification.permission === "default") {
        Notification.requestPermission();
    }
    loadPendingApprovals();
    if (approvalPollingInterval) clearInterval(approvalPollingInterval);
    approvalPollingInterval = setInterval(loadPendingApprovals, 2000);
}
