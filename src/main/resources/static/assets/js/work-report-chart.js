// work-report-chart.js
(() => {
    'use strict';

    // === 1. Work Report Chart (Multi-line) ===
    function initWorkReportChart() {
        let chart = null;
        const formId = 'wr-chart-form';
        const fromId = 'wr-from';
        const toId = 'wr-to';
        const equipId = 'wr-equipment';
        const yearSelectId = 'wr-year-select';
        const yearSelectorDiv = 'wr-year-selector';
        const canvasId = 'work-report-line-chart';

        function formatDate(date) {
            return date.toISOString().split('T')[0];
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

        async function fetchEquipmentList() {
            try {
                const response = await fetch(`${baseUrl}/api/dashboards/equipment-complaint-count`);
                if (!response.ok) throw new Error();
                const data = await response.json();
                const select = document.getElementById(equipId);
                select.innerHTML = '<option value="">All Equipment</option>';
                data.forEach(eq => {
                    const opt = new Option(eq.equipmentName, eq.equipmentCode);
                    select.appendChild(opt);
                });
            } catch (err) {
                console.error('Load equipment failed:', err);
            }
        }

        function updateChart(mode, from, to, year) {
            const ctx = document.getElementById(canvasId).getContext('2d');
            if (chart) chart.destroy();
            let url = '', isMonthly = false;
            const equipCode = document.getElementById(equipId).value;
            if (mode === 'yearly') {
                url = `${baseUrl}/api/dashboards/monthly-work-report?year=${year}`;
                if (equipCode) url += `&equipmentCode=${encodeURIComponent(equipCode)}`;
                isMonthly = true;
            } else {
                url = `${baseUrl}/api/dashboards/daily-work-report?from=${from}&to=${to}`;
                if (equipCode) url += `&equipmentCode=${encodeURIComponent(equipCode)}`;
            }

            fetch(url).then(r => r.json()).then(data => {
                const labels = isMonthly ? ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"] : data.map(d => d.date);
                const cData = Array(labels.length).fill(0), pData = Array(labels.length).fill(0), bData = Array(labels.length).fill(0), oData = Array(labels.length).fill(0);
                data.forEach(d => {
                    const i = isMonthly ? d.month - 1 : labels.indexOf(d.date);
                    if (i >= 0) {
                        cData[i] = d.correctiveMaintenanceCount || 0;
                        pData[i] = d.preventiveMaintenanceCount || 0;
                        bData[i] = d.breakdownCount || 0;
                        oData[i] = d.otherCount || 0;
                    }
                });

                chart = new Chart(ctx, {
                    type: 'line',
                    data: { labels, datasets: [
                        { label: "Corrective", borderColor: "#e74c3c", pointBackgroundColor: "#e74c3c", fill: false, borderWidth: 2, data: cData },
                        { label: "Preventive", borderColor: "#3498db", pointBackgroundColor: "#3498db", fill: false, borderWidth: 2, data: pData },
                        { label: "Breakdown", borderColor: "#f39c12", pointBackgroundColor: "#f39c12", fill: false, borderWidth: 2, data: bData },
                        { label: "Other", borderColor: "#95a5a6", pointBackgroundColor: "#95a5a6", fill: false, borderWidth: 2, data: oData }
                    ]},
                    options: {
                        responsive: true, maintainAspectRatio: false,
                        scales: {
                            y: { beginAtZero: true, ticks: { stepSize: 1 }, title: { display: true, text: 'Reports' } }
                        },
                        tooltips: { callbacks: { label: (t, d) => `${d.datasets[t.datasetIndex].label}: ${t.parsed.y} reports` } }
                    }
                });
            });
        }

        const now = new Date();
        const from = new Date(now); from.setDate(now.getDate() - 6);
        document.getElementById(fromId).value = formatDate(from);
        document.getElementById(toId).value = formatDate(now);
        populateYearSelector(yearSelectId, now.getFullYear());

        document.querySelectorAll(`#${formId} .btn[data-range]`).forEach(btn => {
            btn.addEventListener('click', () => {
                const range = btn.getAttribute('data-range');
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
            updateChart('yearly', null, null, document.getElementById(yearSelectId).value);
        });

        document.getElementById(formId).addEventListener('submit', e => {
            e.preventDefault();
            updateChart('daily', document.getElementById(fromId).value, document.getElementById(toId).value);
        });

        document.getElementById(equipId).addEventListener('change', () => {
            const from = document.getElementById(fromId).value;
            const to = document.getElementById(toId).value;
            const year = document.getElementById(yearSelectId).value;
            if (document.getElementById(yearSelectorDiv).style.display === 'inline-block') {
                updateChart('yearly', null, null, year);
            } else {
                updateChart('daily', from, to);
            }
        });

        fetchEquipmentList().then(() => updateChart('daily', formatDate(from), formatDate(now)));
    }

    // === 2. Breakdown Chart ===
    function initBreakdownChart() {
        let chart = null;
        const formId = 'breakdown-chart-form';
        const fromId = 'breakdown-from';
        const toId = 'breakdown-to';
        const yearSelectId = 'breakdown-year-select';
        const yearSelectorDiv = 'breakdown-year-selector';

        function formatDate(date) {
            return date.toISOString().split('T')[0];
        }

        function updateChart(mode, from, to, year) {
            const ctx = document.getElementById('breakdown-line-chart').getContext('2d');
            if (chart) chart.destroy();
            let url = mode === 'yearly'
                ? `${baseUrl}/api/dashboards/monthly-breakdown?year=${year}`
                : `${baseUrl}/api/dashboards/daily-breakdown?from=${from}&to=${to}`;

            fetch(url).then(r => r.json()).then(data => {
                const labels = mode === 'yearly'
                    ? ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]
                    : data.map(d => d.date);
                const values = mode === 'yearly'
                    ? Array(12).fill(0).map((_, i) => (data.find(d => d.month === i + 1) || {}).totalResolutionTimeMinutes || 0)
                    : data.map(d => d.totalResolutionTimeMinutes || 0);
                const counts = mode === 'yearly'
                    ? Array(12).fill(0).map((_, i) => (data.find(d => d.month === i + 1) || {}).breakdownCount || 0)
                    : data.map(d => d.breakdownCount || 0);

                chart = new Chart(ctx, {
                    type: 'line',
                    data: { labels, datasets: [{
                        label: "Total Resolution Time (min)",
                        borderColor: "#1d7af3",
                        pointBackgroundColor: "#1d7af3",
                        fill: true,
                        borderWidth: 2,
                        data: values,
                        counts: counts
                    }] },
                    options: {
                        tooltips: {
                            callbacks: {
                                label: (t, d) => [ `Total Time: ${t.parsed.y} min`, `Count: ${d.datasets[0].counts[t.dataIndex]}` ]
                            }
                        }
                    }
                });
            });
        }

        const now = new Date();
        const from = new Date(now); from.setDate(now.getDate() - 6);
        document.getElementById(fromId).value = formatDate(from);
        document.getElementById(toId).value = formatDate(now);
        (() => {
            const select = document.getElementById(yearSelectId);
            const currentYear = new Date().getFullYear();
            select.innerHTML = '';
            for (let y = currentYear - 10; y <= currentYear + 1; y++) {
                const opt = new Option(y, y);
                if (y === currentYear) opt.selected = true;
                select.appendChild(opt);
            }
        })();

        document.querySelectorAll(`#${formId} .btn[data-range]`).forEach(btn => {
            btn.addEventListener('click', () => {
                const range = btn.getAttribute('data-range');
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
            updateChart('yearly', null, null, document.getElementById(yearSelectId).value);
        });

        document.getElementById(formId).addEventListener('submit', e => {
            e.preventDefault();
            updateChart('daily', document.getElementById(fromId).value, document.getElementById(toId).value);
        });

        updateChart('daily', formatDate(from), formatDate(now));
    }

    window.addEventListener('DOMContentLoaded', () => {
        initWorkReportChart();
        initBreakdownChart();
    });

})();