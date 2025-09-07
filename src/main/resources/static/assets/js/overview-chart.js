// Helper Functions

function formatDate(date) {
    return date.toISOString().split('T')[0]; // YYYY-MM-DD
}

function toApiDateTime(dateStr, isEnd = false) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    if (isEnd) {
        date.setHours(23, 59, 59, 999); // 23:59:59.999
    } else {
        date.setHours(0, 0, 0, 0); // 00:00:00.000
    }
    return date.toISOString().slice(0, 16); // "2025-08-01T00:00"
}

// Populate year selector
function populateYearSelector(selectId, selectedYear = null) {
    const select = document.getElementById(selectId);
    const currentYear = new Date().getFullYear();
    select.innerHTML = '';
    for (let y = currentYear - 10; y <= currentYear + 1; y++) {
        const opt = new Option(y, y);
        if (y === selectedYear || (selectedYear === null && y === currentYear)) {
            opt.selected = true;
        }
        select.appendChild(opt);
    }
}

// Overall Complaint
function toDateTimeLocal(date) {
    const pad = (n) => n.toString().padStart(2, '0');
    const year = date.getFullYear();
    const month = pad(date.getMonth() + 1); // 0-indexed
    const day = pad(date.getDate());
    const hours = pad(date.getHours());
    const minutes = pad(date.getMinutes());
    return `${year}-${month}-${day}T${hours}:${minutes}`;
}

function setComplaintStatsDefaults() {
    const now = new Date();
    const startOfDay = new Date(now);
    startOfDay.setHours(0, 0, 0, 0);

    const endOfDay = new Date(now);
    endOfDay.setHours(23, 59, 59, 999);

    const fromInput = document.getElementById('complaint-stats-from');
    const toInput = document.getElementById('complaint-stats-to');

    if (fromInput && toInput) {
        fromInput.value = toDateTimeLocal(startOfDay);
        toInput.value = toDateTimeLocal(endOfDay);
    }
}

function fetchComplaintStats(from, to) {
    let url = `${baseUrl}/api/dashboards/status-count`;
    const params = new URLSearchParams();

    if (from) params.append('from', from);
    if (to) params.append('to', to);

    if (params.toString()) {
        url += '?' + params.toString();
    }

    fetch(url)
        .then(response => {
            if (!response.ok) throw new Error('Network error');
            return response.json();
        })
        .then(data => {
            // Update card values
            document.getElementById('complaint-stats-total').textContent = data.totalAllComplaints || 0;
            document.getElementById('complaint-stats-open').textContent = data.totalOpen || 0;
            document.getElementById('complaint-stats-closed').textContent = data.totalClosed || 0;
            document.getElementById('complaint-stats-pending').textContent = data.totalPending || 0;
        })
        .catch(err => {
            console.error('Fetch error:', err);
        });
}

document.getElementById('complaint-stats-form').addEventListener('submit', function (e) {
    e.preventDefault();
    const from = document.getElementById('complaint-stats-from').value;
    const to = document.getElementById('complaint-stats-to').value;
    fetchComplaintStats(from, to);
});

window.addEventListener('DOMContentLoaded', function () {
    setComplaintStatsDefaults();

    const from = document.getElementById('complaint-stats-from').value;
    const to = document.getElementById('complaint-stats-to').value;

    fetchComplaintStats(from, to);
});

// Complaint Chart
let complaintChart = null;

// Helper: Format Date to YYYY-MM-DD
function formatDate(date) {
    return date.toISOString().split('T')[0];
}

// Helper: Convert to API datetime format (T00:00 or T23:59)
function toApiDateTime(dateStr, isEnd) {
    const d = new Date(dateStr);
    if (isEnd) {
        d.setHours(23, 59, 59, 999);
    } else {
        d.setHours(0, 0, 0, 0);
    }
    return d.toISOString().slice(0, 16); // "YYYY-MM-DDTHH:mm"
}

// Helper: Populate Year Selector
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

// === Update Chart Function ===
function updateComplaintChart(mode, from = null, to = null, year = null) {
    if (mode === 'yearly') {
        const url = `/api/dashboards/monthly-complaint${year ? '?year=' + year : ''}`;
        fetch(url)
            .then(r => r.json())
            .then(data => {
                if (!Array.isArray(data) || data.length === 0) return;

                const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
                const labels = Array(12).fill('');
                const openData = Array(12).fill(0);
                const closedData = Array(12).fill(0);
                const pendingData = Array(12).fill(0);

                data.forEach(d => {
                    const monthIndex = parseInt(d.date.split('-')[1]) - 1;
                    if (monthIndex >= 0 && monthIndex < 12) {
                        labels[monthIndex] = months[monthIndex];
                        openData[monthIndex] = d.open || 0;
                        closedData[monthIndex] = d.closed || 0;
                        pendingData[monthIndex] = d.pending || 0;
                    }
                });

                for (let i = 0; i < 12; i++) {
                    if (!labels[i]) labels[i] = months[i];
                }

                renderComplaintChart(labels, openData, closedData, pendingData, 'Monthly Ticket Summary');
            })
            .catch(err => console.error('Monthly fetch error:', err));

    } else {
        const fromApi = toApiDateTime(from, false); // T00:00
        const toApi = toApiDateTime(to, true);     // T23:59
        const url = `/api/dashboards/daily-complaint?from=${fromApi}&to=${toApi}`;

        fetch(url)
            .then(r => r.json())
            .then(data => {
                if (!Array.isArray(data)) return;

                const labels = data.map(d => d.date);
                const openData = data.map(d => d.open || 0);
                const closedData = data.map(d => d.closed || 0);
                const pendingData = data.map(d => d.pending || 0);

                renderComplaintChart(labels, openData, closedData, pendingData, 'Daily Ticket Summary');
            })
            .catch(err => console.error('Daily fetch error:', err));
    }
}

