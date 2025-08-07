package ahqpck.maintenance.report.service;

import ahqpck.maintenance.report.dto.PartDTO;
import ahqpck.maintenance.report.entity.Part;
import ahqpck.maintenance.report.exception.NotFoundException;
import ahqpck.maintenance.report.repository.PartRepository;
import ahqpck.maintenance.report.specification.PartSpecification;
import ahqpck.maintenance.report.util.FileUploadUtil;
import ahqpck.maintenance.report.util.ImportUtil;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

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
@RequiredArgsConstructor
public class PartService {

    @Value("${app.upload-part-image.dir:src/main/resources/static/upload/part/image}")
    private String uploadDir;

    private final PartRepository partRepository;
    private final Validator validator;

    private final FileUploadUtil fileUploadUtil;
    private final ImportUtil importUtil;

    public Page<PartDTO> getAllParts(String keyword, int page, int size, String sortBy, boolean asc) {
        Sort sort = asc ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Part> spec = PartSpecification.search(keyword);
        Page<Part> partPage = partRepository.findAll(spec, pageable);

        return partPage.map(this::toDTO);
    }

    public PartDTO getPartById(String id) {
        Part part = partRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Part not found with ID: " + id));
        return toDTO(part);
    }

    public void createPart(PartDTO dto, MultipartFile imageFile) {

        if (partRepository.existsByCodeIgnoreCase(dto.getCode())) {
            throw new IllegalArgumentException("Part with this code already exists.");
        }

        Part part = new Part();
        part.setCode(dto.getCode().trim());
        part.setName(dto.getName().trim());
        part.setDescription(dto.getDescription());
        part.setCategory(dto.getCategory());
        part.setSupplier(dto.getSupplier());
        part.setStockQuantity(dto.getStockQuantity() != null ? dto.getStockQuantity() : 0);

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String fileName = fileUploadUtil.saveFile(uploadDir, imageFile, "image");
                part.setImage(fileName);
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to save image: " + e.getMessage());
            }
        }

        partRepository.save(part);
    }

    public void updatePart(PartDTO dto, MultipartFile imageFile, boolean deleteImage) {
        String id = dto.getId();
        Part part = partRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Part not found with ID: " + id));

        mapToEntity(part, dto);

        String oldImage = part.getImage();

        if (deleteImage && oldImage != null) {
            fileUploadUtil.deleteFile(uploadDir, oldImage);
            part.setImage(null);
        } else if (imageFile != null && !imageFile.isEmpty()) {

            try {
                String newImage = fileUploadUtil.saveFile(uploadDir, imageFile, "image");
                if (oldImage != null) {
                    fileUploadUtil.deleteFile(uploadDir, oldImage);
                }
                part.setImage(newImage);
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to save image: " + e.getMessage());
            }
        }

        partRepository.save(part);
    }

    public void deletePart(String id) {
        Part part = partRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Part not found with ID: " + id));

        if (part.getImage() != null) {
            fileUploadUtil.deleteFile(uploadDir, part.getImage());
        }
        partRepository.delete(part);
    }

    private void mapToEntity(Part part, PartDTO dto) {
        part.setCode(dto.getCode().trim());
        part.setName(dto.getName().trim());
        part.setDescription(dto.getDescription());
        part.setCategory(dto.getCategory());
        part.setSupplier(dto.getSupplier());
        part.setStockQuantity(dto.getStockQuantity() != null ? dto.getStockQuantity() : 0);
    }

    private PartDTO toDTO(Part part) {
        PartDTO dto = new PartDTO();
        dto.setId(part.getId());
        dto.setCode(part.getCode());
        dto.setName(part.getName());
        dto.setDescription(part.getDescription());
        dto.setCategory(part.getCategory());
        dto.setSupplier(part.getSupplier());
        dto.setImage(part.getImage());
        dto.setStockQuantity(part.getStockQuantity());
        return dto;
    }
}