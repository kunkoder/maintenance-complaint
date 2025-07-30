// ComplaintService.java
package ahqpck.maintenance.report.service;

import ahqpck.maintenance.report.entity.*;
import ahqpck.maintenance.report.repository.ComplaintPartRepository;
import ahqpck.maintenance.report.repository.ComplaintRepository;
import ahqpck.maintenance.report.repository.PartRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private static final Logger log = LoggerFactory.getLogger(ComplaintService.class);

    private final ComplaintRepository complaintRepository;
    private final PartRepository partRepository;
    private final ComplaintPartRepository complaintPartRepository;

    /**
     * Create a new maintenance complaint
     */
    @Transactional
    public Complaint createComplaint(Complaint complaint) {
        log.info("Creating new complaint with subject: '{}' for machine: {}", 
            complaint.getSubject(), complaint.getMachine());
        
        complaint.setStatus(Complaint.Status.OPEN);
        complaint.setCloseTime(null);
        complaint.setTotalResolutionTimeMinutes(null);
        Complaint saved = complaintRepository.save(complaint);
    
        log.info("Complaint created successfully with ID: {}", saved.getId());
        return saved;
    }

    /**
     * Add a part to an existing complaint (before closing)
     */
    @Transactional
    public void addPartToComplaint(String complaintId, String partCode, Integer quantity) {
        log.info("Adding part '{}' (qty: {}) to complaint ID: {}", partCode, quantity, complaintId);
        
        Complaint complaint = complaintRepository.findById(complaintId)
            .orElseThrow(() -> new NoSuchElementException("Complaint not found: " + complaintId));

        Part part = partRepository.findByCode(partCode)
            .orElseThrow(() -> new NoSuchElementException("Part not found: " + partCode));

        // Check if this part is already added
        boolean alreadyAdded = complaint.getPartsUsed().stream()
            .anyMatch(cp -> cp.getPart().getId().equals(part.getId()));

        if (alreadyAdded) {
            throw new IllegalArgumentException("Part is already added to this complaint. Update quantity instead.");
        }

        complaint.addPart(part, quantity);
        complaintRepository.save(complaint);
        log.info("Part '{}' (qty: {}) added to complaint {}", partCode, quantity, complaintId); 
    }

    /**
     * Update complaint status
     * If status is set to CLOSED:
     *  - Sets closeTime
     *  - Calculates total resolution time
     *  - Deducts used parts from inventory
     */
    @Transactional
    public Complaint updateStatus(String complaintId, Complaint.Status newStatus) {
        log.info("Updating complaint ID: {} status to {}", complaintId, newStatus);
        
        Complaint complaint = complaintRepository.findById(complaintId)
            .orElseThrow(() -> new NoSuchElementException("Complaint not found: " + complaintId));

        Complaint.Status oldStatus = complaint.getStatus();
        complaint.setStatus(newStatus);

        if (newStatus == Complaint.Status.CLOSED && oldStatus != Complaint.Status.CLOSED) {
            LocalDateTime now = LocalDateTime.now();
            complaint.setCloseTime(now);
            // totalResolutionTimeMinutes is calculated in @PreUpdate
        }

        complaintRepository.save(complaint);

        // Deduct inventory only when transitioning to CLOSED
        if (newStatus == Complaint.Status.CLOSED && oldStatus != Complaint.Status.CLOSED) {
            deductPartsFromInventory(complaint);
        }

            if (newStatus == Complaint.Status.CLOSED && oldStatus != Complaint.Status.CLOSED) {
            log.info("Complaint {} CLOSED: Deducting {} parts from inventory", complaintId, complaint.getPartsUsed().size());
        }

        return complaint;
    }

    /**
     * Deduct all parts used in this complaint from stock
     */
    private void deductPartsFromInventory(Complaint complaint) {
        for (ComplaintPart cp : complaint.getPartsUsed()) {
            log.info("Deducting {} x '{}' (Part ID: {}) from stock", 
                cp.getQuantity(), cp.getPart().getName(), cp.getPart().getId());
            cp.getPart().useParts(cp.getQuantity());
            partRepository.save(cp.getPart());
        }
    }

    /**
     * Reopen a CLOSED complaint → restock parts
     */
    @Transactional
    public Complaint reopenComplaint(String complaintId) {
        log.warn("Reopening CLOSED complaint: {}", complaintId);
        Complaint complaint = complaintRepository.findById(complaintId)
            .orElseThrow(() -> new NoSuchElementException("Complaint not found: " + complaintId));

        if (complaint.getStatus() != Complaint.Status.CLOSED) {
            throw new IllegalArgumentException("Only CLOSED complaints can be reopened.");
        }

        // Restock all parts
        restockParts(complaint);

        complaint.setStatus(Complaint.Status.IN_PROGRESS);
        complaint.setCloseTime(null);
        complaint.setTotalResolutionTimeMinutes(null);

        log.info("Complaint {} reopened and {} parts restocked", complaintId, complaint.getPartsUsed().size());
        return complaintRepository.save(complaint);
    }

    private void restockParts(Complaint complaint) {
        for (ComplaintPart cp : complaint.getPartsUsed()) {
            Part part = cp.getPart();
            part.addStock(cp.getQuantity());
            partRepository.save(part);
        }
    }

    /**
     * Find complaint by ID
     */
    public Complaint getComplaintById(String complaintId) {
        return complaintRepository.findById(complaintId)
            .orElseThrow(() -> new NoSuchElementException("Complaint not found: " + complaintId));
    }

    /**
     * Get all complaints
     */
    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAll();
    }

    /**
     * Get complaints by status
     */
    public List<Complaint> getComplaintsByStatus(Complaint.Status status) {
        return complaintRepository.findByStatus(status);
    }

    /**
     * Get complaints by machine
     */
    public List<Complaint> getComplaintsByMachine(String machine) {
        return complaintRepository.findByMachine(machine);
    }

    /**
     * Update assignee
     */
    @Transactional
    public Complaint updateAssignee(String complaintId, String assignee) {
        Complaint complaint = getComplaintById(complaintId);
        complaint.setAssignee(assignee);
        return complaintRepository.save(complaint);
    }
}