// === Render Chart ===
function renderComplaintChart(labels, open, closed, pending, title) {
    const ctx = document.getElementById('complaint-bar-chart').getContext('2d');
    if (complaintChart) complaintChart.destroy();

    const allData = [...open, ...closed, ...pending];
    const stackedValues = labels.map((_, i) => (open[i] || 0) + (closed[i] || 0) + (pending[i] || 0));
    const totalMax = Math.max(...stackedValues, 0);

    let stepSize;
    if (totalMax <= 20) {
        stepSize = 5;
    } else if (totalMax <= 50) {
        stepSize = 10;
    } else if (totalMax <= 100) {
        stepSize = 20;
    } else if (totalMax <= 200) {
        stepSize = 25;
    } else if (totalMax <= 500) {
        stepSize = 50;
    } else {
        stepSize = Math.ceil(totalMax / 10 / 10) * 10;
    }

    const yAxisMax = Math.ceil(totalMax / stepSize) * stepSize;

    complaintChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [
                { label: "Open", backgroundColor: '#fdaf4b', data: open },
                { label: "Closed", backgroundColor: '#59d05d', data: closed },
                { label: "Pending", backgroundColor: '#d9534f', data: pending }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            title: { display: true, text: title },
            scales: {
                x: { stacked: true },
                y: {
                    stacked: true,
                    beginAtZero: true,
                    max: yAxisMax,
                    ticks: { stepSize: stepSize }
                }
            },
            plugins: {
                tooltip: {
                    mode: 'index',
                    intersect: false,
                    callbacks: {
                        label: function (context) {
                            return `${context.dataset.label}: ${context.parsed.y}`;
                        }
                    }
                }
            }
        }
    });
}

// === Initialize Filter Form ===
function initComplaintChartForm() {
    const now = new Date();
    const from = new Date(now);
    from.setDate(now.getDate() - 6);

    // Set default dates
    document.getElementById('complaint-from').value = formatDate(from);
    document.getElementById('complaint-to').value = formatDate(now);

    // Populate year selector
    populateYearSelector('complaint-year-select', now.getFullYear());

    // === Prevent dropdown from closing on interaction ===
    document.querySelector('.dropdown-menu').addEventListener('click', function (e) {
        if (e.target.closest('select, input, .btn, .input-group')) {
            e.stopPropagation(); // 🔑 Keep dropdown open
        }
    });

    // === Quick Range Buttons ===
    document.querySelectorAll('.dropdown-menu .btn[data-range]').forEach(btn => {
        btn.addEventListener('click', () => {
            const range = btn.getAttribute('data-range');
            const now = new Date();

            if (range === 'weekly') {
                document.getElementById('complaint-year-selector').style.display = 'none';
                const from = new Date(now);
                from.setDate(now.getDate() - 6);
                document.getElementById('complaint-from').value = formatDate(from);
                document.getElementById('complaint-to').value = formatDate(now);
                updateComplaintChart('daily', formatDate(from), formatDate(now));
            } else if (range === 'monthly') {
                document.getElementById('complaint-year-selector').style.display = 'none';
                const from = new Date(now.getFullYear(), now.getMonth(), 1);
                document.getElementById('complaint-from').value = formatDate(from);
                document.getElementById('complaint-to').value = formatDate(now);
                updateComplaintChart('daily', formatDate(from), formatDate(now));
            } else if (range === 'yearly') {
                document.getElementById('complaint-year-selector').style.display = 'block';
                const year = document.getElementById('complaint-year-select').value;
                updateComplaintChart('yearly', null, null, year);
            }
        });
    });

    // === Year Change ===
    document.getElementById('complaint-year-select').addEventListener('change', () => {
        const year = document.getElementById('complaint-year-select').value;
        updateComplaintChart('yearly', null, null, year);
    });

    // === Apply Button ===
    document.getElementById('apply-complaint-filters').addEventListener('click', () => {
        const from = document.getElementById('complaint-from').value;
        const to = document.getElementById('complaint-to').value;
        const year = document.getElementById('complaint-year-select').value;

        if (document.getElementById('complaint-year-selector').style.display === 'block') {
            updateComplaintChart('yearly', null, null, year);
        } else {
            document.getElementById('complaint-year-selector').style.display = 'none';
            updateComplaintChart('daily', from, to);
        }
    });

    // === Initial Load ===
    updateComplaintChart('daily', formatDate(from), formatDate(now));
}

