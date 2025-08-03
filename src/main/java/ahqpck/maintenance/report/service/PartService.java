package ahqpck.maintenance.report.service;

import ahqpck.maintenance.report.dto.PartDTO;
import ahqpck.maintenance.report.entity.Part;
import ahqpck.maintenance.report.exception.FileStorageException;
import ahqpck.maintenance.report.exception.ImageTooLargeException;
import ahqpck.maintenance.report.exception.InvalidImageException;
import ahqpck.maintenance.report.exception.PartNotFoundException;
import ahqpck.maintenance.report.exception.ValidationException;
import ahqpck.maintenance.report.repository.PartRepository;
import ahqpck.maintenance.report.specification.PartSpecifications;
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
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
public class PartService {

    @Value("${app.upload.dir:uploads/parts}")
    private String uploadDir;

    private final PartRepository partRepository;

    public PartService(PartRepository partRepository) {
        this.partRepository = partRepository;
    }

    // === GET ALL WITH PAGINATION, SEARCH, SORT ===
    public Page<PartDTO> getAllParts(String keyword, int page, int size, String sortBy, boolean asc) {
        Sort sort = asc ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Part> spec = PartSpecifications.search(keyword);
        Page<Part> partPage = partRepository.findAll(spec, pageable);

        return partPage.map(this::toDTO);
    }

    // === GET BY ID ===
    public PartDTO getPartById(String id) {
        Part part = partRepository.findById(id)
                .orElseThrow(() -> new PartNotFoundException("Part not found with ID: " + id));
        return toDTO(part);
    }

     public void createPart(PartDTO dto, MultipartFile imageFile) {
        // 1. Check if code already exists
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

        // 2. Save image if provided
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String fileName = FileUploadUtil.saveFile(uploadDir, imageFile);
                part.setImage(fileName);
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to save image: " + e.getMessage());
            }
        }

        partRepository.save(part);
    }

    // === UPDATE ===
    public void updatePart(String id, PartDTO dto, MultipartFile imageFile, boolean deleteImage) {
        Part part = partRepository.findById(id)
                .orElseThrow(() -> new PartNotFoundException("Part not found with ID: " + id));

        validatePart(dto, imageFile);

        part.setCode(dto.getCode().trim());
        part.setName(dto.getName().trim());
        part.setDescription(dto.getDescription());
        part.setCategory(dto.getCategory());
        part.setSupplier(dto.getSupplier());
        part.setStockQuantity(dto.getStockQuantity());

        // Handle image
        String oldImage = part.getImage();
        if (deleteImage) {
            if (oldImage != null) {
                deleteImage(oldImage);
                part.setImage(null);
            }
        } else if (imageFile != null && !imageFile.isEmpty()) {
            if (oldImage != null) {
                deleteImage(oldImage);
            }
            part.setImage(saveImage(imageFile));
        }

        partRepository.save(part);
    }

    // === DELETE ===
    public void deletePart(String id) {
        Part part = partRepository.findById(id)
                .orElseThrow(() -> new PartNotFoundException("Part not found with ID: " + id));

        if (part.getImage() != null) {
            deleteImage(part.getImage());
        }
        partRepository.delete(part);
    }

    // === HELPER: Convert to DTO ===
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

    // === IMAGE HANDLING ===
    private String saveImage(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        if (file.getSize() > 1_000_000) {
            throw new ImageTooLargeException("Image must be less than 1MB");
        }

        if (!Objects.requireNonNull(file.getContentType()).startsWith("image/")) {
            throw new InvalidImageException("Only image files are allowed");
        }

        String ext = getFileExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + "." + ext;

        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Files.copy(file.getInputStream(), uploadPath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            return filename;
        } catch (IOException e) {
            throw new FileStorageException("Failed to store image: " + e.getMessage());
        }
    }

    private void deleteImage(String filename) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log error, but don't block deletion
            System.err.println("Failed to delete image: " + filename);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    // === VALIDATION ===
    private void validatePart(PartDTO dto, MultipartFile imageFile) {
        if (dto.getCode() == null || dto.getCode().trim().isEmpty()) {
            throw new ValidationException("Code is required");
        }
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new ValidationException("Name is required");
        }

        // Optional: Check for duplicate code (except self in update)
        // Add service param for update case
    }
}