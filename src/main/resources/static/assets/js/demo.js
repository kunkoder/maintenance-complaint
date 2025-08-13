"use strict";

// Overall Statistics Daily information about complaint status

// STATUS OPEN
let circleOpen, circleInProgress, circlePending, circleClosed;

function initializeCircles() {
    // Ensure elements exist before creating circles
    if (!document.getElementById('circles-1')) {
        console.error('Missing element: circles-1');
        return;
    }

    circleOpen = Circles.create({
        id: 'circles-1',
        radius: 70,
        value: 0,
        maxValue: 100,
        width: 9,
        text: '0',
        colors: ['#f1f1f1', '#FF9E27'],
        duration: 400,
        wrpClass: 'circles-wrp',
        textClass: 'circles-text',
        styleWrapper: true,
        styleText: true
    });

    circleInProgress = Circles.create({
        id: 'circles-2',
        radius: 70,
        value: 0,
        maxValue: 100,
        width: 9,
        text: '0',
        colors: ['#f1f1f1', '#2BB930'],
        duration: 400,
        wrpClass: 'circles-wrp',
        textClass: 'circles-text',
        styleWrapper: true,
        styleText: true
    });

    circlePending = Circles.create({
        id: 'circles-3',
        radius: 70,
        value: 0,
        maxValue: 100,
        width: 9,
        text: '0',
        colors: ['#f1f1f1', '#F25961'],
        duration: 400,
        wrpClass: 'circles-wrp',
        textClass: 'circles-text',
        styleWrapper: true,
        styleText: true
    });

    circleClosed = Circles.create({
        id: 'circles-4',
        radius: 70,
        value: 0,
        maxValue: 100,
        width: 9,
        text: '0',
        colors: ['#f1f1f1', '#17a2b8'],
        duration: 400,
        wrpClass: 'circles-wrp',
        textClass: 'circles-text',
        styleWrapper: true,
        styleText: true
    });

    // Now fetch and update
    fetchStatusCounts();
}

function fetchStatusCounts() {
    fetch('http://localhost:8000/api/dashboards/status-counts')
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok: ' + response.statusText);
            }
            return response.json();
        })
        .then(data => {
            const statusMap = {};
            data.forEach(item => {
                statusMap[item.status] = item.count;
            });

            const openCount = statusMap['OPEN'] || 0;
            const inProgressCount = statusMap['IN_PROGRESS'] || 0;
            const pendingCount = statusMap['PENDING'] || 0;
            const closedCount = statusMap['CLOSED'] || 0;

            // ✅ Only update if circle instance exists
            if (circleOpen) circleOpen.update(openCount);
            if (circleInProgress) circleInProgress.update(inProgressCount);
            if (circlePending) circlePending.update(pendingCount);
            if (circleClosed) circleClosed.update(closedCount);

            // Optional: Manually update text if needed
            updateTextSafely('circles-1', openCount);
            updateTextSafely('circles-2', inProgressCount);
            updateTextSafely('circles-3', pendingCount);
            updateTextSafely('circles-4', closedCount);
        })
        .catch(error => {
            console.error('Error fetching status counts:', error);
            // Update UI to show error
            updateTextSafely('circles-1', 'Err');
            updateTextSafely('circles-2', 'Err');
            updateTextSafely('circles-3', 'Err');
            updateTextSafely('circles-4', 'Err');
        });
}

// Safe helper to update text
function updateTextSafely(circleId, value) {
    const textEl = document.querySelector(`#${circleId} .circles-text`);
    if (textEl) {
        textEl.innerHTML = value;
    }
}

// Initialize after DOM is ready
document.addEventListener('DOMContentLoaded', initializeCircles);

var totalIncomeChart = document.getElementById('totalIncomeChart').getContext('2d');

// Complaints of The Week
var mytotalIncomeChart = new Chart(totalIncomeChart, {
	type: 'bar',
	data: {
		labels: ["S", "M", "T", "W", "T", "F", "S"],
		datasets: [{
			label: "Total Income",
			backgroundColor: '#ff9e27',
			borderColor: 'rgb(23, 125, 255)',
			data: [6, 4, 9, 5, 4, 6, 4, 3, 8, 10],
		}],
	},
	options: {
		responsive: true,
		maintainAspectRatio: false,
		legend: {
			display: false,
		},
		scales: {
			yAxes: [{
				ticks: {
					display: false //this will remove only the label
				},
				gridLines: {
					drawBorder: false,
					display: false
				}
			}],
			xAxes: [{
				gridLines: {
					drawBorder: false,
					display: false
				}
			}]
		},
	}
});

var ctx = document.getElementById('statisticsChart').getContext('2d');