// === Initialize on DOM Load ===
document.addEventListener('DOMContentLoaded', () => {
    initComplaintChartForm();
});

// Engineers Responsibility
let currentFrom = null;
let currentTo = null;

function toDateTimeLocal(date) {
    const pad = (n) => n.toString().padStart(2, '0');
    const year = date.getFullYear();
    const month = pad(date.getMonth() + 1);
    const day = pad(date.getDate());
    const hours = pad(date.getHours());
    const minutes = pad(date.getMinutes());
    return `${year}-${month}-${day}T${hours}:${minutes}`;
}

// Format date for display: "Aug 10"
function formatShort(dateStr) {
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
}

function fetchEngineerData(from = null, to = null) {
    let url = `${baseUrl}/api/dashboards/assignee-daily-status`;
    if (from && to) {
        url += `?from=${from}&to=${to}`;
    }

    fetch(url)
        .then(res => res.json())
        .then(data => {
            currentFrom = data.dates[0];
            currentTo = data.dates[data.dates.length - 1];

            // Update headers
            const dayHeaderRow = document.getElementById('dayHeaders');
            dayHeaderRow.innerHTML = '';
            const numDays = data.dates.length;

            // Helper: create day headers (Day 1, Day 2, ...)
            function createDayThs(dateArray, offset = 0) {
                const ths = [];
                for (let i = 0; i < dateArray.length; i++) {
                    const th = document.createElement('th');
                    th.textContent = `${formatShort(dateArray[i])}`;
                    th.style.whiteSpace = 'pre-line';
                    ths.push(th);
                }
                return ths;
            }

            const openThs = createDayThs(data.dates);
            openThs.forEach(th => dayHeaderRow.appendChild(th));

            const pendingThs = createDayThs(data.dates);
            pendingThs.forEach(th => dayHeaderRow.appendChild(th));

            const closedThs = createDayThs(data.dates);
            closedThs.forEach(th => dayHeaderRow.appendChild(th));

            const tbody = document.getElementById('engineerTableBody');
            tbody.innerHTML = '';

            data.data.forEach(row => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
          <td class="text-left fw-bold">${row.assignee}</td>
          ${row.open.map(v => `<td>${v}</td>`).join('')}
          ${row.pending.map(v => `<td>${v}</td>`).join('')}
          ${row.closed.map(v => `<td>${v}</td>`).join('')}
        `;
                tbody.appendChild(tr);
            });
        })
        .catch(err => {
            console.error('Failed to load engineer data:', err);
        });
}

function shiftRange(offsetDays) {
    if (!currentFrom || !currentTo) return;

    const from = new Date(currentFrom);
    const to = new Date(currentTo);

    const daysDiff = Math.ceil((to - from) / (1000 * 60 * 60 * 24)); // number of days

    const newFrom = new Date(from);
    newFrom.setDate(newFrom.getDate() + offsetDays);
    const newTo = new Date(to);
    newTo.setDate(newTo.getDate() + offsetDays);

    const fromStr = toDateTimeLocal(newFrom).slice(0, 16); // '2025-08-10T00:00'
    const toStr = toDateTimeLocal(newTo).slice(0, 16);

    fetchEngineerData(fromStr, toStr);
}

document.getElementById('prevEngineerBtn').addEventListener('click', () => {
    shiftRange(-1); // shift backward by 1 day window size
});

document.getElementById('nextEngineerBtn').addEventListener('click', () => {
    shiftRange(1);
});

document.getElementById('refreshEngineerBtn').addEventListener('click', () => {
    currentFrom = null;
    currentTo = null;
    fetchEngineerData();
});

window.addEventListener('DOMContentLoaded', () => {
    fetchEngineerData();
});

// Equipment Complained
document.addEventListener('DOMContentLoaded', function () {
    const API_URL = `${baseUrl}/api/dashboards/equipment-complaint-count`;

    let currentPage = 1;
    const pageSize = 7;
    let allData = [];

    async function fetchEquipmentData() {
        console.log("Fetching equipment data...");
        try {
            const response = await fetch(API_URL);
            if (!response.ok) throw new Error('Failed to fetch data');
            allData = await response.json();
            console.log("equipment", allData);
            renderPage(currentPage);
        } catch (err) {
            console.error('Error loading equipment data:', err);
            const container = document.getElementById('equipmentListContainer');
            if (container) {
                container.innerHTML = `
                    <div class="text-center text-muted">Failed to load data</div>
                `;
            }
        }
    }

    function renderPage(page) {
        const container = document.getElementById('equipmentListContainer');
        if (!container) return; // Guard clause if element not found
        container.innerHTML = '';

        const start = (page - 1) * pageSize;
        const end = start + pageSize;
        const pageData = allData.slice(start, end);

        if (pageData.length === 0) {
            container.innerHTML = `<div class="text-center text-muted">No data</div>`;
            return;
        }

        pageData.forEach((item, index) => {
            const rank = start + index + 1;
            const div = document.createElement('div');
            div.className = 'd-flex';
            div.innerHTML = `
                <div class="mr-3 d-flex align-items-center" style="min-width: 28px;">
                    <span class="fw-bold" style="font-size: 0.95rem; color: #555; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;">
                        ${rank}
                    </span>
                </div>
                <div class="flex-1 pt-1 ml-2">
                    <h6 class="fw-bold mb-1">${item.equipmentCode}</h6>
                    <small class="text-muted">${item.equipmentName}</small>
                </div>
                <div class="d-flex ml-auto align-items-center">
                    <h3 class="text-info fw-bold">${item.totalComplaints}</h3>
                </div>
            `;
            container.appendChild(div);

            // Add separator
            const sep = document.createElement('div');
            sep.className = 'separator-dashed';
            container.appendChild(sep);
        });
    }

    // Initialize buttons only after DOM is ready
    const nextBtn = document.getElementById('nextEquipmentBtn');
    const prevBtn = document.getElementById('prevEquipmentBtn');
    const refreshBtn = document.getElementById('refreshEquipmentBtn');

    if (nextBtn) {
        nextBtn.addEventListener('click', () => {
            if (allData.length > currentPage * pageSize) {
                currentPage++;
                renderPage(currentPage);
            }
        });
    }

    if (prevBtn) {
        prevBtn.addEventListener('click', () => {
            if (currentPage > 1) {
                currentPage--;
                renderPage(currentPage);
            }
        });
    }

    if (refreshBtn) {
        refreshBtn.addEventListener('click', () => {
            currentPage = 1;
            fetchEquipmentData(); // Re-fetch latest data
        });
    }

    // Initial data load
    fetchEquipmentData();
});

// Work Report Chart
let workReportChart = null;

function updateWorkReportChart(mode, from = null, to = null, year = null) {
    const ctx = document.getElementById('work-report-line-chart').getContext('2d');
    if (workReportChart) workReportChart.destroy();

    if (mode === 'yearly') {
        fetch(`/api/dashboards/monthly-work-report?year=${year}`)
            .then(r => r.json())
            .then(data => {
                if (!Array.isArray(data)) return;

                const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
                const labels = months;

                // Initialize datasets
                const correctiveData = Array(12).fill(0);
                const preventiveData = Array(12).fill(0);
                const breakdownData = Array(12).fill(0);
                const otherData = Array(12).fill(0);

                data.forEach(d => {
                    const i = d.month - 1;
                    if (i >= 0 && i < 12) {
                        correctiveData[i] = d.correctiveMaintenanceCount || 0;
                        preventiveData[i] = d.preventiveMaintenanceCount || 0;
                        breakdownData[i] = d.breakdownCount || 0;
                        otherData[i] = d.otherCount || 0;
                    }
                });

                workReportChart = new Chart(ctx, {
                    type: 'line',
                    data: {
                        labels: labels,
                        datasets: [
                            {
                                label: "Corrective Maintenance",
                                borderColor: "#e74c3c",
                                pointBorderColor: "#FFF",
                                pointBackgroundColor: "#e74c3c",
                                pointBorderWidth: 2,
                                pointHoverRadius: 4,
                                pointRadius: 4,
                                backgroundColor: 'transparent',
                                fill: false,
                                borderWidth: 2,
                                data: correctiveData,
                                counts: correctiveData
                            },
                            {
                                label: "Preventive Maintenance",
                                borderColor: "#3498db",
                                pointBorderColor: "#FFF",
                                pointBackgroundColor: "#3498db",
                                pointBorderWidth: 2,
                                pointHoverRadius: 4,
                                pointRadius: 4,
                                backgroundColor: 'transparent',
                                fill: false,
                                borderWidth: 2,
                                data: preventiveData,
                                counts: preventiveData
                            },
                            {
                                label: "Breakdown",
                                borderColor: "#f39c12",
                                pointBorderColor: "#FFF",
                                pointBackgroundColor: "#f39c12",
                                pointBorderWidth: 2,
                                pointHoverRadius: 4,
                                pointRadius: 4,
                                backgroundColor: 'transparent',
                                fill: false,
                                borderWidth: 2,
                                data: breakdownData,
                                counts: breakdownData
                            },
                            {
                                label: "Other",
                                borderColor: "#95a5a6",
                                pointBorderColor: "#FFF",
                                pointBackgroundColor: "#95a5a6",
                                pointBorderWidth: 2,
                                pointHoverRadius: 4,
                                pointRadius: 4,
                                backgroundColor: 'transparent',
                                fill: false,
                                borderWidth: 2,
                                data: otherData,
                                counts: otherData
                            }
                        ]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        legend: {
                            position: 'bottom',
                            labels: { fontColor: '#333', padding: 15 }
                        },
                        tooltips: {
                            mode: "nearest",
                            intersect: false,
                            callbacks: {
                                label: function (tooltipItem, data) {
                                    const dataset = data.datasets[tooltipItem.datasetIndex];
                                    const value = dataset.data[tooltipItem.index];
                                    const label = dataset.label;
                                    return `${label}: ${value} reports`;
                                }
                            }
                        },
                        scales: {
                            x: { stacked: false },
                            y: {
                                beginAtZero: true,
                                ticks: {
                                    stepSize: 1,
                                    callback: function (value) {
                                        return Number.isInteger(value) ? value : null;
                                    }
                                },
                                title: {
                                    display: true,
                                    text: 'Number of Reports'
                                }
                            }
                        }
                    }
                });
            });

    } else {
        fetch(`/api/dashboards/daily-work-report?from=${from}&to=${to}`)
            .then(r => r.json())
            .then(data => {
                if (!Array.isArray(data)) return;

                const labels = data.map(d => d.date);
                const correctiveData = data.map(d => d.correctiveMaintenanceCount || 0);
                const preventiveData = data.map(d => d.preventiveMaintenanceCount || 0);
                const breakdownData = data.map(d => d.breakdownCount || 0);
                const otherData = data.map(d => d.otherCount || 0);

                workReportChart = new Chart(ctx, {
                    type: 'line',
                    data: {
                        labels: labels,
                        datasets: [
                            {
                                label: "Corrective Maintenance",
                                borderColor: "#e74c3c",
                                pointBorderColor: "#FFF",
                                pointBackgroundColor: "#e74c3c",
                                pointBorderWidth: 2,
                                pointHoverRadius: 4,
                                pointRadius: 4,
                                backgroundColor: 'transparent',
                                fill: false,
                                borderWidth: 2,
                                data: correctiveData,
                                counts: correctiveData
                            },
                            {
                                label: "Preventive Maintenance",
                                borderColor: "#3498db",
                                pointBorderColor: "#FFF",
                                pointBackgroundColor: "#3498db",
                                pointBorderWidth: 2,
                                pointHoverRadius: 4,
                                pointRadius: 4,
                                backgroundColor: 'transparent',
                                fill: false,
                                borderWidth: 2,
                                data: preventiveData,
                                counts: preventiveData
                            },
                            {
                                label: "Breakdown",
                                borderColor: "#f39c12",
                                pointBorderColor: "#FFF",
                                pointBackgroundColor: "#f39c12",
                                pointBorderWidth: 2,
                                pointHoverRadius: 4,
                                pointRadius: 4,
                                backgroundColor: 'transparent',
                                fill: false,
                                borderWidth: 2,
                                data: breakdownData,
                                counts: breakdownData
                            },
                            {
                                label: "Other",
                                borderColor: "#95a5a6",
                                pointBorderColor: "#FFF",
                                pointBackgroundColor: "#95a5a6",
                                pointBorderWidth: 2,
                                pointHoverRadius: 4,
                                pointRadius: 4,
                                backgroundColor: 'transparent',
                                fill: false,
                                borderWidth: 2,
                                data: otherData,
                                counts: otherData
                            }
                        ]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        legend: {
                            position: 'bottom',
                            labels: { fontColor: '#333', padding: 15 }
                        },
                        tooltips: {
                            mode: "nearest",
                            intersect: false,
                            callbacks: {
                                label: function (tooltipItem, data) {
                                    const dataset = data.datasets[tooltipItem.datasetIndex];
                                    const value = dataset.data[tooltipItem.index];
                                    const label = dataset.label;
                                    return `${label}: ${value} reports`;
                                }
                            }
                        },
                        scales: {
                            x: { stacked: false },
                            y: {
                                beginAtZero: true,
                                ticks: {
                                    stepSize: 1,
                                    callback: function (value) {
                                        return Number.isInteger(value) ? value : null;
                                    }
                                },
                                title: {
                                    display: true,
                                    text: 'Number of Reports'
                                }
                            }
                        }
                    }
                });
            });
    }
}

function initWorkReportChartForm() {
    const now = new Date();
    const from = new Date(now);
    from.setDate(now.getDate() - 6);

    document.getElementById('work-report-from').value = formatDate(from);
    document.getElementById('work-report-to').value = formatDate(now);

    // Populate year selector
    populateYearSelector('work-report-year-select', now.getFullYear());

    // Quick buttons
    document.querySelectorAll('#work-report-chart-form .btn[data-range]').forEach(btn => {
        btn.addEventListener('click', () => {
            const range = btn.getAttribute('data-range');
            const now = new Date();
            if (range === 'weekly') {
                document.getElementById('work-report-year-selector').style.display = 'none';
                const from = new Date(now);
                from.setDate(now.getDate() - 6);
                document.getElementById('work-report-from').value = formatDate(from);
                document.getElementById('work-report-to').value = formatDate(now);
                updateWorkReportChart('daily', formatDate(from), formatDate(now));
            } else if (range === 'monthly') {
                document.getElementById('work-report-year-selector').style.display = 'none';
                const from = new Date(now.getFullYear(), now.getMonth(), 1);
                document.getElementById('work-report-from').value = formatDate(from);
                document.getElementById('work-report-to').value = formatDate(now);
                updateWorkReportChart('daily', formatDate(from), formatDate(now));
            } else if (range === 'yearly') {
                document.getElementById('work-report-year-selector').style.display = 'inline-block';
                const year = document.getElementById('work-report-year-select').value;
                updateWorkReportChart('yearly', null, null, year);
            }
        });
    });

    // Year change
    document.getElementById('work-report-year-select').addEventListener('change', () => {
        const year = document.getElementById('work-report-year-select').value;
        updateWorkReportChart('yearly', null, null, year);
    });

    // Apply button
    document.getElementById('work-report-chart-form').addEventListener('submit', e => {
        e.preventDefault();
        const from = document.getElementById('work-report-from').value;
        const to = document.getElementById('work-report-to').value;
        document.getElementById('work-report-year-selector').style.display = 'none';
        updateWorkReportChart('daily', from, to);
    });

    // Initial load
    updateWorkReportChart('daily', formatDate(from), formatDate(now));
}

// Helper: format date to YYYY-MM-DD
function formatDate(date) {
    return date.toISOString().split('T')[0];
}

// Helper: populate year selector
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

// Breakdown Chart
let breakdownChart = null;

// Helper: Format Date to YYYY-MM-DD
function formatDate(date) {
    return date.toISOString().split('T')[0];
}

// Helper: Populate Year Selector
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

// === Update Breakdown Chart ===
function updateBreakdownChart(mode, from = null, to = null, year = null) {
    const ctx = document.getElementById('breakdown-line-chart').getContext('2d');
    if (breakdownChart) breakdownChart.destroy();

    if (mode === 'yearly') {
        fetch(`/api/dashboards/monthly-breakdown?year=${year}`)
            .then(r => {
                if (!r.ok) throw new Error(`HTTP ${r.status}`);
                return r.json();
            })
            .then(data => {
                if (!Array.isArray(data)) return;

                const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
                const values = Array(12).fill(0);
                const counts = Array(12).fill(0);

                data.forEach(d => {
                    const i = d.month - 1;
                    if (i >= 0 && i < 12) {
                        values[i] = d.totalResolutionTimeMinutes || 0;
                        counts[i] = d.breakdownCount || 0;
                    }
                });

                breakdownChart = new Chart(ctx, {
                    type: 'line',
                    data: {
                        labels: months,
                        datasets: [{
                            label: "Total Resolution Time (min)",
                            borderColor: "#1d7af3",
                            pointBorderColor: "#FFF",
                            pointBackgroundColor: "#1d7af3",
                            pointBorderWidth: 2,
                            pointHoverRadius: 4,
                            pointRadius: 4,
                            backgroundColor: 'transparent',
                            fill: true,
                            borderWidth: 2,
                            data: values,
                            breakdownCounts: counts // Attach count for tooltip
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        legend: {
                            position: 'bottom',
                            labels: { fontColor: '#1d7af3', padding: 15 }
                        },
                        tooltips: {
                            mode: "nearest",
                            intersect: false,
                            callbacks: {
                                label: function (tooltipItem, data) {
                                    const dataset = data.datasets[0];
                                    const value = dataset.data[tooltipItem.index];
                                    const count = dataset.breakdownCounts[tooltipItem.index];
                                    return [
                                        `Total Time: ${value} min`,
                                        `Breakdown Count: ${count}`
                                    ];
                                }
                            }
                        },
                        scales: {
                            x: { stacked: false },
                            y: {
                                beginAtZero: true,
                                title: {
                                    display: true,
                                    text: 'Resolution Time (minutes)'
                                },
                                ticks: {
                                    stepSize: Math.max(1, Math.round(Math.max(...values) / 10) || 1)
                                }
                            }
                        }
                    }
                });
            })
            .catch(err => console.error('Monthly breakdown fetch error:', err));

    } else {
        fetch(`/api/dashboards/daily-breakdown?from=${from}&to=${to}`)
            .then(r => {
                if (!r.ok) throw new Error(`HTTP ${r.status}`);
                return r.json();
            })
            .then(data => {
                if (!Array.isArray(data)) return;

                const labels = data.map(d => d.date);
                const values = data.map(d => d.totalResolutionTimeMinutes || 0);
                const counts = data.map(d => d.breakdownCount || 0);

                breakdownChart = new Chart(ctx, {
                    type: 'line',
                    data: {
                        labels: labels,
                        datasets: [{
                            label: "Total Resolution Time (min)",
                            borderColor: "#1d7af3",
                            pointBorderColor: "#FFF",
                            pointBackgroundColor: "#1d7af3",
                            pointBorderWidth: 2,
                            pointHoverRadius: 4,
                            pointRadius: 4,
                            backgroundColor: 'transparent',
                            fill: true,
                            borderWidth: 2,
                            data: values,
                            breakdownCounts: counts
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        legend: {
                            position: 'bottom',
                            labels: { fontColor: '#1d7af3', padding: 15 }
                        },
                        tooltips: {
                            mode: "nearest",
                            intersect: false,
                            callbacks: {
                                label: function (tooltipItem, data) {
                                    const dataset = data.datasets[0];
                                    const value = dataset.data[tooltipItem.index];
                                    const count = dataset.breakdownCounts[tooltipItem.index];
                                    return [
                                        `Total Time: ${value} min`,
                                        `Breakdown Count: ${count}`
                                    ];
                                }
                            }
                        },
                        scales: {
                            x: { stacked: false },
                            y: {
                                beginAtZero: true,
                                title: {
                                    display: true,
                                    text: 'Resolution Time (minutes)'
                                },
                                ticks: {
                                    stepSize: Math.max(1, Math.round(Math.max(...values) / 10) || 1)
                                }
                            }
                        }
                    }
                });
            })
            .catch(err => console.error('Daily breakdown fetch error:', err));
    }
}

// === Initialize Breakdown Chart Form ===
function initBreakdownChartForm() {
    const now = new Date();
    const from = new Date(now);
    from.setDate(now.getDate() - 6);

    // Set default dates
    document.getElementById('breakdown-from').value = formatDate(from);
    document.getElementById('breakdown-to').value = formatDate(now);

    // Populate year selector
    populateYearSelector('breakdown-year-select', now.getFullYear());

    // === Prevent dropdown from closing when interacting with form ===
    document.querySelector('.dropdown-menu').addEventListener('click', function (e) {
        if (e.target.closest('select, input, .btn, .input-group')) {
            e.stopPropagation(); // 🔑 Keep dropdown open
        }
    });

    // === Quick Range Buttons ===
    document.querySelectorAll('.dropdown-menu .btn[data-range]').forEach(btn => {
        btn.addEventListener('click', () => {
            const range = btn.getAttribute('data-range');
            const now = new Date();

            if (range === 'weekly') {
                document.getElementById('breakdown-year-selector').style.display = 'none';
                const from = new Date(now);
                from.setDate(now.getDate() - 6);
                document.getElementById('breakdown-from').value = formatDate(from);
                document.getElementById('breakdown-to').value = formatDate(now);
                updateBreakdownChart('daily', formatDate(from), formatDate(now));
            } else if (range === 'monthly') {
                document.getElementById('breakdown-year-selector').style.display = 'none';
                const from = new Date(now.getFullYear(), now.getMonth(), 1);
                document.getElementById('breakdown-from').value = formatDate(from);
                document.getElementById('breakdown-to').value = formatDate(now);
                updateBreakdownChart('daily', formatDate(from), formatDate(now));
            } else if (range === 'yearly') {
                document.getElementById('breakdown-year-selector').style.display = 'block';
                const year = document.getElementById('breakdown-year-select').value;
                updateBreakdownChart('yearly', null, null, year);
            }
        });
    });

    // === Year Selector Change ===
    document.getElementById('breakdown-year-select').addEventListener('change', () => {
        const year = document.getElementById('breakdown-year-select').value;
        updateBreakdownChart('yearly', null, null, year);
    });

    // === Apply Button ===
    document.getElementById('apply-breakdown-filters').addEventListener('click', () => {
        const from = document.getElementById('breakdown-from').value;
        const to = document.getElementById('breakdown-to').value;
        const year = document.getElementById('breakdown-year-select').value;

        if (document.getElementById('breakdown-year-selector').style.display === 'block') {
            updateBreakdownChart('yearly', null, null, year);
        } else {
            document.getElementById('breakdown-year-selector').style.display = 'none';
            updateBreakdownChart('daily', from, to);
        }
    });

    // === Initial Load ===
    updateBreakdownChart('daily', formatDate(from), formatDate(now));
}

// Equipment Repaired
const EQUIPMENT_WORK_API_URL = `${baseUrl}/api/dashboards/equipment-work-report`;
const CONTAINER_ID = 'equipment-work-list-container';
const PREV_BTN_ID = 'equipment-work-prev-btn';
const NEXT_BTN_ID = 'equipment-work-next-btn';
const REFRESH_BTN_ID = 'equipment-work-refresh-btn';

// --- State Variables ---
let wrCurrentPage = 1;
const wrPageSize = 7;
let wrAllData = [];

// --- Utility Functions ---
function formatNumber(num) {
    return num.toLocaleString();
}

function formatMinutes(minutes) {
    return `${formatNumber(minutes)} min`;
}

// --- Fetch Data ---
async function fetchEquipmentWorkData() {
    const container = document.getElementById(CONTAINER_ID);
    if (!container) {
        console.error('❌ Container not found:', CONTAINER_ID);
        return;
    }

    container.innerHTML = '<div class="text-center py-3">Loading...</div>';

    try {
        const response = await fetch(EQUIPMENT_WORK_API_URL);
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        wrAllData = await response.json();
        console.log('✅ API Response:', wrAllData);

        // Sort by totalWorkReports descending
        wrAllData.sort((a, b) => (b.totalWorkReports || 0) - (a.totalWorkReports || 0));

        renderPage(wrCurrentPage);
    } catch (err) {
        console.error('🚨 Error loading equipment work data:', err);
        document.getElementById(CONTAINER_ID).innerHTML = `
                <div class="text-center text-muted py-3">
                    Failed to load data: ${err.message}
                </div>
            `;
    }
}

// --- Render Current Page ---
function renderPage(page) {
    const container = document.getElementById(CONTAINER_ID);
    if (!container) return;

    const start = (page - 1) * wrPageSize;
    const end = start + wrPageSize;
    const pageData = wrAllData.slice(start, end);

    container.innerHTML = ''; // Clear

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
                    <span class="fw-bold" style="
                        font-size: 0.95rem; 
                        color: #555; 
                        font-family: 'Segoe UI', sans-serif;">
                        ${rank}
                    </span>
                </div>
                <div class="flex-1 ml-2 pt-1">
                    <h6 class="fw-bold mb-1">
                        ${item.equipmentCode}
                        <span class="text-muted pl-3">(${item.totalWorkReports} reports)</span>
                    </h6>
                    <small class="text-muted">${item.equipmentName}</small>
                </div>
                <div class="d-flex ml-auto align-items-center">
                    <small class="text-muted">${formatMinutes(item.totalResolutionTime)}</small>
                </div>
            `;
        container.appendChild(div);

        // Add separator
        const sep = document.createElement('div');
        sep.className = 'separator-dashed';
        container.appendChild(sep);
    });
}

// --- Button Setup ---
function setupButtons() {
    const prevBtn = document.getElementById(PREV_BTN_ID);
    const nextBtn = document.getElementById(NEXT_BTN_ID);
    const refreshBtn = document.getElementById(REFRESH_BTN_ID);

    if (prevBtn) {
        prevBtn.addEventListener('click', () => {
            if (wrCurrentPage > 1) {
                wrCurrentPage--;
                renderPage(wrCurrentPage);
            }
        });
    } else {
        console.warn('⚠️ Prev button not found:', PREV_BTN_ID);
    }

    if (nextBtn) {
        nextBtn.addEventListener('click', () => {
            if (wrAllData.length > wrCurrentPage * wrPageSize) {
                wrCurrentPage++;
                renderPage(wrCurrentPage);
            }
        });
    } else {
        console.warn('⚠️ Next button not found:', NEXT_BTN_ID);
    }

    if (refreshBtn) {
        refreshBtn.addEventListener('click', () => {
            wrCurrentPage = 1;
            fetchEquipmentWorkData();
        });
    } else {
        console.warn('⚠️ Refresh button not found:', REFRESH_BTN_ID);
    }
}

function enableScrollNavigation() {
    const container = document.getElementById(CONTAINER_ID);
    if (!container) return;

    let isScrolling = false;

    container.addEventListener('wheel', (e) => {
        if (isScrolling) return; // Prevent rapid triggers
        e.preventDefault();

        isScrolling = true;
        setTimeout(() => { isScrolling = false; }, 300);

        const delta = e.deltaY;

        if (delta > 0) {
            // ✅ Scroll down → go to NEXT page (newer data)
            if (wrAllData.length > wrCurrentPage * wrPageSize) {
                wrCurrentPage++;
                renderPage(wrCurrentPage);
            }
        } else if (delta < 0) {
            // ✅ Scroll up → go to PREVIOUS page (older data)
            if (wrCurrentPage > 1) {
                wrCurrentPage--;
                renderPage(wrCurrentPage);
            }
        }
    });

    // Optional: Add subtle visual hint
    container.style.cursor = 'grab';
    container.setAttribute('title', 'Scroll to navigate pages');

    // Visual feedback on hover
    container.addEventListener('mouseenter', () => {
        container.style.cursor = 'grab';
    });
    container.addEventListener('mousedown', () => {
        container.style.cursor = 'grabbing';
    });
    container.addEventListener('mouseup', () => {
        container.style.cursor = 'grab';
    });
}

// --- On DOM Ready ---
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        setupButtons();
        enableScrollNavigation();
        fetchEquipmentWorkData();
    });
} else {
    // Already loaded
    setupButtons();
    enableScrollNavigation();
    fetchEquipmentWorkData();
}

window.addEventListener('DOMContentLoaded', () => {
    initComplaintChartForm();
    initBreakdownChartForm();
    initWorkReportChartForm();
    fetchEquipmentData();
});