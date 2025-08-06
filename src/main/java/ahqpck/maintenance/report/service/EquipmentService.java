package ahqpck.maintenance.report.service;

import ahqpck.maintenance.report.dto.EquipmentDTO;
import ahqpck.maintenance.report.entity.Equipment;
import ahqpck.maintenance.report.exception.ImportException;
import ahqpck.maintenance.report.exception.NotFoundException;
import ahqpck.maintenance.report.repository.EquipmentRepository;
import ahqpck.maintenance.report.specification.EquipmentSpecification;
import ahqpck.maintenance.report.util.FileUploadUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class EquipmentService {

    @Value("${app.upload-equipment-image.dir:src/main/resources/static/upload/equipment/image}")
    private String uploadDir;

    private final EquipmentRepository equipmentRepository;
    private final Validator validator; // Make sure this is autowired

    public EquipmentService(EquipmentRepository equipmentRepository, Validator validator) {
        this.equipmentRepository = equipmentRepository;
        this.validator = validator;
    }

    public Page<EquipmentDTO> getAllEquipments(String keyword, int page, int size, String sortBy, boolean asc) {
        Sort sort = asc ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Equipment> spec = EquipmentSpecification.search(keyword);
        Page<Equipment> equipmentPage = equipmentRepository.findAll(spec, pageable);

        return equipmentPage.map(this::toDTO);
    }

    public EquipmentDTO getEquipmentById(String id) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Equipment not found with ID: " + id));
        return toDTO(equipment);
    }

    public void createEquipment(EquipmentDTO dto, MultipartFile imageFile) {
        if (equipmentRepository.existsByCodeIgnoreCase(dto.getCode())) {
            throw new IllegalArgumentException("Equipment with this code already exists.");
        }

        Equipment equipment = new Equipment();
        mapToEntity(equipment, dto);

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String fileName = FileUploadUtil.saveFile(uploadDir, imageFile, "image");
                equipment.setImage(fileName);
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to save image: " + e.getMessage());
            }
        }

        equipmentRepository.save(equipment);
    }

    public void updateEquipment(EquipmentDTO dto, MultipartFile imageFile, boolean deleteImage) {
        String id = dto.getId();
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Equipment not found with ID: " + id));

        mapToEntity(equipment, dto);

        String oldImage = equipment.getImage();
        if (deleteImage && oldImage != null) {
            FileUploadUtil.deleteFile(uploadDir, oldImage);
            equipment.setImage(null);
        } else if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String newImage = FileUploadUtil.saveFile(uploadDir, imageFile, "image");
                if (oldImage != null) {
                    FileUploadUtil.deleteFile(uploadDir, oldImage);
                }
                equipment.setImage(newImage);
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to save image: " + e.getMessage());
            }
        }

        equipmentRepository.save(equipment);
    }

    public void deleteEquipment(String id) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Equipment not found with ID: " + id));

        if (equipment.getImage() != null) {
            FileUploadUtil.deleteFile(uploadDir, equipment.getImage());
        }
        equipmentRepository.delete(equipment);
    }

    // Add this method to EquipmentService
    public ImportResult importEquipmentsFromExcel(List<Map<String, Object>> data) {
        List<String> errorMessages = new ArrayList<>();
        int importedCount = 0;

        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("No data to import.");
        }

        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> row = data.get(i);
            try {
                EquipmentDTO dto = new EquipmentDTO();

                dto.setCode(toString(row.get("code")));
                dto.setName(toString(row.get("name")));
                dto.setModel(toString(row.get("model")));
                dto.setUnit(toString(row.get("unit")));
                dto.setQty(parseInteger(row.get("qty")));
                dto.setManufacturer(toString(row.get("manufacturer")));
                dto.setSerialNo(toString(row.get("serialNo")));
                dto.setManufacturedDate(toLocalDate(row.get("manufacturedDate")));
                dto.setCommissionedDate(toLocalDate(row.get("commissionedDate")));
                dto.setCapacity(toString(row.get("capacity")));
                dto.setRemarks(toString(row.get("remarks")));

                // Use injected validator
                Set<ConstraintViolation<EquipmentDTO>> violations = validator.validate(dto);
                if (!violations.isEmpty()) {
                    String msg = violations.stream()
                            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                            .collect(Collectors.joining(", "));
                    throw new IllegalArgumentException("Validation failed: " + msg);
                }

                if (dto.getCode() == null || dto.getCode().isEmpty()) {
                    throw new IllegalArgumentException("Code is required");
                }

                if (equipmentRepository.existsByCodeIgnoreCase(dto.getCode())) {
                    throw new IllegalArgumentException("Duplicate equipment code: " + dto.getCode());
                }

                createEquipment(dto, null);
                importedCount++;

            } catch (Exception e) {
                String message = e.getMessage() != null ? e.getMessage() : "Unknown error";
                errorMessages.add("Row " + (i + 1) + ": " + message);
            }
        }

        return new ImportResult(importedCount, errorMessages);
    }

    private String toString(Object obj) {
        return obj != null ? obj.toString().trim() : null;
    }

    private Integer parseInteger(Object obj) {
        if (obj == null || obj.toString().trim().isEmpty())
            return null;
        try {
            return Integer.parseInt(obj.toString().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format: " + obj);
        }
    }

    private LocalDate toLocalDate(Object obj) {
        if (obj == null || obj.toString().trim().isEmpty())
            return null;

        String str = obj.toString().trim();

        // Handle Excel serial date
        if (str.matches("\\d+(\\.\\d+)?")) {
            double serial = Double.parseDouble(str);
            return convertExcelDate(serial);
        }

        // Try multiple full and partial date formats
        return Stream.of(
                // Full date formats
                "yyyy-MM-dd",
                "dd/MM/yyyy", "MM/dd/yyyy",
                "dd-MM-yyyy", "MM-dd-yyyy",

                // Month-year formats
                "MMM yyyy", // "Apr 2014"
                "MMMM yyyy", // "April 2014"
                "MM/yyyy", // "04/2014"
                "M/yyyy", // "4/2014"
                "yyyy-MM", // "2014-04"
                "yyyy/MM")
                .map(pattern -> {
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                        TemporalAccessor parsed = formatter.parse(str);

                        // If the parsed result has only year and month, default day to 1
                        if (parsed.isSupported(ChronoField.YEAR) && parsed.isSupported(ChronoField.MONTH_OF_YEAR)) {
                            int year = parsed.get(ChronoField.YEAR);
                            int month = parsed.get(ChronoField.MONTH_OF_YEAR);
                            return LocalDate.of(year, month, 1);
                        }

                        // Otherwise, try as full LocalDate
                        if (parsed.isSupported(ChronoField.DAY_OF_MONTH)) {
                            return LocalDate.from(parsed);
                        }

                        return null;
                    } catch (DateTimeException ignored) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid date format: " + str));
    }

    private LocalDate convertExcelDate(double serial) {
        int n = (int) serial;
        if (n >= 60)
            n--; // Excel 1900 leap year bug
        return LocalDate.of(1899, 12, 30).plusDays(n);
    }

    // === Result Holder Class (static inner class) ===
    public static class ImportResult {
        private final int importedCount;
        private final List<String> errorMessages;

        public ImportResult(int importedCount, List<String> errorMessages) {
            this.importedCount = importedCount;
            this.errorMessages = errorMessages;
        }

        public int getImportedCount() {
            return importedCount;
        }

        public List<String> getErrorMessages() {
            return errorMessages;
        }

        public boolean hasErrors() {
            return !errorMessages.isEmpty();
        }
    }

    private void mapToEntity(Equipment equipment, EquipmentDTO dto) {
        equipment.setCode(dto.getCode().trim());
        equipment.setName(dto.getName().trim());
        equipment.setModel(dto.getModel());
        equipment.setUnit(dto.getUnit());
        equipment.setQty(dto.getQty() != null ? dto.getQty() : 0);
        equipment.setManufacturer(dto.getManufacturer());
        equipment.setSerialNo(dto.getSerialNo());
        equipment.setManufacturedDate(dto.getManufacturedDate());
        equipment.setCommissionedDate(dto.getCommissionedDate());
        equipment.setCapacity(dto.getCapacity());
        equipment.setRemarks(dto.getRemarks());
    }

    private EquipmentDTO toDTO(Equipment equipment) {
        EquipmentDTO dto = new EquipmentDTO();
        dto.setId(equipment.getId());
        dto.setCode(equipment.getCode());
        dto.setName(equipment.getName());
        dto.setModel(equipment.getModel());
        dto.setUnit(equipment.getUnit());
        dto.setQty(equipment.getQty());
        dto.setManufacturer(equipment.getManufacturer());
        dto.setSerialNo(equipment.getSerialNo());
        dto.setManufacturedDate(equipment.getManufacturedDate());
        dto.setCommissionedDate(equipment.getCommissionedDate());
        dto.setCapacity(equipment.getCapacity());
        dto.setRemarks(equipment.getRemarks());
        dto.setImage(equipment.getImage());
        return dto;
    }

}