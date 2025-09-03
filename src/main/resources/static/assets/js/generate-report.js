document.getElementById('generateReportBtn').addEventListener('click', async function (e) {
    e.preventDefault();

    const { jsPDF } = window.jspdf;
    const btn = document.getElementById('generateReportBtn');
    const originalText = btn.innerHTML;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Generating...';
    btn.disabled = true;

    try {
        const pdf = new jsPDF('p', 'mm', 'a4');
        const pageWidth = pdf.internal.pageSize.getWidth();
        let y = 15;

        // Add Title
        pdf.setFontSize(20);
        pdf.setTextColor(40);
        pdf.text('Maintenance Dashboard Report', 14, y);
        y += 10;

        // Add Subtitle
        pdf.setFontSize(12);
        pdf.setTextColor(100);
        const now = new Date().toLocaleString();
        pdf.text(`Generated on: ${now}`, 14, y);
        y += 15;

        // List of elements to capture (excluding equipment)
        const sections = [
            { element: document.querySelector('.card.full-height .card-body'), title: 'Overall Ticket Statistics', scale: 2 },
            { element: document.querySelector('#multipleBarChart'), title: 'Daily Ticket Summary', scale: 2 },
            { element: document.querySelector('.card .table-responsive'), title: 'Engineers Responsibility', scale: 1.5 }
        ];

        // Capture all sections except equipment
        for (const section of sections) {
            if (!section.element) continue;

            if (y + 30 > 280) {
                pdf.addPage();
                y = 20;
            }
            pdf.setFontSize(14);
            pdf.setTextColor(60);
            pdf.text(section.title, 14, y);
            y += 8;

            const canvas = await html2canvas(section.element, {
                scale: section.scale,
                useCORS: true,
                backgroundColor: '#fff',
                logging: false,
                width: section.element.scrollWidth,
                height: section.element.scrollHeight
            });

            const imgData = canvas.toDataURL('image/png');
            const imgWidth = pageWidth - 28;
            const imgHeight = (canvas.height * imgWidth) / canvas.width;

            if (y + imgHeight > 280) {
                pdf.addPage();
                y = 20;
            }

            pdf.addImage(imgData, 'PNG', 14, y, imgWidth, imgHeight);
            y += imgHeight + 10;
        }

        // --- ✅ FIXED & BALANCED: Top Equipment Breakdowns ---
        {
            const tempContainer = document.createElement('div');
            tempContainer.style.fontFamily = 'Arial, sans-serif';
            tempContainer.style.fontSize = '11px';
            tempContainer.style.width = '400px';
            tempContainer.style.padding = '10px';
            tempContainer.style.backgroundColor = '#ffffff';
            tempContainer.style.boxSizing = 'border-box';

            const table = document.createElement('table');
            table.style.width = '100%';
            table.style.borderCollapse = 'collapse';

            // Header
            const thead = `
    <tr>
      <th style="text-align: left; padding: 6px; background-color: #007BFF; color: white; font-size: 7px; font-weight: bold;">#</th>
      <th style="text-align: left; padding: 6px; background-color: #007BFF; color: white; font-size: 7px; font-weight: bold;">Code</th>
      <th style="text-align: left; padding: 6px; background-color: #007BFF; color: white; font-size: 7px; font-weight: bold;">Name</th>
      <th style="text-align: right; padding: 6px; background-color: #007BFF; color: white; font-size: 7px; font-weight: bold;">Count</th>
    </tr>
  `;
            table.innerHTML = `<thead>${thead}</thead><tbody></tbody>`;

            const tbody = table.querySelector('tbody');

            // ✅ Only get divs that contain real equipment data
            const allDivs = Array.from(document.querySelectorAll('#equipmentListContainer > div'));
            const validItems = [];

            allDivs.forEach(div => {
                const code = div.querySelector('h6');
                const name = div.querySelector('small');
                if (code && name && code.textContent.trim() && name.textContent.trim()) {
                    validItems.push({
                        code: code.textContent.trim(),
                        name: name.textContent.trim(),
                        count: (div.querySelector('.text-info')?.textContent || '0').trim()
                    });
                }
            });

            // ✅ Now map with correct ranking
            validItems.forEach((item, index) => {
                const rank = index + 1;
                const row = document.createElement('tr');
                row.style.backgroundColor = index % 2 === 0 ? '#f9f9f9' : '#ffffff';

                row.innerHTML = `
      <td style="padding: 4px 5px; font-weight: bold; color: #333; font-size: 7px;">${rank}</td>
      <td style="padding: 4px 5px; font-weight: bold; color: #000; font-size: 7px;">${item.code}</td>
      <td style="padding: 4px 5px; color: #555; font-size: 7px;">${item.name}</td>
      <td style="padding: 4px 5px; text-align: right; font-weight: bold; color: #007bff; font-size: 7px;">${item.count}</td>
    `;

                tbody.appendChild(row);
            });

            if (validItems.length === 0) {
                const row = document.createElement('tr');
                row.innerHTML = `<td colspan="4" style="padding: 10px; text-align: center; color: #999; font-size: 10px;">No data</td>`;
                tbody.appendChild(row);
            }

            tempContainer.appendChild(table);

            // Hide from view
            tempContainer.style.position = 'absolute';
            tempContainer.style.left = '-9999px';
            tempContainer.style.top = '-9999px';
            document.body.appendChild(tempContainer);

            try {
                const canvas = await html2canvas(tempContainer, {
                    scale: 2,
                    backgroundColor: '#fff'
                });
                document.body.removeChild(tempContainer);

                const imgData = canvas.toDataURL('image/png');
                const imgWidth = pageWidth - 28;
                const imgHeight = (canvas.height * imgWidth) / canvas.width;

                if (y + imgHeight > 280) {
                    pdf.addPage();
                    y = 20;
                }

                pdf.addImage(imgData, 'PNG', 14, y, imgWidth, imgHeight);
                y += imgHeight + 10;
            } catch (err) {
                console.error('Failed to capture equipment list', err);
                document.body.removeChild(tempContainer);
                y += 30;
            }
        }
        // --- End of Equipment Section ---

        // Footer
        pdf.setFontSize(10);
        pdf.setTextColor(150);
        // pdf.text('Kaiadmin Dashboard - Clean, awesome, simple and modern', 14, y || 290);
        pdf.text(`Page 1`, pageWidth - 14, 290, { align: 'right' });

        // Save
        pdf.save(`Dashboard_Report_${new Date().toISOString().split('T')[0]}.pdf`);

    } catch (err) {
        console.error('PDF generation error:', err);
    } finally {
        btn.innerHTML = originalText;
        btn.disabled = false;
    }
});