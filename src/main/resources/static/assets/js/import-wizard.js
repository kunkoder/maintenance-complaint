/**
 * ImportWizard - Lightweight, reusable import modal logic
 * Usage: new ImportWizard({ fieldMapping: { ... }, likelyFields: [...] })
 */
class ImportWizard {
    constructor(options) {
        this.options = {
            // Required elements
            fileInputId: 'importFile',
            sheetSelectId: 'sheetSelect',
            columnCheckboxesId: 'columnCheckboxes',
            headerRowId: 'headerRow',
            previewTableId: 'item-table',
            formId: 'importForm',

            // Buttons
            nextBtnId: 'nextBtn',
            prevBtnId: 'prevBtn',
            importBtnId: 'importBtn',
            exportExcelBtnId: 'exportExcel',
            exportPdfBtnId: 'exportPdf',

            // Hidden input names
            dataFieldName: 'data',
            sheetFieldName: 'sheet',
            headerRowFieldName: 'headerRow',

            // Auto-select columns containing these keywords
            likelyFields: [],

            // Map displayed column names to internal field names
            // e.g., "Serial No." → "serialNumber"
            fieldMapping: {},

            // Hooks
            onBeforeImport: () => true,
            onSuccess: () => { },
            onError: (err) => alert("Error: " + err.message),

            ...options
        };

        this.workbook = null;
        this.selectedSheet = "";
        this.headerRowIndex = 0;
        this.parsedData = [];
        this.steps = ['sheetSelection', 'columnMapping', 'previewSection'];
        this.currentStep = -1;

        this.init();
    }

    init() {
        this.bindFileUpload();
        this.bindSheetSelection();
        this.bindNavigation();
        this.bindFinalImport();
        if (this.options.exportExcelBtnId) this.bindExportExcel();
        if (this.options.exportPdfBtnId) this.bindExportPdf();
    }

    showStep(n) {
        this.steps.forEach(id => {
            const el = document.getElementById(id);
            if (el) el.style.display = 'none';
        });

        if (n >= 0 && n < this.steps.length) {
            document.getElementById(this.steps[n]).style.display = 'block';
            this.currentStep = n;
        }

        const prev = document.getElementById(this.options.prevBtnId);
        const next = document.getElementById(this.options.nextBtnId);
        const imp = document.getElementById(this.options.importBtnId);

        if (prev) prev.style.display = this.currentStep > 0 ? 'inline-block' : 'none';
        if (next) next.style.display = this.currentStep < this.steps.length - 1 ? 'inline-block' : 'none';
        if (imp) imp.style.display = this.currentStep === 2 ? 'inline-block' : 'none';
    }

    bindFileUpload() {
        const input = document.getElementById(this.options.fileInputId);
        if (!input) return;

        input.addEventListener('change', (e) => {
            const file = e.target.files[0];
            if (!file) return;

            const reader = new FileReader();
            reader.onload = (ev) => {
                try {
                    this.workbook = XLSX.read(new Uint8Array(ev.target.result), { type: 'array' });

                    const sheetNames = this.workbook.SheetNames;
                    const select = document.getElementById(this.options.sheetSelectId);
                    select.innerHTML = '';

                    // Add all sheet options
                    sheetNames.forEach(name => {
                        const opt = document.createElement('option');
                        opt.value = name;
                        opt.textContent = name;
                        select.appendChild(opt);
                    });

                    // Keep selectedSheet = "" (user must choose)
                    select.value = ""; // ← explicitly clear

                    document.getElementById('sheetSelection').style.display = 'block';
                    this.showStep(0);
                } catch (err) {
                    this.options.onError(new Error("Failed to read file."));
                }
            };
            reader.readAsArrayBuffer(file);
        });
    }

    bindSheetSelection() {
        const select = document.getElementById(this.options.sheetSelectId);
        if (!select) return;

        select.addEventListener('change', () => {
            this.selectedSheet = select.value;
            this.detectAndRenderColumns();
            this.showStep(1);
        });
    }

    detectHeaderRow(sheet) {
        const rows = XLSX.utils.sheet_to_json(sheet, { header: 1, defval: '', range: 0 });
        for (let i = 0; i < rows.length; i++) {
            const row = rows[i];
            const nonEmpty = row.filter(c => c?.toString().trim()).length;
            if (nonEmpty < 2) continue;
            const textCells = row.filter(c => {
                const s = c?.toString().trim();
                return s && s.length > 0 && isNaN(s);
            }).length;
            if (textCells >= 2) return { rowIndex: i, headerRow: row };
        }
        return { rowIndex: 0, headerRow: rows[0] || [] };
    }

    detectAndRenderColumns() {
        const sheet = this.workbook.Sheets[this.selectedSheet];
        const { rowIndex, headerRow } = this.detectHeaderRow(sheet);
        this.headerRowIndex = rowIndex;
        document.getElementById(this.options.headerRowId).textContent = `Row ${rowIndex + 1}`;

        const container = document.getElementById(this.options.columnCheckboxesId);
        container.innerHTML = '';

        headerRow.forEach((col, idx) => {
            const text = col?.toString().trim();
            if (!text) return;

            const btn = document.createElement('div');
            btn.className = 'col-btn';
            btn.dataset.idx = idx;
            btn.textContent = text;

            if (this.options.likelyFields.some(f => text.toLowerCase().includes(f))) {
                btn.classList.add('selected');
            }

            btn.addEventListener('click', () => btn.classList.toggle('selected'));
            container.appendChild(btn);
        });
    }

