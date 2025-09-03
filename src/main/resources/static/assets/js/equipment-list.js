// equipment-list.js
(() => {
    'use strict';

    function initEquipmentList(containerId, apiURL, renderFn, prevBtnId, nextBtnId, refreshBtnId) {
        let currentPage = 1;
        const pageSize = 7;
        let allData = [];

        async function fetchData() {
            try {
                const response = await fetch(apiURL);
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
            pageData.forEach(renderFn);
        }

        function enableScroll() {
            const container = document.getElementById(containerId);
            if (!container) return;
            container.addEventListener('wheel', e => {
                e.preventDefault();
                if (e.deltaY > 0 && allData.length > currentPage * pageSize) {
                    currentPage++;
                    renderPage(currentPage);
                } else if (e.deltaY < 0 && currentPage > 1) {
                    currentPage--;
                    renderPage(currentPage);
                }
            });
        }

        document.getElementById(prevBtnId)?.addEventListener('click', () => {
            if (currentPage > 1) { currentPage--; renderPage(currentPage); }
        });
        document.getElementById(nextBtnId)?.addEventListener('click', () => {
            if (allData.length > currentPage * pageSize) { currentPage++; renderPage(currentPage); }
        });
        document.getElementById(refreshBtnId)?.addEventListener('click', () => {
            currentPage = 1; fetchData();
        });

        enableScroll();
        fetchData();
    }

    // Equipment Complained
    initEquipmentList(
        'equipmentListContainer',
        `${baseUrl}/api/dashboards/equipment-complaint-count`,
        (item, index, arr) => {
            const rank = (Math.ceil(arr.length / 7) - 1) * 7 + index + 1;
            const div = document.createElement('div');
            div.className = 'd-flex';
            div.innerHTML = `
                <div class="mr-3 d-flex align-items-center" style="min-width: 28px;">
                    <span class="fw-bold" style="font-size: 0.95rem; color: #555;">${rank}</span>
                </div>
                <div class="flex-1 pt-1 ml-2">
                    <h6 class="fw-bold mb-1">${item.equipmentCode}</h6>
                    <small class="text-muted">${item.equipmentName}</small>
                </div>
                <div class="d-flex ml-auto align-items-center">
                    <h3 class="text-info fw-bold">${item.totalComplaints}</h3>
                </div>
            `;
            document.getElementById('equipmentListContainer').appendChild(div);
            const sep = document.createElement('div');
            sep.className = 'separator-dashed';
            document.getElementById('equipmentListContainer').appendChild(sep);
        },
        'prevEquipmentBtn',
        'nextEquipmentBtn',
        'refreshEquipmentBtn'
    );

    // Equipment Repaired
    initEquipmentList(
        'equipment-work-list-container',
        `${baseUrl}/api/dashboards/equipment-work-report`,
        (item, index, arr) => {
            const rank = (Math.ceil(arr.length / 7) - 1) * 7 + index + 1;
            const div = document.createElement('div');
            div.className = 'd-flex align-items-center';
            div.style.minHeight = '50px';
            div.innerHTML = `
                <div class="mr-3 d-flex align-items-center" style="min-width: 28px;">
                    <span class="fw-bold" style="font-size: 0.95rem; color: #555;">${rank}</span>
                </div>
                <div class="flex-1 ml-2 pt-1">
                    <h6 class="fw-bold mb-1">${item.equipmentCode} <span class="text-muted pl-3">(${item.totalWorkReports} reports)</span></h6>
                    <small class="text-muted">${item.equipmentName}</small>
                </div>
                <div class="d-flex ml-auto align-items-center">
                    <small class="text-muted">${item.totalResolutionTime} min</small>
                </div>
            `;
            document.getElementById('equipment-work-list-container').appendChild(div);
            const sep = document.createElement('div');
            sep.className = 'separator-dashed';
            document.getElementById('equipment-work-list-container').appendChild(sep);
        },
        'equipment-work-prev-btn',
        'equipment-work-next-btn',
        'equipment-work-refresh-btn'
    );

})();