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

function toDateTimeLocal(date) {
    const pad = (n) => n.toString().padStart(2, '0');
    const year = date.getFullYear();
    const month = pad(date.getMonth() + 1); // 0-indexed
    const day = pad(date.getDate());
    const hours = pad(date.getHours());
    const minutes = pad(date.getMinutes());
    return `${year}-${month}-${day}T${hours}:${minutes}`;
}

// Overall Complaint
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

function initComplaintStatsForm() {
    setComplaintStatsDefaults();

    const from = document.getElementById('complaint-stats-from').value;
    const to = document.getElementById('complaint-stats-to').value;

    fetchComplaintStats(from, to);
}

// Complaint Chart
let complaintChart = null;

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

function initComplaintChartForm() {
    const now = new Date();
    const from = new Date(now);
    from.setDate(now.getDate() - 6);

    document.getElementById('complaint-from').value = formatDate(from);
    document.getElementById('complaint-to').value = formatDate(now);

    populateYearSelector('complaint-year-select', now.getFullYear());

    document.querySelector('.dropdown-menu').addEventListener('click', function (e) {
        if (e.target.closest('select, input, .btn, .input-group')) {
            e.stopPropagation(); // 🔑 Keep dropdown open
        }
    });

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

    document.getElementById('complaint-year-select').addEventListener('change', () => {
        const year = document.getElementById('complaint-year-select').value;
        updateComplaintChart('yearly', null, null, year);
    });

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

    updateComplaintChart('daily', formatDate(from), formatDate(now));
}