    generatePreview() {
        const sheet = this.workbook.Sheets[this.selectedSheet];
        const rows = XLSX.utils.sheet_to_json(sheet, { header: 1 });
        if (rows.length <= this.headerRowIndex) return;

        const headerRow = rows[this.headerRowIndex];
        const selectedBtns = document.querySelectorAll(`#${this.options.columnCheckboxesId} .col-btn.selected`);
        const selectedIndices = Array.from(selectedBtns).map(b => parseInt(b.dataset.idx));

        const thead = document.querySelector(`#${this.options.previewTableId} thead tr`);
        const tbody = document.getElementById('previewTbody');
        thead.innerHTML = ''; tbody.innerHTML = '';

        // Headers
        selectedBtns.forEach(btn => {
            const th = document.createElement('th');
            th.textContent = btn.textContent;
            thead.appendChild(th);
        });

        // Sample rows
        const max = Math.min(10, rows.length - this.headerRowIndex - 1);
        for (let i = 1; i <= max; i++) {
            const row = rows[this.headerRowIndex + i];
            const tr = document.createElement('tr');
            selectedIndices.forEach(idx => {
                const td = document.createElement('td');
                td.textContent = row[idx] || '';
                tr.appendChild(td);
            });
            tbody.appendChild(tr);
        }

        // Build parsed data
        this.parsedData = [];
        for (let i = this.headerRowIndex + 1; i < rows.length; i++) {
            const row = rows[i];
            const obj = {};
            selectedIndices.forEach(idx => {
                const btn = document.querySelector(`.col-btn[data-idx="${idx}"]`);
                if (btn && btn.classList.contains('selected')) {
                    const display = btn.textContent.trim();
                    const key = this.options.fieldMapping[display] || display.toLowerCase();
                    obj[key] = row[idx];
                }
            });
            this.parsedData.push(obj);
        }
    }

    bindNavigation() {
        document.getElementById(this.options.nextBtnId).addEventListener('click', () => {
            if (this.currentStep === 1) this.generatePreview();
            this.showStep(this.currentStep + 1);
        });

        document.getElementById(this.options.prevBtnId).addEventListener('click', () => {
            this.showStep(this.currentStep - 1);
        });
    }

    bindFinalImport() {
        document.getElementById(this.options.importBtnId).addEventListener('click', () => {
            if (this.parsedData.length === 0) {
                this.options.onError(new Error("No data to import."));
                return;
            }

            if (this.options.onBeforeImport() === false) return;

            const form = document.getElementById(this.options.formId);
            document.getElementById('importData').value = JSON.stringify(this.parsedData);
            document.getElementById('importSheet').value = this.selectedSheet;
            document.getElementById('importHeaderRow').value = this.headerRowIndex + 1;

            form.submit();
            this.options.onSuccess();
        });
    }

    // bindExportExcel() {
    //     const btn = document.getElementById(this.options.exportExcelBtnId);
    //     if (!btn) return;

    //     btn.addEventListener('click', (e) => {
    //         e.preventDefault();
    //         const headers = Array.from(document.querySelectorAll(`#${this.options.previewTableId} thead th`))
    //             .filter(th => !th.textContent.includes('Action'))
    //             .map(th => th.textContent.trim());

    //         const data = Array.from(document.querySelectorAll(`#${this.options.previewTableId} tbody tr`))
    //             .filter(tr => tr.style.display !== 'none')
    //             .map(tr => {
    //                 const tds = tr.querySelectorAll('td');
    //                 return Array.from(tds).slice(0, -1).map(td => td.textContent.trim());
    //             });

    //         const ws = XLSX.utils.aoa_to_sheet([headers, ...data]);
    //         const wb = XLSX.utils.book_new();
    //         XLSX.utils.book_append_sheet(wb, ws, "Data");
    //         XLSX.writeFile(wb, `Import_Data_${this.today()}.xlsx`);
    //     });
    // }

    // bindExportPdf() {
    //     const btn = document.getElementById(this.options.exportPdfBtnId);
    //     if (!btn) return;

    //     btn.addEventListener('click', (e) => {
    //         e.preventDefault();
    //         const { jsPDF } = window.jspdf;
    //         const doc = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' });

    //         const headers = Array.from(document.querySelectorAll(`#${this.options.previewTableId} thead th`))
    //             .filter(th => !th.textContent.includes('Action'))
    //             .map(th => th.textContent.trim());

    //         const data = Array.from(document.querySelectorAll(`#${this.options.previewTableId} tbody tr`))
    //             .filter(tr => tr.style.display !== 'none')
    //             .map(tr => {
    //                 const tds = tr.querySelectorAll('td');
    //                 return Array.from(tds).slice(0, -1).map(td => td.textContent.trim());
    //             });

    //         doc.setFontSize(16);
    //         doc.text("Import Data", 14, 15);
    //         doc.autoTable({
    //             head: [headers],
    //             body: data,
    //             startY: 20,
    //             theme: 'grid',
    //             styles: { fontSize: 8 },
    //             columnStyles: headers.reduce((acc, _, i) => ({ ...acc, [i]: { cellWidth: 'auto' } }), {})
    //         });
    //         doc.save(`Import_Data_${this.today()}.pdf`);
    //     });
    // }

    today() {
        return new Date().toISOString().slice(0, 10);
    }
}