package ahqpck.maintenance.report.service;

import ahqpck.maintenance.report.dto.EquipmentDTO;
import ahqpck.maintenance.report.entity.Equipment;
import ahqpck.maintenance.report.exception.NotFoundException;
import ahqpck.maintenance.report.repository.EquipmentRepository;
import ahqpck.maintenance.report.specification.EquipmentSpecification;
import ahqpck.maintenance.report.util.FileUploadUtil;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class EquipmentService {

    @Value("${app.upload-equipment-image.dir:src/main/resources/static/upload/equipment/image}")
    private String uploadDir;

    private final EquipmentRepository equipmentRepository;

    public EquipmentService(EquipmentRepository equipmentRepository) {
        this.equipmentRepository = equipmentRepository;
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

        if (equipmentRepository.existsByCodeIgnoreCaseAndIdNot(dto.getCode(), id)) {
            throw new IllegalArgumentException("Equipment with this code already exists.");
        }

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