// Equipment Statistics
var statisticsChart = new Chart(ctx, {
	type: 'line',
	data: {
		labels: ["AQPCK-1001", "AQPCK-1002", "AQPCK-1003", "AQPCK-1005", "AQPCK-1006", "AQPCK-1007", "AQPCK-1008", "AQPCK-1009/1"],
		datasets: [
			{
				label: "Closed",
				borderColor: '#2bb930',
				pointBackgroundColor: 'rgba(43, 185, 48, 0.6)',   // #2bb930 → rgba
				pointRadius: 0,
				backgroundColor: 'rgba(43, 185, 48, 0.4)',         // #2bb930 → rgba
				legendColor: '#2bb930',
				fill: true,
				borderWidth: 2,
				data: [154, 184, 175, 203, 210, 231, 240, 278, 252, 312, 320, 374]
			},
			{
				label: "Open",
				borderColor: '#177dff',
				pointBackgroundColor: 'rgba(23, 125, 255, 0.6)',  // #177dff → rgba
				pointRadius: 0,
				backgroundColor: 'rgba(23, 125, 255, 0.4)',        // #177dff → rgba
				legendColor: '#177dff',
				fill: true,
				borderWidth: 2,
				data: [256, 230, 245, 287, 240, 250, 230, 295, 331, 431, 456, 521]
			},
			{
				label: "In Progress",
				borderColor: '#fdaf4b',
				pointBackgroundColor: 'rgba(253, 175, 75, 0.6)', // #fdaf4b → rgba
				pointRadius: 0,
				backgroundColor: 'rgba(253, 175, 75, 0.4)',       // #fdaf4b → rgba
				legendColor: '#fdaf4b',
				fill: true,
				borderWidth: 2,
				data: [542, 480, 430, 550, 530, 453, 380, 434, 568, 610, 700, 900]
			},
			{
				label: "Pending",
				borderColor: '#f3545d',
				pointBackgroundColor: 'rgba(243, 84, 93, 0.6)',  // #f3545d → rgba
				pointRadius: 0,
				backgroundColor: 'rgba(243, 84, 93, 0.4)',        // #f3545d → rgba
				legendColor: '#f3545d',
				fill: true,
				borderWidth: 2,
				data: [1200, 1300, 1250, 1400, 1350, 1450, 1500, 1600, 1700, 1800, 1900, 2000]
			}
		]
	},
	options: {
		responsive: true,
		maintainAspectRatio: false,
		legend: {
			display: false // We'll use custom HTML legend later
		},
		tooltips: {
			mode: 'index',
			intersect: false,
			backgroundColor: 'rgba(0,0,0,0.8)',
			titleColor: '#fff',
			bodyColor: '#fff',
			xPadding: 12,
			yPadding: 12,
			caretSize: 8,
			cornerRadius: 6,
			borderColor: '#333',
			callbacks: {
				title: function (tooltipItems, data) {
					const months = ["Tab Welding Machine", "Plate Surface Grinding Machine", "Plate Automatic UT Inspection Machine", "Plate Edge Milling Machine", "Plate Edge Crimping Machine", "Press Bending Machine",
						"Tack Welding", "Internal Welding Machine Line - 1"];
					return months[tooltipItems[0].index];
				},

				label: function (tooltipItem, data) {
					const dataset = data.datasets[tooltipItem.datasetIndex];
					const value = dataset.data[tooltipItem.index]; // Correct way
					const label = dataset.label || '';
					const formattedValue = value.toLocaleString(); // e.g., 1,234

					const icons = { 'Subscribers': '👤', 'New Visitors': '🌐', 'Active Users': '🟢' };
					const icon = icons[label] || '🔹';

					return `${icon} ${label}: ${formattedValue}`;
				}
			}
		},
		layout: {
			padding: { left: 5, right: 5, top: 15, bottom: 15 }
		},
		scales: {
			yAxes: [{
				ticks: {
					fontStyle: "500",
					beginAtZero: false,
					maxTicksLimit: 5,
					padding: 10,
					// Format Y-axis labels (e.g., 500 → 0.5k)
					callback: function (value) {
						if (value >= 1000) {
							return (value / 1000).toFixed(1) + 'k';
						}
						return value.toLocaleString();
					}
				},
				gridLines: {
					drawTicks: false,
					display: false
				}
			}],
			xAxes: [{
				gridLines: {
					zeroLineColor: "transparent"
				},
				ticks: {
					padding: 10,
					fontStyle: "500"
				}
			}]
		},
		legendCallback: function (chart) {
			var text = [];
			text.push('<ul class="' + chart.id + '-legend html-legend">');
			for (var i = 0; i < chart.data.datasets.length; i++) {
				text.push('<li><span style="background-color:' + chart.data.datasets[i].legendColor + '"></span>');
				if (chart.data.datasets[i].label) {
					text.push(chart.data.datasets[i].label);
				}
				text.push('</li>');
			}
			text.push('</ul>');
			return text.join('');
		}
	}
});

var myLegendContainer = document.getElementById("myChartLegend");

// generate HTML legend
myLegendContainer.innerHTML = statisticsChart.generateLegend();

// bind onClick event to all LI-tags of the legend
var legendItems = myLegendContainer.getElementsByTagName('li');
for (var i = 0; i < legendItems.length; i += 1) {
	legendItems[i].addEventListener("click", legendClickCallback, false);
}

var multipleBarChart = document.getElementById('multipleBarChart').getContext('2d');

var myMultipleBarChart = new Chart(multipleBarChart, {
			type: 'bar',
			data: {
				labels: ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"],
				datasets : [{
					label: "First time visitors",
					backgroundColor: '#59d05d',
					borderColor: '#59d05d',
					data: [95, 100, 112, 101, 144, 159, 178, 156, 188, 190, 210, 245],
				},{
					label: "Visitors",
					backgroundColor: '#fdaf4b',
					borderColor: '#fdaf4b',
					data: [145, 256, 244, 233, 210, 279, 287, 253, 287, 299, 312,356],
				}, {
					label: "Pageview",
					backgroundColor: '#177dff',
					borderColor: '#177dff',
					data: [185, 279, 273, 287, 234, 312, 322, 286, 301, 320, 346, 399],
				}],
			},
			options: {
				responsive: true, 
				maintainAspectRatio: false,
				legend: {
					position : 'bottom'
				},
				title: {
					display: true,
					text: 'Traffic Stats'
				},
				tooltips: {
					mode: 'index',
					intersect: false
				},
				responsive: true,
				scales: {
					xAxes: [{
						stacked: true,
					}],
					yAxes: [{
						stacked: true
					}]
				}
			}
		});