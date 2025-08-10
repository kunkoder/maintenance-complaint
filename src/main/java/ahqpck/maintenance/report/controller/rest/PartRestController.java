package ahqpck.maintenance.report.controller.rest;

import ahqpck.maintenance.report.dto.PartDTO;
import ahqpck.maintenance.report.service.PartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/parts")
@RequiredArgsConstructor
public class PartRestController {

  private final PartService partService;

  // === GET ALL PARTS WITH PAGINATION ===
  @GetMapping
  public ResponseEntity<PageResponse<PartDTO>> getAllParts(
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "name") String sortBy,
      @RequestParam(defaultValue = "true") boolean asc) {

    var partsPage = partService.getAllParts(keyword, page, size, sortBy, asc);
    var pageResponse = new PageResponse<>(
        partsPage.getContent(),
        partsPage.getNumber(),
        partsPage.getSize(),
        partsPage.getTotalElements(),
        partsPage.getTotalPages(),
        partsPage.hasPrevious(),
        partsPage.hasNext());

    return ResponseEntity.ok(pageResponse);
  }

  // === GET PART BY ID ===
  @GetMapping("/{id}")
  public ResponseEntity<PartDTO> getPartById(@PathVariable String id) {
    PartDTO dto = partService.getPartById(id);
    return ResponseEntity.ok(dto);
  }

  // === CREATE PART ===
  @PostMapping
  public ResponseEntity<ApiResponse<PartDTO>> createPart(
      @Valid @RequestPart PartDTO partDTO,
      @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) {

    partService.createPart(partDTO, imageFile);
    var response = new ApiResponse<>(true, "Part created successfully.", partDTO);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  // === UPDATE PART ===
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<PartDTO>> updatePart(
      @PathVariable String id,
      @Valid @RequestPart PartDTO partDTO,
      @RequestPart(value = "imageFile", required = false) MultipartFile imageFile,
      @RequestParam(value = "deleteImage", required = false, defaultValue = "false") boolean deleteImage) {

    partDTO.setId(id); // Ensure ID is set
    partService.updatePart(partDTO, imageFile, deleteImage);
    var response = new ApiResponse<>(true, "Part updated successfully.", partDTO);
    return ResponseEntity.ok(response);
  }

  // === DELETE PART ===
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deletePart(@PathVariable String id) {
    partService.deletePart(id);
    ApiResponse<Void> response = new ApiResponse<>(true, "Part deleted successfully.", null);
    return ResponseEntity.ok(response);
  }

  // === HELPER CLASSES ===

  /**
   * Generic response wrapper for paginated results
   */
  public static class PageResponse<T> {
    private List<T> content;
    private int number;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean hasPrevious;
    private boolean hasNext;

    public PageResponse(List<T> content, int number, int size, long totalElements, int totalPages,
        boolean hasPrevious, boolean hasNext) {
      this.content = content;
      this.number = number;
      this.size = size;
      this.totalElements = totalElements;
      this.totalPages = totalPages;
      this.first = number == 0;
      this.last = number == totalPages - 1;
      this.hasPrevious = hasPrevious;
      this.hasNext = hasNext;
    }

    // Getters
    public List<T> getContent() {
      return content;
    }

    public int getNumber() {
      return number;
    }

    public int getSize() {
      return size;
    }

    public long getTotalElements() {
      return totalElements;
    }

    public int getTotalPages() {
      return totalPages;
    }

    public boolean isFirst() {
      return first;
    }

    public boolean isLast() {
      return last;
    }

    public boolean isHasPrevious() {
      return hasPrevious;
    }

    public boolean isHasNext() {
      return hasNext;
    }
  }

  /**
   * Generic API response wrapper
   */
  public static class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    public ApiResponse(boolean success, String message, T data) {
      this.success = success;
      this.message = message;
      this.data = data;
    }

    // Getters and Setters
    public boolean isSuccess() {
      return success;
    }

    public String getMessage() {
      return message;
    }

    public T getData() {
      return data;
    }
  }
}