// Engineers Responsibility
let currentFrom = null;
let currentTo = null;

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

            const dayHeaderRow = document.getElementById('dayHeaders');
            const thead = dayHeaderRow.parentNode;

            // 👇 STEP 1: Clear existing dynamic headers (date row + subheader row)
            dayHeaderRow.innerHTML = ''; // Clear date headers

            // Remove existing subheader row if exists (to prevent stacking)
            const existingSubHeader = thead.querySelector('tr[data-subheader="true"]');
            if (existingSubHeader) {
                existingSubHeader.remove();
            }

            // Hide static status headers (Open, Pending, Closed)
            document.getElementById('openHeader').colSpan = 0;
            document.getElementById('pendingHeader').style.display = 'none';
            document.getElementById('closedHeader').colSpan = 0;

            const numDays = data.dates.length;

            // 👇 STEP 2: Render new date headers (grouped by date)
            for (let i = 0; i < numDays; i++) {
                const dateShort = formatShort(data.dates[i]);

                const thDate = document.createElement('th');
                thDate.colSpan = 2;
                thDate.textContent = dateShort;
                thDate.classList.add('bg-primary', 'text-white'); // 🔵 Match primary color
                thDate.style.verticalAlign = 'middle';
                dayHeaderRow.appendChild(thDate);
            }

            // 👇 STEP 3: Create and insert subheader row (Open / Closed)
            const subHeaderRow = document.createElement('tr');
            subHeaderRow.setAttribute('data-subheader', 'true'); // marker for cleanup
            subHeaderRow.classList.add('bg-primary', 'text-white'); // 🔵 Primary background

            for (let i = 0; i < numDays; i++) {
                const thOpen = document.createElement('th');
                thOpen.textContent = 'Open';
                thOpen.style.fontSize = '0.8em';
                thOpen.classList.add('text-white');
                subHeaderRow.appendChild(thOpen);

                const thClosed = document.createElement('th');
                thClosed.textContent = 'Closed';
                thClosed.style.fontSize = '0.8em';
                thClosed.classList.add('text-white');
                subHeaderRow.appendChild(thClosed);
            }

            // Insert subheader row right after dayHeaders
            thead.insertBefore(subHeaderRow, dayHeaderRow.nextSibling);

            // 👇 STEP 4: Render table body
            const tbody = document.getElementById('engineerTableBody');
            tbody.innerHTML = '';

            data.data.forEach(row => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
          <td class="text-left fw-bold">${row.assignee}</td>
          ${data.dates.map((date, i) => `
            <td>${row.open[i] || 0}</td>
            <td>${row.closed[i] || 0}</td>
          `).join('')}
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

// Work Report Chart
let wrChart = null;

function renderWrChart(labels, corrective, preventive, breakdown, other, title) {
    const ctx = document.getElementById('wr-equipment-line-chart').getContext('2d');
    if (wrChart) wrChart.destroy();

    // Calculate stacked totals for dynamic y-axis
    const stackedValues = labels.map((_, i) =>
        (corrective[i] || 0) +
        (preventive[i] || 0) +
        (breakdown[i] || 0) +
        (other[i] || 0)
    );
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

    wrChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [
                {
                    label: "Corrective Maintenance",
                    backgroundColor: '#d9534f',
                    data: corrective
                },
                {
                    label: "Preventive Maintenance",
                    backgroundColor: '#59d05d',
                    data: preventive
                },
                {
                    label: "Breakdown",
                    backgroundColor: '#fdaf4b',
                    data: breakdown
                },
                {
                    label: "Other",
                    backgroundColor: '#95a5a6',
                    data: other
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                title: {
                    display: true,
                    text: title,
                    font: {
                        size: 16
                    }
                },
                tooltip: {
                    mode: 'index',
                    intersect: false,
                    callbacks: {
                        label: function (context) {
                            return `${context.dataset.label}: ${context.parsed.y}`;
                        }
                    }
                },
                legend: {
                    position: 'bottom',
                    labels: {
                        color: '#333', // Chart.js v3 uses 'color'
                        padding: 15
                    }
                }
            },
            scales: {
                x: {
                    stacked: true
                },
                y: {
                    stacked: true,
                    beginAtZero: true,
                    max: yAxisMax,
                    ticks: {
                        stepSize: stepSize,
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
}

function updateWrChart(mode, from = null, to = null, year = null, equipmentCode = null) {
    let url = '';
    if (mode === 'yearly') {
        url = `/api/dashboards/monthly-work-report-equipment?year=${year}`;
        if (equipmentCode) url += `&equipmentCode=${encodeURIComponent(equipmentCode)}`;
    } else {
        const fromApi = toApiDateTime(from, false);
        const toApi = toApiDateTime(to, true);
        url = `/api/dashboards/daily-work-report-equipment?from=${from}&to=${to}`;
        if (equipmentCode) url += `&equipmentCode=${encodeURIComponent(equipmentCode)}`;
    }

    console.log("Fetching:", url);

    fetch(url)
        .then(r => {
            if (!r.ok) throw new Error(`HTTP error! status: ${r.status}`);
            return r.json();
        })
        .then(data => {
            if (!Array.isArray(data)) {
                console.warn("Expected array, got:", data);
                return;
            }

            let labels, correctiveData, preventiveData, breakdownData, otherData;

            if (mode === 'yearly') {
                const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
                labels = Array(12).fill('');
                correctiveData = Array(12).fill(0);
                preventiveData = Array(12).fill(0);
                breakdownData = Array(12).fill(0);
                otherData = Array(12).fill(0);

                data.forEach(d => {
                    const i = d.month - 1;
                    if (i >= 0 && i < 12) {
                        labels[i] = months[i];
                        correctiveData[i] = d.correctiveMaintenanceCount || 0;
                        preventiveData[i] = d.preventiveMaintenanceCount || 0;
                        breakdownData[i] = d.breakdownCount || 0;
                        otherData[i] = d.otherCount || 0;
                    }
                });

                // Fill in any missing months
                for (let i = 0; i < 12; i++) {
                    if (!labels[i]) labels[i] = months[i];
                }

                renderWrChart(labels, correctiveData, preventiveData, breakdownData, otherData, 'Monthly Work Report');
            } else {
                labels = data.map(d => d.date);
                correctiveData = data.map(d => d.correctiveMaintenanceCount || 0);
                preventiveData = data.map(d => d.preventiveMaintenanceCount || 0);
                breakdownData = data.map(d => d.breakdownCount || 0);
                otherData = data.map(d => d.otherCount || 0);

                renderWrChart(labels, correctiveData, preventiveData, breakdownData, otherData, 'Daily Work Report');
            }
        })
        .catch(err => {
            console.error("Error loading chart data:", err);
            const ctx = document.getElementById('wr-equipment-line-chart').getContext('2d');
            ctx.save();
            ctx.textAlign = 'center';
            ctx.textBaseline = 'middle';
            ctx.font = "16px Arial";
            ctx.fillText("Failed to load data", ctx.canvas.width / 2, ctx.canvas.height / 2);
            ctx.restore();
        });
}

function initWrChartForm() {
    const now = new Date();
    const from = new Date(now);
    from.setDate(now.getDate() - 6);

    document.getElementById('wr-from').value = formatDate(from);
    document.getElementById('wr-to').value = formatDate(now);

    populateYearSelector('wr-year-select', now.getFullYear());

    fetch('/api/dashboards/equipment-complaint-count')
        .then(r => r.json())
        .then(equipmentList => {
            const select = document.getElementById('wr-equipment');
            equipmentList
                .sort((a, b) => a.equipmentName.localeCompare(b.equipmentName))
                .forEach(item => {
                    const opt = new Option(item.equipmentName, item.equipmentCode);
                    select.appendChild(opt);
                });

            const initialEquipment = document.getElementById('wr-equipment').value;
            updateWrChart('daily', formatDate(from), formatDate(now), null, initialEquipment);
        })
        .catch(err => {
            console.warn("Failed to load equipment list:", err);
            const initialEquipment = document.getElementById('wr-equipment').value;
            updateWrChart('daily', formatDate(from), formatDate(now), null, initialEquipment);
        });

    document.querySelectorAll('.dropdown-menu .btn[data-range]').forEach(btn => {
        btn.addEventListener('click', () => {
            const range = btn.getAttribute('data-range');
            const now = new Date();
            const equipmentCode = document.getElementById('wr-equipment').value;

            if (range === 'weekly') {
                document.getElementById('wr-year-selector').style.display = 'none';
                const from = new Date(now);
                from.setDate(now.getDate() - 6);
                document.getElementById('wr-from').value = formatDate(from);
                document.getElementById('wr-to').value = formatDate(now);
                updateWrChart('daily', formatDate(from), formatDate(now), null, equipmentCode);
            } else if (range === 'monthly') {
                document.getElementById('wr-year-selector').style.display = 'none';
                const from = new Date(now.getFullYear(), now.getMonth(), 1);
                document.getElementById('wr-from').value = formatDate(from);
                document.getElementById('wr-to').value = formatDate(now);
                updateWrChart('daily', formatDate(from), formatDate(now), null, equipmentCode);
            } else if (range === 'yearly') {
                document.getElementById('wr-year-selector').style.display = 'block';
                const year = document.getElementById('wr-year-select').value;
                updateWrChart('yearly', null, null, year, equipmentCode);
            }
        });
    });

    document.getElementById('wr-year-select').addEventListener('change', () => {
        const year = document.getElementById('wr-year-select').value;
        const equipmentCode = document.getElementById('wr-equipment').value;
        updateWrChart('yearly', null, null, year, equipmentCode);
    });

    document.getElementById('apply-filters-btn').addEventListener('click', () => {
        const from = document.getElementById('wr-from').value;
        const to = document.getElementById('wr-to').value;
        const equipmentCode = document.getElementById('wr-equipment').value;
        const year = document.getElementById('wr-year-select').value;

        if (document.getElementById('wr-year-selector').style.display === 'block') {
            updateWrChart('yearly', null, null, year, equipmentCode);
        } else {
            document.getElementById('wr-year-selector').style.display = 'none';
            updateWrChart('daily', from, to, null, equipmentCode);
        }
    });
}

document.addEventListener('DOMContentLoaded', () => {
    const equipmentSelect = document.getElementById('wr-equipment');

    if (!equipmentSelect) {
        console.error("Element #wr-equipment not found");
        return;
    }

    ['mousedown', 'click', 'focusin'].forEach(eventType => {
        equipmentSelect.addEventListener(eventType, (e) => {
            e.stopPropagation();
        });
    });
    initWrChartForm();
});

// Breakdown Chart
let breakdownChart = null;

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

function initBreakdownChartForm() {
    const now = new Date();
    const from = new Date(now);
    from.setDate(now.getDate() - 6);

    document.getElementById('breakdown-from').value = formatDate(from);
    document.getElementById('breakdown-to').value = formatDate(now);

    populateYearSelector('breakdown-year-select', now.getFullYear());

    document.querySelector('.dropdown-menu').addEventListener('click', function (e) {
        if (e.target.closest('select, input, .btn, .input-group')) {
            e.stopPropagation(); // 🔑 Keep dropdown open
        }
    });

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

    document.getElementById('breakdown-year-select').addEventListener('change', () => {
        const year = document.getElementById('breakdown-year-select').value;
        updateBreakdownChart('yearly', null, null, year);
    });

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

    updateBreakdownChart('daily', formatDate(from), formatDate(now));
}

// Equipment Repaired
const EQUIPMENT_WORK_API_URL = 'http://localhost:8000/api/dashboards/equipment-count';
const CONTAINER_ID = 'equipment-work-list-container';
const PREV_BTN_ID = 'equipment-work-prev-btn';
const NEXT_BTN_ID = 'equipment-work-next-btn';
const REFRESH_BTN_ID = 'equipment-work-refresh-btn';

let wrCurrentPage = 1;
const wrPageSize = 5;
let wrAllData = [];

function formatNumber(num) {
    return num.toLocaleString();
}

function formatMinutes(minutes) {
    return `${formatNumber(minutes)} min`;
}

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

        wrAllData.sort((a, b) => (b.totalResolutionTime || 0) - (a.totalResolutionTime || 0));

        wrCurrentPage = 1; // Reset to first page on refresh
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

        // Determine color classes based on rank
        let avatarBgClass = rank <= 3 ? 'bg-primary' : 'bg-secondary';
        let timeColorClass = '';
        if (rank === 1) timeColorClass = 'text-danger';
        else if (rank === 2) timeColorClass = 'text-warning';
        else if (rank === 3) timeColorClass = 'text-info';
        else timeColorClass = 'text-success';

        // Create row div
        const rowDiv = document.createElement('div');
        rowDiv.className = `d-flex align-items-center ${index < pageData.length - 1 ? 'mb-3' : ''}`;

        rowDiv.innerHTML = `
            <div class="avatar ${avatarBgClass} text-white rounded-circle d-flex align-items-center justify-content-center"
                 style="width: 40px; height: 40px; font-weight: bold;">
                ${rank}
            </div>
            <div class="flex-1 pt-1 ml-3">
                <h6 class="fw-bold mb-0">${item.equipmentName}</h6>
                <small class="text-muted">Code: ${item.equipmentCode}</small>
                <div class="mt-1">
                    <span class="badge bg-info">Work Reports: ${item.totalWorkReports}</span>
                    <span class="badge bg-warning ms-1">Complaints: ${item.totalComplaints}</span>
                </div>
            </div>
            <div class="text-end ml-2">
                <h5 class="fw-bold ${timeColorClass}">${formatMinutes(item.totalResolutionTime)}</h5>
                <small class="text-muted d-block">Occurrences: <strong>${formatNumber(item.totalOccurrences)}</strong></small>
            </div>
        `;

        container.appendChild(rowDiv);

        // Add separator except after last item
        const sep = document.createElement('div');
        sep.className = 'separator-dashed';
        container.appendChild(sep);
    });
}

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
        if (isScrolling) return;
        e.preventDefault();

        isScrolling = true;
        setTimeout(() => { isScrolling = false; }, 300);

        const delta = e.deltaY;

        if (delta > 0) {
            // Scroll down → next page
            if (wrAllData.length > wrCurrentPage * wrPageSize) {
                wrCurrentPage++;
                renderPage(wrCurrentPage);
            }
        } else if (delta < 0) {
            // Scroll up → previous page
            if (wrCurrentPage > 1) {
                wrCurrentPage--;
                renderPage(wrCurrentPage);
            }
        }
    });

    container.style.cursor = 'grab';
    container.setAttribute('title', 'Scroll to navigate pages');

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

function initEquipmentWorkList() {
    setupButtons();
    enableScrollNavigation();
    fetchEquipmentWorkData();
}

window.addEventListener('DOMContentLoaded', () => {
    initComplaintStatsForm();
    initComplaintChartForm();
    fetchEngineerData();
    initEquipmentWorkList();
    initBreakdownChartForm();
});