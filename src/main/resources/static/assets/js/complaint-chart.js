// complaint-chart.js
(() => {
    'use strict';

    // === 1. Overall Complaint Stats ===
    function initComplaintStats() {
        const formId = 'complaint-stats-form';
        const fromId = 'complaint-stats-from';
        const toId = 'complaint-stats-to';
        const url = `${baseUrl}/api/dashboards/status-count`;

        function toDateTimeLocal(date) {
            const pad = n => n.toString().padStart(2, '0');
            return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
        }

        function setDefaults() {
            const now = new Date();
            const from = new Date(now);
            from.setHours(0, 0, 0, 0);
            from.setDate(now.getDate() - 6);
            const to = new Date(now);
            to.setHours(23, 59, 59, 999);
            document.getElementById(fromId).value = toDateTimeLocal(from);
            document.getElementById(toId).value = toDateTimeLocal(to);
        }

        function fetchStats(from, to) {
            const params = new URLSearchParams();
            if (from) params.append('from', from);
            if (to) params.append('to', to);
            fetch(url + (params.toString() ? '?' + params : ''))
                .then(r => r.json())
                .then(data => {
                    document.getElementById('complaint-stats-total').textContent = data.totalAllComplaints || 0;
                    document.getElementById('complaint-stats-open').textContent = data.totalOpen || 0;
                    document.getElementById('complaint-stats-closed').textContent = data.totalClosed || 0;
                    document.getElementById('complaint-stats-pending').textContent = data.totalPending || 0;
                })
                .catch(err => console.error('Fetch error:', err));
        }

        document.getElementById(formId).addEventListener('submit', e => {
            e.preventDefault();
            fetchStats(
                document.getElementById(fromId).value,
                document.getElementById(toId).value
            );
        });

        setDefaults();
        const from = document.getElementById(fromId).value;
        const to = document.getElementById(toId).value;
        fetchStats(from, to);
    }

    // === 2. Complaint Chart (Daily/Monthly Bar Chart) ===
    function initComplaintChart() {
        let chart = null;
        const formId = 'complaint-chart-form';
        const fromId = 'complaint-from';
        const toId = 'complaint-to';
        const yearSelectId = 'complaint-year-select';
        const yearSelectorDiv = 'complaint-year-selector';

        function formatDate(date) {
            return date.toISOString().split('T')[0];
        }

        function renderChart(labels, open, closed, pending, title) {
            const ctx = document.getElementById('complaint-bar-chart').getContext('2d');
            if (chart) chart.destroy();
            const totalMax = Math.max(...labels.map((_, i) => (open[i] || 0) + (closed[i] || 0) + (pending[i] || 0)), 0);
            let stepSize = 5;
            if (totalMax > 500) stepSize = Math.ceil(totalMax / 10 / 10) * 10;
            else if (totalMax > 200) stepSize = 50;
            else if (totalMax > 100) stepSize = 20;
            else if (totalMax > 50) stepSize = 10;

            const yAxisMax = Math.ceil(totalMax / stepSize) * stepSize;

            chart = new Chart(ctx, {
                type: 'bar',
                data: { labels, datasets: [
                    { label: "Open", backgroundColor: '#fdaf4b', data: open },
                    { label: "Closed", backgroundColor: '#59d05d', data: closed },
                    { label: "Pending", backgroundColor: '#d9534f', data: pending }
                ]},
                options: {
                    responsive: true, maintainAspectRatio: false,
                    scales: { x: { stacked: true }, y: { stacked: true, beginAtZero: true, max: yAxisMax, ticks: { stepSize } } },
                    plugins: { tooltip: { mode: 'index', intersect: false } }
                }
            });
        }

        function populateYearSelector(selectId, selectedYear) {
            const select = document.getElementById(selectId);
            const currentYear = new Date().getFullYear();
            select.innerHTML = '';
            for (let y = currentYear - 10; y <= currentYear + 1; y++) {
                const opt = new Option(y, y);
                if (y === selectedYear) opt.selected = true;
                select.appendChild(opt);
            }
        }

        function updateChart(mode, from, to, year) {
            let url, labels = [], open = [], closed = [], pending = [];
            if (mode === 'yearly') {
                url = `/api/dashboards/monthly-complaint${year ? '?year=' + year : ''}`;
                fetch(url).then(r => r.json()).then(data => {
                    const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
                    labels = Array(12).fill('');
                    open = Array(12).fill(0); closed = Array(12).fill(0); pending = Array(12).fill(0);
                    data.forEach(d => {
                        const i = d.month - 1;
                        if (i >= 0 && i < 12) {
                            open[i] = d.open || 0;
                            closed[i] = d.closed || 0;
                            pending[i] = d.pending || 0;
                        }
                    });
                    months.forEach((m, i) => labels[i] = months[i]);
                    renderChart(labels, open, closed, pending, 'Monthly Ticket Summary');
                });
            } else {
                url = `/api/dashboards/daily-complaint?from=${from}&to=${to}`;
                fetch(url).then(r => r.json()).then(data => {
                    labels = data.map(d => d.date);
                    open = data.map(d => d.open || 0);
                    closed = data.map(d => d.closed || 0);
                    pending = data.map(d => d.pending || 0);
                    renderChart(labels, open, closed, pending, 'Daily Ticket Summary');
                });
            }
        }

        const now = new Date();
        const from = new Date(now); from.setDate(now.getDate() - 6);
        document.getElementById(fromId).value = formatDate(from);
        document.getElementById(toId).value = formatDate(now);
        populateYearSelector(yearSelectId, now.getFullYear());

        document.querySelectorAll(`#${formId} .btn[data-range]`).forEach(btn => {
            btn.addEventListener('click', () => {
                const range = btn.getAttribute('data-range');
                const now = new Date();
                if (range === 'weekly') {
                    document.getElementById(yearSelectorDiv).style.display = 'none';
                    const f = new Date(now); f.setDate(now.getDate() - 6);
                    document.getElementById(fromId).value = formatDate(f);
                    document.getElementById(toId).value = formatDate(now);
                    updateChart('daily', formatDate(f), formatDate(now));
                } else if (range === 'monthly') {
                    document.getElementById(yearSelectorDiv).style.display = 'none';
                    const f = new Date(now.getFullYear(), now.getMonth(), 1);
                    document.getElementById(fromId).value = formatDate(f);
                    document.getElementById(toId).value = formatDate(now);
                    updateChart('daily', formatDate(f), formatDate(now));
                } else if (range === 'yearly') {
                    document.getElementById(yearSelectorDiv).style.display = 'inline-block';
                    const year = document.getElementById(yearSelectId).value;
                    updateChart('yearly', null, null, year);
                }
            });
        });

        document.getElementById(yearSelectId).addEventListener('change', () => {
            const year = document.getElementById(yearSelectId).value;
            updateChart('yearly', null, null, year);
        });

        document.getElementById(formId).addEventListener('submit', e => {
            e.preventDefault();
            const from = document.getElementById(fromId).value;
            const to = document.getElementById(toId).value;
            document.getElementById(yearSelectorDiv).style.display = 'none';
            updateChart('daily', from, to);
        });

        updateChart('daily', formatDate(from), formatDate(now));
    }

    // === 3. Engineers Responsibility ===
    function initEngineerResponsibility() {
        let currentFrom = null, currentTo = null;
        const containerId = 'engineer-list-container';
        const prevBtnId = 'prevEngineerBtn';
        const nextBtnId = 'nextEngineerBtn';
        const refreshBtnId = 'refreshEngineerBtn';
        const pageSize = 7;
        let currentPage = 1;
        let allData = [];

        async function fetchData() {
            try {
                const url = `${baseUrl}/api/dashboards/assignee-daily-status`;
                const params = new URLSearchParams();
                if (currentFrom) params.append('from', currentFrom);
                if (currentTo) params.append('to', currentTo);
                const response = await fetch(url + (params.toString() ? '?' + params : ''));
                if (!response.ok) throw new Error();
                allData = await response.json();
                renderPage(currentPage);
            } catch (err) {
                document.getElementById(containerId).innerHTML = '<div class="text-center text-muted py-3">Failed to load</div>';
            }
        }

        function renderPage(page) {
            const container = document.getElementById(containerId);
            container.innerHTML = '';
            const start = (page - 1) * pageSize;
            const end = start + pageSize;
            const pageData = allData.slice(start, end);
            if (pageData.length === 0) {
                container.innerHTML = '<div class="text-center text-muted py-3">No data</div>';
                return;
            }
            pageData.forEach((item, index) => {
                const rank = start + index + 1;
                const div = document.createElement('div');
                div.className = 'd-flex align-items-center';
                div.style.minHeight = '50px';
                div.innerHTML = `
                    <div class="mr-3 d-flex align-items-center" style="min-width: 28px;">
                        <span class="fw-bold" style="font-size: 0.95rem; color: #555;">${rank}</span>
                    </div>
                    <div class="flex-1 ml-2 pt-1">
                        <h6 class="fw-bold mb-1">${item.assignee}</h6>
                        <small class="text-muted">${item.totalOpen} open, ${item.totalClosed} closed</small>
                    </div>
                `;
                container.appendChild(div);
                const sep = document.createElement('div');
                sep.className = 'separator-dashed';
                container.appendChild(sep);
            });
        }

        function shiftRange(dir) {
            const now = new Date();
            const to = currentTo ? new Date(currentTo) : new Date();
            const from = currentFrom ? new Date(currentFrom) : new Date(to);
            from.setDate(from.getDate() + dir * 7);
            to.setDate(to.getDate() + dir * 7);
            currentFrom = from.toISOString().split('T')[0];
            currentTo = to.toISOString().split('T')[0];
            currentPage = 1;
            fetchData();
        }

        document.getElementById(prevBtnId)?.addEventListener('click', () => shiftRange(-1));
        document.getElementById(nextBtnId)?.addEventListener('click', () => shiftRange(1));
        document.getElementById(refreshBtnId)?.addEventListener('click', () => {
            currentFrom = null; currentTo = null; currentPage = 1; fetchData();
        });

        fetchData();
    }

    // Initialize
    window.addEventListener('DOMContentLoaded', () => {
        initComplaintStats();
        initComplaintChart();
        initEngineerResponsibility();
    });

})();